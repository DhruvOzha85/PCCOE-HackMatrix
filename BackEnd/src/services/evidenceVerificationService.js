/**
 * Evidence Verification Service (FIN-03 Addition 2)
 * Orchestrates cross-document evidence verification across uploaded applicant documents,
 * maps statutory scheme conditions into structured representations,
 * calls the NLP Intelligence verification microservice, and persists audit trails.
 */

const { supabaseAdmin } = require('../config/supabaseConfig');
const profileService = require('./profileService');
const schemeService = require('./schemeService');
const documentServices = require('./documentServices');
const intelligenceClient = require('./intelligenceClient');

// In-memory cache for fast retrieval and offline dev resilience
const memoryReportStore = new Map();
const memoryConflictStore = new Map();

/**
 * Standard statutory condition builder for government schemes.
 */
const buildSchemeConditions = (schemeId, schemeData) => {
    const cleanId = String(schemeId).toLowerCase().trim();

    // Standard rule definitions for common hackathon schemes
    const SCHEME_CONDITION_MAP = {
        pmaw_2024: [
            {
                condition_id: 'cond_pmaw_income',
                scheme_id: schemeId,
                description: 'Annual family income must not exceed ₹3,00,000 for EWS category',
                category: 'annual_income',
                operator: '<=',
                threshold: 300000,
                unit: 'INR/year',
                required_evidence_types: ['income_cert', 'income_certificate', 'salary_slip'],
                mandatory: true,
                rule_combination: 'AND'
            },
            {
                condition_id: 'cond_pmaw_state',
                scheme_id: schemeId,
                description: 'Applicant must be a permanent resident of India / Maharashtra',
                category: 'residence',
                operator: '==',
                threshold: 'Maharashtra',
                unit: null,
                required_evidence_types: ['domicile', 'aadhaar', 'address_proof'],
                mandatory: true,
                rule_combination: 'AND'
            },
            {
                condition_id: 'cond_pmaw_age',
                scheme_id: schemeId,
                description: 'Applicant must be at least 18 years of age',
                category: 'age',
                operator: '>=',
                threshold: 18,
                unit: 'years',
                required_evidence_types: ['aadhaar', 'pan'],
                mandatory: true,
                rule_combination: 'AND'
            }
        ],
        pm_egp: [
            {
                condition_id: 'cond_pmegp_age',
                scheme_id: schemeId,
                description: 'Applicant must be at least 18 years old to start a project',
                category: 'age',
                operator: '>=',
                threshold: 18,
                unit: 'years',
                required_evidence_types: ['aadhaar', 'pan'],
                mandatory: true,
                rule_combination: 'AND'
            },
            {
                condition_id: 'cond_pmegp_income',
                scheme_id: schemeId,
                description: 'Beneficiary family income must be within ₹5,00,000 per annum',
                category: 'annual_income',
                operator: '<=',
                threshold: 500000,
                unit: 'INR/year',
                required_evidence_types: ['income_cert'],
                mandatory: true,
                rule_combination: 'AND'
            }
        ],
        pm_kisan: [
            {
                condition_id: 'cond_kisan_land',
                scheme_id: schemeId,
                description: 'Applicant must hold cultivable land up to 5 acres / 2 hectares',
                category: 'land_ownership',
                operator: '<=',
                threshold: 5.0,
                unit: 'acres',
                required_evidence_types: ['land', 'land_records'],
                mandatory: true,
                rule_combination: 'AND'
            },
            {
                condition_id: 'cond_kisan_occ',
                scheme_id: schemeId,
                description: 'Applicant occupation must be Small or Marginal Farmer',
                category: 'occupation',
                operator: '==',
                threshold: 'farmer',
                unit: null,
                required_evidence_types: ['land'],
                mandatory: true,
                rule_combination: 'AND'
            }
        ]
    };

    if (SCHEME_CONDITION_MAP[cleanId]) {
        return SCHEME_CONDITION_MAP[cleanId];
    }

    // Dynamic fallback generation based on scheme data
    const criteria = schemeData?.eligibility_criteria || schemeData?.criteria || [];
    const conditions = [];

    // Always check age >= 18
    conditions.push({
        condition_id: `cond_${cleanId}_age`,
        scheme_id: schemeId,
        description: 'Applicant must be at least 18 years of age',
        category: 'age',
        operator: '>=',
        threshold: 18,
        unit: 'years',
        required_evidence_types: ['aadhaar', 'pan'],
        mandatory: true,
        rule_combination: 'AND'
    });

    // Check income condition if specified
    const incomeLimit = schemeData?.income_limit || schemeData?.annual_income_limit;
    if (incomeLimit) {
        conditions.push({
            condition_id: `cond_${cleanId}_income`,
            scheme_id: schemeId,
            description: `Annual family income must not exceed ₹${Number(incomeLimit).toLocaleString('en-IN')}`,
            category: 'annual_income',
            operator: '<=',
            threshold: Number(incomeLimit),
            unit: 'INR/year',
            required_evidence_types: ['income_cert'],
            mandatory: true,
            rule_combination: 'AND'
        });
    } else {
        // Default standard income threshold check
        conditions.push({
            condition_id: `cond_${cleanId}_income`,
            scheme_id: schemeId,
            description: 'Annual family income must not exceed ₹2,50,000',
            category: 'annual_income',
            operator: '<=',
            threshold: 250000,
            unit: 'INR/year',
            required_evidence_types: ['income_cert'],
            mandatory: true,
            rule_combination: 'AND'
        });
    }

    // Check state residence
    const stateReq = schemeData?.state || 'Maharashtra';
    conditions.push({
        condition_id: `cond_${cleanId}_state`,
        scheme_id: schemeId,
        description: `Applicant must be a permanent resident of ${stateReq}`,
        category: 'residence',
        operator: '==',
        threshold: stateReq,
        unit: null,
        required_evidence_types: ['domicile', 'aadhaar'],
        mandatory: true,
        rule_combination: 'AND'
    });

    return conditions;
};

/**
 * Runs cross-document evidence verification for an applicant against a scheme.
 */
const verifyApplicantEvidence = async ({
    userId,
    schemeId,
    applicationId = null,
    assessmentDate = null
}) => {
    if (!userId) throw new Error('User ID is required for evidence verification');
    if (!schemeId) throw new Error('Scheme ID is required for evidence verification');

    // 1. Fetch applicant profile
    let applicantProfile = null;
    try {
        applicantProfile = await profileService.getProfileById(userId);
    } catch (_) { }

    // 2. Fetch scheme metadata
    let schemeData = null;
    let schemeName = schemeId;
    try {
        const res = await schemeService.getSchemeById(schemeId);
        schemeData = res?.scheme || res;
        schemeName = schemeData?.scheme_name || schemeData?.name || schemeData?.title || schemeId;
    } catch (_) { }

    // 3. Decompose scheme into structured conditions
    const conditions = buildSchemeConditions(schemeId, schemeData);

    // 4. Fetch applicant documents from document vault
    let documents = [];
    try {
        documents = await documentServices.listDocuments(userId, applicationId);
    } catch (err) {
        console.warn(`[evidenceVerificationService] Could not list documents: ${err.message}`);
    }

    // 5. Call Intelligence Microservice endpoint
    const payload = {
        applicant_id: userId,
        application_id: applicationId,
        scheme_id: schemeId,
        scheme_name: schemeName,
        conditions,
        documents: documents.map(d => ({
            id: d.id,
            document_id: d.id,
            fileName: d.fileName,
            filename: d.fileName,
            documentType: d.documentType,
            document_type: d.documentType,
            issuer: d.issuer || d.extractedData?.issuing_authority,
            issueDate: d.extractedData?.issue_date,
            validUntil: d.extractedData?.valid_until,
            page: d.ocrMetadata?.pages || 1,
            extractedText: d.extractedText || '',
            extractedData: d.extractedData || {}
        })),
        applicant_profile: applicantProfile ? {
            annual_income: applicantProfile.annual_income,
            state: applicantProfile.state,
            date_of_birth: applicantProfile.date_of_birth,
            age: applicantProfile.age,
            category: applicantProfile.caste_category || applicantProfile.category,
            occupation: applicantProfile.occupation
        } : null,
        assessment_date: assessmentDate
    };

    let report = null;
    try {
        const aiResponse = await intelligenceClient.postJson('/v1/evidence/verify', payload, {
            timeoutMs: 30000
        });
        report = aiResponse?.report;
    } catch (err) {
        console.warn(`[evidenceVerificationService] Intelligence microservice unavailable (${err.message}). Using local deterministic verification fallback.`);
        // Local deterministic fallback preserving three classifications and conflict detection
        report = generateLocalFallbackReport(payload);
    }

    if (report) {
        if (!report.condition_results && report.conditions) {
            report.condition_results = report.conditions;
        }
        if (!report.conditions && report.condition_results) {
            report.conditions = report.condition_results;
        }
    }

    // 6. Cache and persist report
    const cacheKey = applicationId || `${userId}_${schemeId}`;
    memoryReportStore.set(cacheKey, report);

    // Store conflicts in conflict store
    if (report?.detected_conflicts) {
        for (const conf of report.detected_conflicts) {
            memoryConflictStore.set(conf.conflict_id, {
                ...conf,
                applicant_id: userId,
                application_id: applicationId,
                scheme_id: schemeId
            });
        }
    }

    // Persist to Supabase if table exists (graceful degradation)
    try {
        await supabaseAdmin.from('evidence_verification_reports').upsert({
            id: report.report_id || cacheKey,
            user_id: userId,
            application_id: applicationId,
            scheme_id: schemeId,
            overall_status: report.overall_status,
            report_data: report,
            verified_at: new Date().toISOString()
        });
    } catch (_) { }

    return {
        success: true,
        report
    };
};

/**
 * Local fallback verification engine ensuring uninterrupted evaluation if AI server restarts.
 */
const generateLocalFallbackReport = (payload) => {
    const { conditions, documents, applicant_profile } = payload;
    const conditionResults = [];
    const conflicts = [];

    for (const cond of conditions) {
        const cat = (cond.category || '').toLowerCase();
        let status = 'INSUFFICIENT';
        let supporting = [];
        let contradicting = [];
        let insufficient = [];
        let actualVal = null;
        let explanation = `Evaluated condition: ${cond.description}`;

        // Find relevant document
        for (const doc of documents) {
            const text = (doc.extractedText || '').toLowerCase();
            const data = doc.extractedData || {};

            if (cat.includes('income')) {
                const inc = data.annual_income || data.annual_family_income || data.income;
                if (inc !== undefined && inc !== null) {
                    const num = Number(inc);
                    actualVal = num;
                    const passes = cond.operator === '<=' ? num <= cond.threshold : num < cond.threshold;
                    status = passes ? 'SUPPORTING' : 'CONTRADICTING';
                    const item = {
                        evidence_id: `ev_local_${doc.id}`,
                        condition_id: cond.condition_id,
                        document_id: doc.id,
                        document_type: doc.documentType,
                        filename: doc.fileName,
                        issuer: doc.issuer || 'Revenue Authority',
                        excerpt: `Certified annual income: ₹${num.toLocaleString('en-IN')}`,
                        classification: status,
                        explanation: `Extracted family income ₹${num.toLocaleString('en-IN')} ${passes ? 'is within' : 'exceeds'} threshold ₹${Number(cond.threshold).toLocaleString('en-IN')}.`,
                        normalized_value: num,
                        confidence: 0.95
                    };
                    if (passes) supporting.push(item);
                    else contradicting.push(item);
                }
            } else if (cat.includes('age')) {
                const dob = data.date_of_birth || data.dob;
                if (dob) {
                    const birthYear = new Date(dob).getFullYear();
                    const age = new Date().getFullYear() - birthYear;
                    actualVal = age;
                    const passes = age >= (cond.threshold || 18);
                    status = passes ? 'SUPPORTING' : 'CONTRADICTING';
                    const item = {
                        evidence_id: `ev_local_${doc.id}`,
                        condition_id: cond.condition_id,
                        document_id: doc.id,
                        document_type: doc.documentType,
                        filename: doc.fileName,
                        excerpt: `Date of birth certified as ${dob} (Age: ${age} years)`,
                        classification: status,
                        explanation: `Age of ${age} years ${passes ? 'meets' : 'does not meet'} minimum requirement of ${cond.threshold} years.`,
                        normalized_value: age,
                        confidence: 0.95
                    };
                    if (passes) supporting.push(item);
                    else contradicting.push(item);
                }
            } else if (cat.includes('residence') || cat.includes('state')) {
                const state = data.state || (text.includes('maharashtra') ? 'Maharashtra' : null);
                if (state) {
                    actualVal = state;
                    const matches = String(state).toLowerCase() === String(cond.threshold).toLowerCase();
                    status = matches ? 'SUPPORTING' : 'CONTRADICTING';
                    const item = {
                        evidence_id: `ev_local_${doc.id}`,
                        condition_id: cond.condition_id,
                        document_id: doc.id,
                        document_type: doc.documentType,
                        filename: doc.fileName,
                        excerpt: `Domicile state declared as ${state}`,
                        classification: status,
                        explanation: `State of residence (${state}) ${matches ? 'matches' : 'conflicts with'} ${cond.threshold}.`,
                        normalized_value: state,
                        confidence: 0.90
                    };
                    if (matches) supporting.push(item);
                    else contradicting.push(item);
                }
            }
        }

        // Check profile declaration conflict against extracted document values
        if (cat.includes('income') && applicant_profile?.annual_income && actualVal !== null) {
            const profInc = Number(applicant_profile.annual_income);
            if (Math.abs(profInc - actualVal) > 10) {
                conflicts.push({
                    conflict_id: `cnf_local_${cond.condition_id}`,
                    condition_id: cond.condition_id,
                    field_category: 'annual_income',
                    conflicting_evidence_ids: ['profile_declaration', 'document_vault'],
                    source_excerpts: [
                        { source: 'Profile Declaration', value: profInc, excerpt: `Profile declared income: ₹${profInc.toLocaleString('en-IN')}` },
                        { source: 'Uploaded Certificate', value: actualVal, excerpt: `Certified income: ₹${actualVal.toLocaleString('en-IN')}` }
                    ],
                    explanation: `Discrepancy detected: Profile states ₹${profInc.toLocaleString('en-IN')}, but uploaded certificate shows ₹${actualVal.toLocaleString('en-IN')}.`,
                    severity: 'HIGH',
                    status: 'OPEN'
                });
            }
        }

        conditionResults.push({
            condition_id: cond.condition_id,
            description: cond.description,
            category: cond.category,
            overall_status: status,
            supporting_evidence: supporting,
            contradicting_evidence: contradicting,
            insufficient_evidence: insufficient,
            detected_conflicts: conflicts.filter(c => c.condition_id === cond.condition_id),
            threshold: cond.threshold,
            actual_value: actualVal,
            unit: cond.unit,
            explanation,
            requires_manual_review: status !== 'SUPPORTING' || conflicts.length > 0,
            ready_for_rule_engine: status === 'SUPPORTING' && conflicts.length === 0
        });
    }

    return {
        report_id: `rep_${Date.now()}`,
        applicant_id: payload.applicant_id,
        application_id: payload.application_id,
        scheme_id: payload.scheme_id,
        scheme_name: payload.scheme_name,
        overall_status: conflicts.length > 0 ? 'CONFLICT_DETECTED' : 'VERIFIED',
        condition_results: conditionResults,
        detected_conflicts: conflicts,
        unresolved_conflict_count: conflicts.filter(c => c.status === 'OPEN').length,
        ready_for_rule_engine: conflicts.length === 0,
        tallies: {
            supporting_conditions: conditionResults.filter(c => c.overall_status === 'SUPPORTING').length,
            contradicting_conditions: conditionResults.filter(c => c.overall_status === 'CONTRADICTING').length,
            insufficient_conditions: conditionResults.filter(c => c.overall_status === 'INSUFFICIENT').length,
            total_conditions: conditionResults.length
        },
        verified_at: new Date().toISOString()
    };
};

/**
 * Retrieves the verification results for an application.
 */
const getEvidenceResults = async (applicationId, userId) => {
    // 1. Check in-memory store
    if (memoryReportStore.has(applicationId)) {
        return memoryReportStore.get(applicationId);
    }

    // 2. Try prefix search
    for (const [k, v] of memoryReportStore.entries()) {
        if (k.includes(applicationId) || k.includes(userId)) {
            return v;
        }
    }

    // 3. Try Intelligence microservice
    try {
        const aiRes = await intelligenceClient.get(`/v1/evidence/results/${applicationId}`);
        if (aiRes?.report) {
            memoryReportStore.set(applicationId, aiRes.report);
            return aiRes.report;
        }
    } catch (_) { }

    return null;
};

/**
 * Lists all detected cross-document conflicts for reviewer queue.
 */
const listReviewerConflicts = async (filters = {}) => {
    const list = Array.from(memoryConflictStore.values());
    if (filters.status) {
        return list.filter(c => c.status === filters.status);
    }
    return list;
};

/**
 * Resolves a cross-document conflict (Caseworker Reviewer action).
 */
const resolveConflict = async (conflictId, reviewerUser, resolutionData) => {
    if (!conflictId) throw new Error('Conflict ID is required');
    if (!resolutionData?.reason) throw new Error('Mandatory resolution reason is required');

    const conflict = memoryConflictStore.get(conflictId);
    if (!conflict) {
        throw new Error(`Conflict record '${conflictId}' not found`);
    }

    conflict.status = 'RESOLVED';
    conflict.resolution_reason = resolutionData.reason;
    conflict.resolved_by = reviewerUser?.full_name || reviewerUser?.email || reviewerUser?.id || 'Caseworker';
    conflict.resolved_at = new Date().toISOString();
    conflict.authoritative_value = resolutionData.authoritative_value || null;

    // Call Intelligence endpoint
    try {
        await intelligenceClient.postJson(`/v1/evidence/conflicts/${conflictId}/resolve`, {
            resolver_id: reviewerUser?.id || 'reviewer',
            selected_source: resolutionData.selected_source || 'DOCUMENT',
            reason: resolutionData.reason,
            authoritative_value: resolutionData.authoritative_value
        });
    } catch (err) {
        console.warn(`[evidenceVerificationService] Note: Intelligence service sync deferred (${err.message})`);
    }

    // Update parent report if cached
    for (const rep of memoryReportStore.values()) {
        if (rep.detected_conflicts) {
            const match = rep.detected_conflicts.find(c => c.conflict_id === conflictId);
            if (match) {
                match.status = 'RESOLVED';
                match.resolution_reason = resolutionData.reason;
                match.resolved_by = conflict.resolved_by;
                match.resolved_at = conflict.resolved_at;
            }
        }
    }

    return conflict;
};

/**
 * Runs reproducible benchmark evaluation across Baseline A, Baseline B, and Proposed NLP.
 */
const evaluateBaselines = async (split = 'held_out_test') => {
    return await intelligenceClient.postJson('/v1/evidence/evaluate-baselines', {
        split
    });
};

module.exports = {
    verifyApplicantEvidence,
    getEvidenceResults,
    listReviewerConflicts,
    resolveConflict,
    evaluateBaselines,
    buildSchemeConditions
};
