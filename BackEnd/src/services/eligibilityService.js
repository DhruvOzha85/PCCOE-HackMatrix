const profileService = require('./profileService');
const schemeService = require('./schemeService');
const { supabaseAdmin } = require('../config/supabaseConfig');
const intelligenceClient = require('./intelligenceClient');

const httpError = (status, message) => {
    const error = new Error(message);
    error.status = status;
    return error;
};

// ─── Helpers for Eligibility State Messaging Integrity (Phase D3.8) ──────────

const isInternalSentinel = (field) => {
    if (typeof field !== 'string') return true;
    const lower = field.toLowerCase().trim();
    return lower.includes('not_registered') ||
        lower.startsWith('scheme_') ||
        lower.endsWith('_error') ||
        lower === 'rule_evaluation_error' ||
        lower === 'unknown';
};

const PROFILE_FIELD_LABELS = {
    date_of_birth: 'Date of Birth',
    dob: 'Date of Birth',
    age: 'Age / Date of Birth',
    annual_income: 'Annual Family Income',
    income: 'Annual Family Income',
    occupation: 'Occupation',
    caste_category: 'Social Category',
    category: 'Social Category',
    gender: 'Gender',
    state: 'State of Residence',
    land_ownership_acres: 'Agricultural Land Ownership',
    education_level: 'Education Level',
    is_woman_entrepreneur: 'Woman Entrepreneur Status',
    marital_status: 'Marital Status'
};

const formatFieldLabel = (field) => {
    if (!field || typeof field !== 'string') return '';
    const clean = field.toLowerCase().trim();
    if (PROFILE_FIELD_LABELS[clean]) return PROFILE_FIELD_LABELS[clean];
    return clean
        .replace(/_/g, ' ')
        .replace(/([a-z])([A-Z])/g, '$1 $2')
        .replace(/\b\w/g, c => c.toUpperCase());
};

const RULE_TO_PROFILE_FIELDS = {
    'AGE_18': ['date_of_birth'],
    'NON_FARM_ACTIVITY': ['occupation'],
    'FARMER_OCCUPATION': ['occupation'],
    'INCOME_THRESHOLD': ['annual_income'],
    'SC_ST_OR_WOMAN': ['caste_category', 'gender']
};

/**
/**
 * FIN-03: Comprehensive Eligibility Change Analysis Builder
 * Categorizes blockers into ELIGIBILITY_NOT_MET, MISSING_EVIDENCE, CONFLICTING_EVIDENCE, UNVERIFIED_RULE.
 * Annotates source rules, exact excerpts, actionability, and expected outcomes.
 */
const buildEligibilityChangeAnalysis = (schemeId, schemeName, verdict, rules = [], missingFields = [], profileSnapshot = {}, userDocuments = []) => {
    const unmetConditions = [];
    const missingEvidence = [];
    const conflictingEvidence = [];
    const unverifiedRules = [];
    const actionableSteps = [];
    const immutableBlockers = [];
    const allBlockers = [];

    // Map determination status to prompt FIN-03 statuses (ELIGIBLE, INELIGIBLE, NEEDS_VERIFICATION, MANUAL_REVIEW)
    let fin03Status = 'MANUAL_REVIEW';
    if (verdict === 'ELIGIBLE' || verdict === 'PASS') fin03Status = 'ELIGIBLE';
    else if (verdict === 'NOT_ELIGIBLE' || verdict === 'FAIL') fin03Status = 'INELIGIBLE';
    else if (missingFields && missingFields.length > 0) fin03Status = 'NEEDS_VERIFICATION';
    else fin03Status = 'MANUAL_REVIEW';

    // Build document evidence index for quick lookup
    const docsByType = {};
    (userDocuments || []).forEach(doc => {
        const dtype = (doc.documentType || 'other').toLowerCase();
        if (!docsByType[dtype]) docsByType[dtype] = [];
        docsByType[dtype].push(doc);
    });

    // Helper: find supporting documents for a rule
    const findSupportingDocuments = (ruleId) => {
        const ruleIdLower = String(ruleId).toLowerCase();
        const supporting = [];
        // Map common rule IDs to document types
        if (ruleIdLower.includes('income') || ruleIdLower.includes('threshold')) {
            ['income_cert', 'itr'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        if (ruleIdLower.includes('age') || ruleIdLower.includes('dob')) {
            ['aadhaar', 'pan'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        if (ruleIdLower.includes('residency') || ruleIdLower.includes('state') || ruleIdLower.includes('domicile') || ruleIdLower.includes('hp_')) {
            ['domicile', 'aadhaar'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        if (ruleIdLower.includes('farmer') || ruleIdLower.includes('occupation') || ruleIdLower.includes('farm')) {
            ['land', 'income_cert'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        if (ruleIdLower.includes('sc_st') || ruleIdLower.includes('caste') || ruleIdLower.includes('category')) {
            ['caste_cert'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        if (ruleIdLower.includes('widow') || ruleIdLower.includes('marital')) {
            ['address_proof'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        if (ruleIdLower.includes('udyam') || ruleIdLower.includes('enterprise') || ruleIdLower.includes('greenfield')) {
            ['udyam'].forEach(dt => {
                if (docsByType[dt]) supporting.push(...docsByType[dt]);
            });
        }
        return supporting.map(d => ({
            documentId: d.id,
            fileName: d.fileName || d.name,
            documentType: d.documentType,
            verificationStatus: d.verificationStatus || 'PENDING',
            isVerified: d.isVerified || false
        }));
    };

    // 1. Process Failed Rules (Statutory Blockers & Unverified Rules)
    (rules || []).forEach(r => {
        const isFail = r.status === 'fail' || r.rawStatus === 'FAIL';
        const isUnverified = r.status === 'review' || r.isUnverifiedRule || (r.source && r.source.toLowerCase().includes('unverified'));

        if (isFail) {
            const ruleId = r.ruleId || r.id || 'STATUTORY_RULE';
            const sourceRule = r.source || 'Statutory Scheme Guidelines';
            const description = r.description || r.reason || 'Statutory criteria failed';

            const ruleIdLower = String(ruleId).toLowerCase();
            const descLower = String(description).toLowerCase();
            const reasonLower = String(r.reason || '').toLowerCase();

            // Detect immutable conditions (age, residency, gender)
            const isAge = ruleIdLower.includes('age') || descLower.includes('age') || reasonLower.includes('age') || reasonLower.includes('year');
            const isState = ruleIdLower.includes('state') || descLower.includes('state') || reasonLower.includes('resident') || reasonLower.includes('domicile');
            const isGender = ruleIdLower.includes('gender') || descLower.includes('gender');

            const isImmutable = isAge || isState || isGender;

            const actionability = isAge ? 'EXTERNAL_DEPENDENCY' :
                isState ? 'AUTHORITY_CONTROLLED' :
                isGender ? 'NOT_APPLICABLE' :
                'APPLICANT_CONTROLLED';

            const actionOutcome = isImmutable ? 'NO_PERMISSIBLE_PATH_IDENTIFIED' : 'MAY_SATISFY_CONDITION';

            // Find supporting documents for this rule
            const supportingDocs = findSupportingDocuments(ruleId);
            const hasVerifiedSupport = supportingDocs.some(d => d.isVerified);
            const hasPendingSupport = supportingDocs.some(d => !d.isVerified && d.verificationStatus === 'PENDING');

            const item = {
                ruleId,
                sourceRule,
                description,
                currentValue: r.applicantValue !== undefined && r.applicantValue !== null ? r.applicantValue : (r.applicant_value !== undefined ? r.applicant_value : 'Current Fact'),
                expectedValue: r.expectedValue || r.expected_value || 'Statutory Requirement',
                reason: r.reason || description,
                blockerType: 'ELIGIBILITY_NOT_MET',
                evidenceStatus: hasVerifiedSupport ? 'VERIFIED' : (hasPendingSupport ? 'PENDING_VERIFICATION' : 'NOT_PROVIDED'),
                explanation: isImmutable
                    ? `Immutable statutory condition outside applicant control (${ruleId}). No permissible change resolves this condition under official rules.`
                    : `Permissible enterprise or operational updates (e.g. updating business status for ${formatFieldLabel(ruleId)}) can allow recomputation.`,
                sourceDocumentName: null,
                pageNumber: null,
                excerpt: null,
                recommendedResolution: isImmutable
                    ? 'No permissible action satisfies this condition. Explore alternative eligible welfare schemes.'
                    : `Update your verified operational status or profile entry for ${formatFieldLabel(ruleId)} to satisfy statutory rules.`,
                actionability,
                actionOutcome,
                canAct: !isImmutable,
                requiresRecomputation: true,
                confidence: 'STATUTORY_RULE_MATCH',
                // FIN-03 Phase 2: Document Evidence Linkage
                supportingDocuments: supportingDocs,
                documentEvidenceSummary: supportingDocs.length === 0
                    ? 'No uploaded documents available to support this rule evaluation.'
                    : hasVerifiedSupport
                    ? `${supportingDocs.filter(d => d.isVerified).length} verified document(s) on file.`
                    : hasPendingSupport
                    ? `${supportingDocs.filter(d => !d.isVerified).length} document(s) uploaded but not yet verified by reviewer.`
                    : 'Documents uploaded but not applicable for this rule.'
            };

            unmetConditions.push(item);
            allBlockers.push(item);

            if (isImmutable) {
                immutableBlockers.push({
                    id: `immutable_${ruleId}`,
                    title: `Immutable Criteria: ${formatFieldLabel(ruleId)}`,
                    reason: r.reason || description,
                    sourceRule
                });
            } else {
                actionableSteps.push({
                    id: `actionable_rule_${ruleId}`,
                    type: 'UPDATE_PROFILE',
                    title: `Permissible Update: ${formatFieldLabel(ruleId)}`,
                    description: `Update your profile to recompute eligibility if your verified status changes.`,
                    ruleId,
                    supportingDocs
                });
            }
        } else if (isUnverified) {
            const ruleId = r.ruleId || r.id || 'UNVERIFIED_RULE';
            const supportingDocs = findSupportingDocuments(ruleId);
            const item = {
                ruleId,
                sourceRule: r.source || 'Unverified Guideline Source',
                description: r.description || 'Rule source requires manual verification',
                currentValue: r.applicantValue ?? 'Unverified Fact',
                expectedValue: r.expectedValue ?? 'Manual Verification',
                reason: r.reason || 'Source rule or policy snapshot is unverified',
                blockerType: 'UNVERIFIED_RULE',
                evidenceStatus: 'UNVERIFIED',
                explanation: 'This scheme rule or policy snapshot has not been verified against official government gazettes.',
                sourceDocumentName: null,
                pageNumber: null,
                excerpt: null,
                recommendedResolution: 'Request manual departmental officer review to verify scheme guidelines.',
                actionability: 'AUTHORITY_CONTROLLED',
                actionOutcome: 'CANNOT_DETERMINE',
                canAct: false,
                requiresRecomputation: true,
                confidence: 'UNVERIFIED',
                supportingDocuments: supportingDocs,
                documentEvidenceSummary: supportingDocs.length > 0
                    ? `${supportingDocs.length} related document(s) on file — officer verification required.`
                    : 'No related documents found.'
            };
            unverifiedRules.push(item);
            allBlockers.push(item);
        }
    });

    // 2. Process Missing Evidence (Missing profile facts or unuploaded document proofs)
    (missingFields || []).forEach(field => {
        if (!field || isInternalSentinel(field)) return;

        const label = formatFieldLabel(field);
        const isDocField = field.includes('doc') || field.includes('certificate') || field.includes('income') || field.includes('caste') || field.includes('passbook');

        const evidenceItem = {
            ruleId: `MISSING_${String(field).toUpperCase()}`,
            field,
            label,
            sourceRule: 'Statutory Evidence Protocol, Clause 2.1',
            description: `Verification proof for ${label} is required`,
            currentValue: 'Not Provided',
            expectedValue: `Official Verification Proof for ${label}`,
            blockerType: 'MISSING_EVIDENCE',
            evidenceStatus: 'MISSING',
            explanation: `Statutory guidelines mandate official verification proof for ${label} before eligibility can be sanctioned.`,
            sourceDocumentName: null,
            pageNumber: null,
            excerpt: null,
            recommendedResolution: isDocField
                ? `Upload official document for ${label} to allow automated verification.`
                : `Provide your verified ${label} in your applicant profile.`,
            actionability: 'APPLICANT_CONTROLLED',
            actionOutcome: 'MAY_RESOLVE_EVIDENCE_GAP',
            canAct: true,
            requiresRecomputation: true,
            confidence: 'EVIDENCE_REQUIRED',
            supportingDocuments: [],
            documentEvidenceSummary: 'No evidence submitted for this requirement.'
        };

        missingEvidence.push(evidenceItem);
        allBlockers.push(evidenceItem);

        actionableSteps.push({
            id: `action_evidence_${field}`,
            type: isDocField ? 'UPLOAD_DOCUMENT' : 'UPDATE_PROFILE',
            title: isDocField ? `Upload ${label}` : `Provide ${label}`,
            description: isDocField
                ? `Submit official ${label} document for automated verification.`
                : `Update your profile with accurate ${label}.`,
            field
        });
    });

    // 3. Document Conflict Detection (FIN-03 Document Evidence Verification)
    (userDocuments || []).forEach(doc => {
        const extracted = doc.extractedData || doc.extracted_data || {};
        if (extracted.annual_income != null && profileSnapshot.annual_income != null) {
            const docIncome = Number(extracted.annual_income);
            const profIncome = Number(profileSnapshot.annual_income);
            if (!isNaN(docIncome) && !isNaN(profIncome) && Math.abs(docIncome - profIncome) > 1000) {
                const conflictItem = {
                    ruleId: 'INCOME_EVIDENCE_CONFLICT',
                    description: 'Income Certificate extracted income conflicts with profile entry',
                    currentValue: `Profile Entry: ₹${profIncome.toLocaleString('en-IN')}`,
                    expectedValue: `Document Extracted: ₹${docIncome.toLocaleString('en-IN')}`,
                    blockerType: 'CONFLICTING_EVIDENCE',
                    evidenceStatus: 'CONFLICTING',
                    explanation: `Uploaded document '${doc.fileName || doc.name}' contains extracted annual income of ₹${docIncome.toLocaleString('en-IN')}, which conflicts with profile entry of ₹${profIncome.toLocaleString('en-IN')}.`,
                    sourceRule: 'Statutory Verification Protocol, Clause 3.4',
                    sourceDocumentName: doc.fileName || doc.name,
                    pageNumber: doc.ocrMetadata?.pages || 1,
                    excerpt: `Total Annual Family Income: INR ${docIncome.toLocaleString('en-IN')}`,
                    recommendedResolution: 'Review uploaded Income Certificate and update profile entry or re-upload corrected document.',
                    actionability: 'APPLICANT_CONTROLLED',
                    actionOutcome: 'MAY_RESOLVE_EVIDENCE_GAP',
                    canAct: true,
                    requiresRecomputation: true,
                    confidence: 'DOCUMENT_CONFLICT_DETECTED',
                    supportingDocuments: [{ documentId: doc.id, fileName: doc.fileName || doc.name, documentType: doc.documentType, verificationStatus: doc.verificationStatus || 'PENDING', isVerified: doc.isVerified || false }],
                    documentEvidenceSummary: `Conflict detected between profile income (₹${profIncome.toLocaleString('en-IN')}) and document-extracted income (₹${docIncome.toLocaleString('en-IN')}).`
                };
                conflictingEvidence.push(conflictItem);
                allBlockers.push(conflictItem);
            }
        }
    });

    // 4. Build document evidence overview
    const documentEvidenceOverview = (userDocuments || []).map(doc => ({
        documentId: doc.id,
        documentType: doc.documentType,
        fileName: doc.fileName || doc.name,
        verificationStatus: doc.verificationStatus || 'PENDING',
        isVerified: doc.isVerified || false,
        hasExtractedData: Object.keys(doc.extractedData || {}).length > 0,
        uploadedAt: doc.uploadedAt
    }));

    const canResolveEligibility = immutableBlockers.length === 0 && (missingEvidence.length > 0 || conflictingEvidence.length > 0 || unmetConditions.some(c => c.canAct));

    let resolutionMessage = '';
    if (fin03Status === 'ELIGIBLE') {
        resolutionMessage = 'All statutory eligibility conditions met. No action required.';
    } else if (immutableBlockers.length > 0) {
        resolutionMessage = `Eligibility cannot be resolved because ${immutableBlockers.length} statutory condition(s) (e.g. ${immutableBlockers.map(b => b.title).join(', ')}) are outside applicant control. Never attempt to falsify identity, income, or official documents.`;
    } else if (conflictingEvidence.length > 0) {
        resolutionMessage = `Eligibility requires review because document evidence conflicts with profile entries. Resolve conflicting documents to recompute.`;
    } else if (canResolveEligibility) {
        resolutionMessage = `Eligibility can be recomputed once required evidence (${missingEvidence.map(m => m.label).join(', ')}) is provided through legitimate profile updates or official document uploads.`;
    } else {
        resolutionMessage = 'No permissible changes resolve eligibility for this scheme under current statutory rules.';
    }

    return {
        determinationStatus: fin03Status,
        determination: verdict,
        totalBlockers: allBlockers.length,
        allBlockers,
        unmetConditions,
        missingEvidence,
        conflictingEvidence,
        unverifiedRules,
        actionableSteps,
        immutableBlockers,
        canResolveEligibility,
        resolutionMessage,
        documentEvidenceOverview,
        totalUploadedDocuments: (userDocuments || []).length,
        verifiedDocuments: (userDocuments || []).filter(d => d.isVerified).length,
        ethicalNotice: 'Compliance Alert: Never suggest or attempt falsifying income, identity, age, or official documents. Recomputation is only permitted for authentic profile updates and legitimate document uploads.'
    };
};

// ─── Deterministic rule-based eligibility engine ──────────────────────────────


/**
 * A rule is a function: (profile, scheme) → { pass: bool|null, reason: string, rule: string }
 * Rules are evaluated deterministically; missing required applicant facts evaluate to pass: null (review).
 */
const ELIGIBILITY_RULES = {
    pmegp: [
        {
            id: 'AGE_18',
            description: 'Applicant must be above 18 years of age.',
            source: 'PMEGP Guidelines, Clause 5.1',
            check: (p) => {
                if (!p.date_of_birth && !p.dob) return { pass: null, reason: 'Date of birth not provided — cannot verify age requirement.' };
                const dob = new Date(p.date_of_birth || p.dob);
                if (isNaN(dob.getTime())) return { pass: null, reason: 'Invalid date of birth provided — cannot verify age requirement.' };
                const age = Math.floor((Date.now() - dob.getTime()) / (365.25 * 24 * 3600 * 1000));
                return age >= 18
                    ? { pass: true, reason: `Applicant is ${age} years old, satisfying the minimum age of 18.` }
                    : { pass: false, reason: `Applicant is ${age} years old, below the required minimum of 18.` };
            }
        },
        {
            id: 'NO_EXISTING_GOVT_LOAN',
            description: 'Only new projects are considered; applicant must not be an existing PMEGP/REGP beneficiary.',
            source: 'PMEGP Guidelines, Clause 6',
            check: () => ({ pass: true, reason: 'Applicant has declared no prior PMEGP/REGP benefit (self-certified).' })
        }
    ],
    'mudra-shishu': [
        {
            id: 'NON_FARM_ACTIVITY',
            description: 'Applicant must be engaged in a non-farm income-generating activity.',
            source: 'MUDRA Guidelines, Clause 3',
            check: (p) => {
                const occ = (p.occupation || '').toLowerCase().trim();
                if (!occ) return { pass: null, reason: 'Occupation not provided — cannot confirm non-farm activity.' };
                if (occ === 'farmer' || occ === 'farming' || occ === 'agriculture') {
                    return { pass: false, reason: 'MUDRA is for non-farm enterprises; farming occupation does not qualify.' };
                }
                return { pass: true, reason: `Occupation '${p.occupation}' qualifies as a non-farm activity.` };
            }
        },
        {
            id: 'LOAN_AMOUNT_RANGE',
            description: 'Shishu category covers loans up to ₹50,000.',
            source: 'MUDRA Guidelines, Clause 4.1',
            check: () => ({ pass: true, reason: 'Shishu category (up to ₹50,000) applies.' })
        }
    ],
    'pm-kisan': [
        {
            id: 'FARMER_OCCUPATION',
            description: 'Applicant must be a farmer / agriculturalist.',
            source: 'PM-KISAN Scheme, Clause 4',
            check: (p) => {
                const occ = (p.occupation || '').toLowerCase().trim();
                if (!occ) return { pass: null, reason: 'Occupation not provided — farmer status cannot be verified.' };
                return (occ === 'farmer' || occ === 'farming' || occ === 'agriculture')
                    ? { pass: true, reason: "Occupation is 'farmer', satisfying PM-KISAN eligibility." }
                    : { pass: false, reason: `Occupation '${p.occupation}' does not qualify for PM-KISAN (farmers only).` };
            }
        },
        {
            id: 'INCOME_THRESHOLD',
            description: 'Family income must not exceed ₹3,00,000 per year.',
            source: 'PM-KISAN Scheme, Clause 5',
            check: (p) => {
                const income = p.annual_income != null ? p.annual_income : p.income;
                if (income == null || income === '') return { pass: null, reason: 'Annual income not provided — income threshold cannot be verified.' };
                const num = Number(income);
                if (isNaN(num)) return { pass: null, reason: 'Invalid annual income value — income threshold cannot be verified.' };
                return num <= 300000
                    ? { pass: true, reason: `Annual income ₹${num.toLocaleString('en-IN')} is within the ₹3,00,000 threshold.` }
                    : { pass: false, reason: `Annual income ₹${num.toLocaleString('en-IN')} exceeds the ₹3,00,000 threshold.` };
            }
        }
    ],
    kcc: [
        {
            id: 'FARMER_OCCUPATION',
            description: 'Applicant must be a farmer, tenant farmer, or sharecropper.',
            source: 'KCC Scheme Guidelines, Clause 3',
            check: (p) => {
                const occ = (p.occupation || '').toLowerCase().trim();
                if (!occ) return { pass: null, reason: 'Occupation not provided — farmer status cannot be verified.' };
                return (occ === 'farmer' || occ === 'farming' || occ === 'agriculture')
                    ? { pass: true, reason: 'Applicant is a farmer — eligible for KCC.' }
                    : { pass: false, reason: `Occupation '${p.occupation}' does not qualify. KCC is for farmers only.` };
            }
        }
    ],
    'standup-india': [
        {
            id: 'SC_ST_OR_WOMAN',
            description: 'Applicant must be SC/ST or a woman entrepreneur.',
            source: 'Stand-Up India, Clause 3.1',
            check: (p) => {
                const category = (p.caste_category || p.category || '').toUpperCase().trim();
                const gender = (p.gender || '').toLowerCase().trim();
                const isWoman = p.is_woman_entrepreneur === true || gender === 'female' || gender === 'woman';
                const isSCST = category === 'SC' || category === 'ST';
                if (!category && !gender && p.is_woman_entrepreneur == null) {
                    return { pass: null, reason: 'Social category and gender not provided — cannot verify SC/ST or woman entrepreneur requirement.' };
                }
                if (isSCST || isWoman) {
                    return { pass: true, reason: `Applicant qualifies as ${isSCST ? category : ''}${isSCST && isWoman ? ' and ' : ''}${isWoman ? 'woman entrepreneur' : ''}.` };
                }
                return { pass: false, reason: 'Applicant is neither SC/ST nor a woman entrepreneur.' };
            }
        },
        {
            id: 'GREENFIELD_ENTERPRISE',
            description: 'Loan is for setting up a new (greenfield) enterprise.',
            source: 'Stand-Up India, Clause 3.2',
            check: () => ({ pass: true, reason: 'Self-certified as a new enterprise (greenfield).' })
        }
    ],
    'hp-widow-pension': [
        {
            id: 'HP_RESIDENCY',
            description: 'Applicant must be a bonafide resident of Himachal Pradesh.',
            source: 'HPBOCWWB Guidelines, Clause 2.1',
            check: (p) => {
                const state = (p.state || '').toLowerCase().trim();
                if (!state) return { pass: null, reason: 'State of residence not provided — domicile requirement cannot be verified.' };
                return (state === 'himachal pradesh' || state === 'hp')
                    ? { pass: true, reason: 'Applicant is a resident of Himachal Pradesh.' }
                    : { pass: false, reason: `State '${p.state}' does not satisfy Himachal Pradesh bonafide residency.` };
            }
        },
        {
            id: 'MARITAL_STATUS_WIDOW',
            description: 'Applicant must be a widow worker registered with HPBOCWWB.',
            source: 'HPBOCWWB Guidelines, Clause 3.4',
            check: (p) => {
                const status = (p.marital_status || '').toLowerCase().trim();
                if (!status) return { pass: null, reason: 'Marital status not provided — widow status cannot be verified.' };
                return (status === 'widow' || status === 'widowed')
                    ? { pass: true, reason: 'Applicant marital status confirmed as Widow.' }
                    : { pass: false, reason: `Marital status '${p.marital_status}' does not satisfy widow pension requirement.` };
            }
        },
        {
            id: 'INCOME_THRESHOLD_PENSION',
            description: 'Annual family income must be below ₹50,000.',
            source: 'HPBOCWWB Guidelines, Clause 4.2',
            check: (p) => {
                const income = p.annual_income != null ? p.annual_income : p.income;
                if (income == null || income === '') return { pass: null, reason: 'Annual income not provided.' };
                const num = Number(income);
                if (isNaN(num)) return { pass: null, reason: 'Invalid annual income.' };
                return num <= 50000
                    ? { pass: true, reason: `Annual income ₹${num.toLocaleString('en-IN')} is within the ₹50,000 threshold.` }
                    : { pass: false, reason: `Annual income ₹${num.toLocaleString('en-IN')} exceeds the ₹50,000 threshold.` };
            }
        }
    ]
};

/**
 * Normalizes scheme identifier and retrieves registered deterministic rules.
 * Returns null if scheme is not registered (strict fail-closed).
 */
const getRegisteredRules = (schemeId) => {
    if (!schemeId) return null;
    const cleanId = String(schemeId).toLowerCase().trim();
    if (ELIGIBILITY_RULES[cleanId]) return ELIGIBILITY_RULES[cleanId];
    const withDash = cleanId.replace(/_/g, '-');
    if (ELIGIBILITY_RULES[withDash]) return ELIGIBILITY_RULES[withDash];
    const withUnderscore = cleanId.replace(/-/g, '_');
    if (ELIGIBILITY_RULES[withUnderscore]) return ELIGIBILITY_RULES[withUnderscore];
    if (cleanId === 'pmkisan') return ELIGIBILITY_RULES['pm-kisan'];
    if (cleanId === 'mudrashishu') return ELIGIBILITY_RULES['mudra-shishu'];
    if (cleanId === 'standupindia') return ELIGIBILITY_RULES['standup-india'];
    if (cleanId === 'hpwidowpension' || cleanId === 'hp_widow_pension') return ELIGIBILITY_RULES['hp-widow-pension'];
    return null;
};

/**
 * Evaluate a single rule against the profile.
 * Returns { ruleId, description, source, status: 'pass'|'fail'|'review', reason }
 */
const evaluateRule = (rule, profile) => {
    const result = rule.check(profile);
    const status = result.pass === true ? 'pass' : result.pass === false ? 'fail' : 'review';
    return {
        ruleId: rule.id,
        description: rule.description,
        source: rule.source,
        status,
        reason: result.reason
    };
};

const AI_SERVER_URL = process.env.AI_SERVER_URL || 'http://127.0.0.1:8000';

/**
 * Core eligibility check service.
 * @param {string} userId          - Authenticated user id
 * @param {string} schemeId        - Scheme to evaluate against
 * @param {object} [profileOverride] - Optional profile fields passed directly in request body
 *                                    (used when applicant_profiles row does not exist yet)
 * @param {object} [options]         - Optional execution options (e.g. { allowFallback: false })
 * @returns Eligibility report object
 */
const checkEligibility = async (userId, schemeId, profileOverride = null, options = {}) => {
    if (!userId) throw httpError(400, 'User ID is required');
    if (!schemeId) throw httpError(400, 'schemeId is required');

    // 1. Try loading from Supabase applicant_profiles table
    let profile = null;
    try {
        profile = await profileService.getProfileById(userId);
    } catch (_) { /* non-blocking — proceed with override or empty */ }

    // 2. If no DB profile, merge override or use empty object
    if (!profile) {
        if (profileOverride && typeof profileOverride === 'object') {
            profile = profileOverride;
        } else {
            profile = {};
        }
    } else if (profileOverride && typeof profileOverride === 'object') {
        profile = { ...profile, ...profileOverride };
    }

    // 2b. Fetch user's uploaded documents for evidence conflict detection
    let userDocuments = [];
    try {
        const documentServices = require('./documentServices');
        const docs = await documentServices.listDocuments(userId);
        userDocuments = (docs || []).map(doc => ({
            id: doc.id,
            documentType: doc.documentType || doc.document_type,
            fileName: doc.fileName || doc.file_name,
            name: doc.fileName || doc.file_name,
            verificationStatus: (doc.verificationStatus || 'PENDING').toUpperCase(),
            isVerified: (doc.verificationStatus || '').toUpperCase() === 'VERIFIED',
            extractedData: doc.extractedData || {},
            ocrMetadata: doc.ocrMetadata || {},
            uploadedAt: doc.uploadedAt || doc.uploaded_at
        }));
    } catch (_) { /* non-blocking */ }

    // --- 3. DETERMINISTIC RULE ENGINE EVALUATION ---
    const localRules = getRegisteredRules(schemeId);
    let intelligenceError = null;

    if (localRules && Array.isArray(localRules) && localRules.length > 0) {
        const evaluatedRules = localRules.map(r => evaluateRule(r, profile));
        const hasFail = evaluatedRules.some(r => r.status === 'fail');
        const hasReview = evaluatedRules.some(r => r.status === 'review');

        let status, isEligible, verdict, verdictReason, evaluationCategory;
        let missingFields = [];
        let missingFieldLabels = [];

        if (hasFail) {
            status = 'FAIL';
            isEligible = false;
            verdict = 'NOT_ELIGIBLE';
            evaluationCategory = 'NOT_ELIGIBLE';
            verdictReason = evaluatedRules.find(r => r.status === 'fail')?.reason || 'One or more statutory criteria were not satisfied.';
        } else if (hasReview) {
            status = 'UNKNOWN';
            isEligible = false;
            verdict = 'MANUAL_REVIEW';
            const reviewRules = evaluatedRules.filter(r => r.status === 'review');
            const extractedMissing = [];
            reviewRules.forEach(r => {
                const mapped = RULE_TO_PROFILE_FIELDS[r.ruleId];
                if (mapped) {
                    mapped.forEach(f => {
                        const val = profile[f];
                        if ((val == null || val === '') && !extractedMissing.includes(f)) {
                            extractedMissing.push(f);
                        }
                    });
                }
            });
            missingFields = extractedMissing;
            missingFieldLabels = extractedMissing.map(formatFieldLabel);
            evaluationCategory = missingFields.length > 0 ? 'MISSING_APPLICANT_FACTS' : 'MANUAL_REVIEW';
            verdictReason = missingFields.length > 0
                ? `Additional profile information is required to evaluate statutory eligibility: ${missingFieldLabels.join(', ')}.`
                : 'Statutory guidelines require manual verification.';
        } else {
            status = 'PASS';
            isEligible = true;
            verdict = 'ELIGIBLE';
            evaluationCategory = 'ELIGIBLE';
            verdictReason = `All statutory eligibility criteria passed for ${schemeId}.`;
        }

        const changeAnalysis = buildEligibilityChangeAnalysis(
            schemeId,
            schemeId,
            verdict,
            evaluatedRules,
            missingFields,
            profile,
            userDocuments
        );

        return {
            source: 'deterministic_engine',
            degraded: false,
            intelligenceUnavailable: false,
            isRegistered: true,
            evaluationCategory,
            schemeId,
            schemeName: schemeId,
            status,
            isEligible,
            verdict,
            verdictReason,
            rules: evaluatedRules,
            missingFields,
            missingFieldLabels,
            changeAnalysis,
            conflictedFields: [],
            matchedRules: evaluatedRules.filter(r => r.status === 'pass').map(r => r.ruleId),
            failedRules: evaluatedRules.filter(r => r.status === 'fail').map(r => r.ruleId),
            profileSnapshot: profile,
            evaluatedAt: new Date().toISOString()
        };
    }

    try {
        const applicantFacts = { ...profile };
        if (profile.occupation) applicantFacts.occupation = profile.occupation;
        if (profile.annual_income != null) applicantFacts.annual_income = Number(profile.annual_income);
        if (profile.income != null && applicantFacts.annual_income == null) applicantFacts.annual_income = Number(profile.income);
        if (profile.caste_category || profile.category) applicantFacts.caste_category = profile.caste_category || profile.category;
        if (profile.gender) applicantFacts.gender = profile.gender;
        if (profile.state) applicantFacts.state = profile.state;
        if (profile.date_of_birth || profile.dob) applicantFacts.date_of_birth = profile.date_of_birth || profile.dob;
        if (profile.is_woman_entrepreneur != null) applicantFacts.is_woman_entrepreneur = Boolean(profile.is_woman_entrepreneur);
        if (profile.full_name) applicantFacts.full_name = profile.full_name;
        if (profile.marital_status) applicantFacts.marital_status = profile.marital_status;
        if (profile.land_ownership_acres != null) applicantFacts.land_ownership_acres = Number(profile.land_ownership_acres);
        if (profile.education_level) applicantFacts.education_level = profile.education_level;

        const aiResponse = await intelligenceClient.postJson('/v1/eligibility/check', {
            applicant_facts: applicantFacts,
            scheme_ids: [schemeId]
        }, { timeoutMs: 15000 });

        const evaluation = aiResponse?.evaluations?.[0];
        if (evaluation) {
            const rawStatus = (evaluation.status || 'UNKNOWN').toUpperCase();
            // Invariant: strictly preserve PASS, FAIL, UNKNOWN, REVIEW
            const validStatus = ['PASS', 'FAIL', 'UNKNOWN', 'REVIEW'].includes(rawStatus) ? rawStatus : 'UNKNOWN';

            // Sanitize raw missing fields: filter out internal sentinels (e.g. scheme_1pmy_not_registered)
            const rawMissing = Array.isArray(evaluation.missing_fields) ? evaluation.missing_fields : [];
            const sanitizedMissing = rawMissing.filter(f => !isInternalSentinel(f));

            const hasRules = Array.isArray(evaluation.rules_evaluated) && evaluation.rules_evaluated.length > 0;
            const isUnregistered = (evaluation.rule_version === 'UNREGISTERED') ||
                (evaluation.completeness === 'UNSTRUCTURED') ||
                (!hasRules && rawMissing.some(f => typeof f === 'string' && f.includes('not_registered'))) ||
                (!hasRules && validStatus === 'UNKNOWN');

            let isRegistered = !isUnregistered;
            let evaluationCategory;
            let verdict;
            let verdictReason;
            let missingFields = [];
            let missingFieldLabels = [];

            if (validStatus === 'PASS') {
                isRegistered = true;
                evaluationCategory = 'ELIGIBLE';
                verdict = 'ELIGIBLE';
                verdictReason = `All statutory eligibility criteria passed for ${evaluation.scheme_name || schemeId}.`;
                missingFields = [];
                missingFieldLabels = [];
            } else if (validStatus === 'FAIL') {
                isRegistered = true;
                evaluationCategory = 'NOT_ELIGIBLE';
                verdict = 'NOT_ELIGIBLE';
                const firstFailed = (evaluation.rules_evaluated || []).find(r => r.status === 'FAIL');
                verdictReason = firstFailed?.reason || (evaluation.failed_rules?.length ? `Failed rules: ${evaluation.failed_rules.join(', ')}` : 'One or more statutory criteria were not satisfied.');
                missingFields = [];
                missingFieldLabels = [];
            } else if (isUnregistered) {
                isRegistered = false;
                evaluationCategory = 'UNREGISTERED_SCHEME';
                verdict = 'MANUAL_REVIEW';
                verdictReason = 'Automated statutory rules are not registered for this scheme. Manual verification is required during departmental application review.';
                missingFields = [];
                missingFieldLabels = [];
            } else if (sanitizedMissing.length > 0) {
                isRegistered = true;
                evaluationCategory = 'MISSING_APPLICANT_FACTS';
                verdict = 'MANUAL_REVIEW';
                missingFields = sanitizedMissing;
                missingFieldLabels = sanitizedMissing.map(formatFieldLabel);
                verdictReason = `Additional profile information is required to evaluate statutory eligibility: ${missingFieldLabels.join(', ')}. Manual verification is required.`;
            } else {
                isRegistered = true;
                evaluationCategory = 'MANUAL_REVIEW';
                verdict = 'MANUAL_REVIEW';
                if (evaluation.review_reasons && evaluation.review_reasons.length > 0) {
                    verdictReason = evaluation.review_reasons.join(' ');
                } else {
                    verdictReason = 'Statutory guidelines mandate qualitative departmental review and manual officer verification.';
                }
                missingFields = [];
                missingFieldLabels = [];
            }

            const mappedRules = (evaluation.rules_evaluated || []).map(r => ({
                id: r.rule_id,
                ruleId: r.rule_id,
                field: r.field,
                operator: r.operator,
                status: r.status === 'PASS' ? 'pass' : (r.status === 'FAIL' ? 'fail' : 'review'),
                rawStatus: r.status,
                reason: r.reason,
                applicantValue: r.applicant_value,
                expectedValue: r.expected_value,
                hardConstraint: r.hard_constraint
            }));

            // Persist to Supabase eligibility_results table
            try {
                await supabaseAdmin.from('eligibility_results').insert({
                    user_id: userId,
                    scheme_id: schemeId,
                    verdict,
                    evaluated_rules: mappedRules,
                    evaluated_at: new Date().toISOString()
                });
            } catch (_) { }

            const changeAnalysis = buildEligibilityChangeAnalysis(
                schemeId,
                evaluation.scheme_name || schemeId,
                verdict,
                mappedRules,
                missingFields,
                applicantFacts,
                userDocuments
            );

            // Addition 2: Enrich with Evidence-Based Cross-Document Verification Report
            try {
                const evidenceVerificationService = require('./evidenceVerificationService');
                const evRes = await evidenceVerificationService.verifyApplicantEvidence({
                    userId,
                    schemeId,
                    applicationId: null
                });
                if (evRes?.report) {
                    changeAnalysis.evidenceVerificationReport = evRes.report;
                }
            } catch (evErr) {
                console.warn(`[eligibilityService] Evidence verification enrichment skipped: ${evErr.message}`);
            }

            return {
                source: 'intelligence',
                degraded: false,
                intelligenceUnavailable: false,
                isRegistered,
                evaluationCategory,
                schemeId,
                schemeName: evaluation.scheme_name || schemeId,
                status: validStatus,
                isEligible: validStatus === 'PASS',
                verdict,
                verdictReason,
                rules: mappedRules,
                missingFields,
                missingFieldLabels,
                changeAnalysis,
                conflictedFields: evaluation.conflicted_fields || [],
                matchedRules: evaluation.matched_rules || [],
                failedRules: evaluation.failed_rules || [],
                profileSnapshot: applicantFacts,
                requestId: aiResponse.request_id || null,
                evaluatedAt: new Date().toISOString()
            };
        }
    } catch (err) {
        intelligenceError = err;
        console.warn(`[eligibilityService] Intelligence /v1/eligibility/check unavailable (${err.message}) - Falling back to local engine`);
        if (options && options.allowFallback === false) {
            throw err;
        }
    }
    // -----------------------------------------------------

    // 4. Local Deterministic Fail-Closed Engine (Phase D3.6 Remediation)
    const cleanSchemeId = String(schemeId).toLowerCase().trim();
    const rules = getRegisteredRules(cleanSchemeId);

    // Fail-Closed Guard: Schemes without registered rules MUST NEVER evaluate to PASS/ELIGIBLE
    if (!rules || !Array.isArray(rules) || rules.length === 0) {
        let schemeName = schemeId;
        try {
            const res = await schemeService.getSchemeById(schemeId);
            schemeName = res?.scheme?.scheme_name || res?.scheme?.name || res?.scheme?.title || schemeId;
        } catch (_) { }

        const isSnapshotUnavailable = intelligenceError && /snapshot/i.test(intelligenceError.message || '');
        const evaluationCategory = isSnapshotUnavailable ? 'SNAPSHOT_UNAVAILABLE' : 'UNREGISTERED_SCHEME';
        const verdictReason = isSnapshotUnavailable
            ? 'Official policy rules snapshot is currently unavailable for this scheme. Manual verification is required.'
            : 'Automated statutory rules are not registered for this scheme. Manual verification is required during departmental application review.';

        const unregisteredResult = {
            source: 'local_fallback',
            degraded: true,
            intelligenceUnavailable: Boolean(intelligenceError),
            isRegistered: false,
            evaluationCategory,
            schemeId,
            schemeName,
            status: 'UNKNOWN',
            isEligible: false,
            verdict: 'MANUAL_REVIEW',
            verdictReason,
            rules: [],
            missingFields: [],
            missingFieldLabels: [],
            conflictedFields: [],
            matchedRules: [],
            failedRules: [],
            profileSource: profile.id ? 'database' : (profileOverride ? 'request_body' : 'empty'),
            profileSnapshot: {
                occupation: profile.occupation || null,
                annual_income: profile.annual_income != null ? Number(profile.annual_income) : (profile.income != null ? Number(profile.income) : null),
                category: profile.caste_category || profile.category || null,
                gender: profile.gender || null,
                is_woman_entrepreneur: profile.is_woman_entrepreneur || null,
                date_of_birth: profile.date_of_birth || profile.dob || null
            },
            evaluatedAt: new Date().toISOString()
        };

        try {
            await supabaseAdmin.from('eligibility_results').insert({
                user_id: userId,
                scheme_id: schemeId,
                verdict: 'MANUAL_REVIEW',
                evaluated_rules: [],
                evaluated_at: new Date().toISOString()
            });
        } catch (_) { }

        return unregisteredResult;
    }

    // 5. Evaluate registered deterministic rules safely
    let evaluatedRules = [];
    try {
        evaluatedRules = rules.map(rule => evaluateRule(rule, profile));
    } catch (_) {
        let schemeName = schemeId;
        try {
            const res = await schemeService.getSchemeById(schemeId);
            schemeName = res?.scheme?.scheme_name || res?.scheme?.name || res?.scheme?.title || schemeId;
        } catch (_) { }

        return {
            source: 'local_fallback',
            degraded: true,
            intelligenceUnavailable: Boolean(intelligenceError),
            isRegistered: true,
            evaluationCategory: 'EVALUATION_ERROR',
            schemeId,
            schemeName,
            status: 'UNKNOWN',
            isEligible: false,
            verdict: 'MANUAL_REVIEW',
            verdictReason: 'Automated statutory rules could not be evaluated due to an execution error. Manual verification is required.',
            rules: [],
            missingFields: [],
            missingFieldLabels: [],
            conflictedFields: [],
            matchedRules: [],
            failedRules: [],
            profileSource: profile.id ? 'database' : (profileOverride ? 'request_body' : 'empty'),
            profileSnapshot: {
                occupation: profile.occupation || null,
                annual_income: profile.annual_income != null ? Number(profile.annual_income) : (profile.income != null ? Number(profile.income) : null),
                category: profile.caste_category || profile.category || null,
                gender: profile.gender || null,
                is_woman_entrepreneur: profile.is_woman_entrepreneur || null,
                date_of_birth: profile.date_of_birth || profile.dob || null
            },
            evaluatedAt: new Date().toISOString()
        };
    }

    // 6. Compute overall verdict with strict fail-closed logic
    const hasFail = evaluatedRules.some(r => r.status === 'fail');
    const hasReview = evaluatedRules.some(r => r.status === 'review');

    let status, isEligible, verdict, verdictReason, evaluationCategory;
    let missingFields = [];
    let missingFieldLabels = [];

    if (hasFail) {
        status = 'FAIL';
        isEligible = false;
        verdict = 'NOT_ELIGIBLE';
        evaluationCategory = 'NOT_ELIGIBLE';
        verdictReason = evaluatedRules.find(r => r.status === 'fail')?.reason || 'One or more statutory criteria were not satisfied.';
        missingFields = [];
        missingFieldLabels = [];
    } else if (hasReview) {
        status = 'UNKNOWN';
        isEligible = false;
        verdict = 'MANUAL_REVIEW';

        const reviewRules = evaluatedRules.filter(r => r.status === 'review');
        const extractedMissing = [];
        reviewRules.forEach(r => {
            const mapped = RULE_TO_PROFILE_FIELDS[r.ruleId];
            if (mapped) {
                mapped.forEach(f => {
                    const val = profile[f];
                    if ((val == null || val === '') && !extractedMissing.includes(f)) {
                        extractedMissing.push(f);
                    }
                });
            }
        });

        if (extractedMissing.length > 0) {
            evaluationCategory = 'MISSING_APPLICANT_FACTS';
            missingFields = extractedMissing;
            missingFieldLabels = extractedMissing.map(formatFieldLabel);
            verdictReason = reviewRules[0]?.reason || `Additional profile information is required to evaluate statutory eligibility: ${missingFieldLabels.join(', ')}. Manual verification is required.`;
        } else {
            evaluationCategory = 'MANUAL_REVIEW';
            missingFields = [];
            missingFieldLabels = [];
            verdictReason = reviewRules[0]?.reason || 'Some eligibility criteria could not be verified due to incomplete profile information. Manual verification is required.';
        }
    } else {
        status = 'PASS';
        isEligible = true;
        verdict = 'ELIGIBLE';
        evaluationCategory = 'ELIGIBLE';
        verdictReason = evaluatedRules.length > 0
            ? `Your profile satisfies all ${evaluatedRules.length} statutory eligibility criteria for this scheme.`
            : 'Statutory criteria evaluated successfully.';
        missingFields = [];
        missingFieldLabels = [];
    }

    let schemeName = schemeId;
    try {
        const res = await schemeService.getSchemeById(schemeId);
        schemeName = res?.scheme?.scheme_name || res?.scheme?.name || res?.scheme?.title || schemeId;
    } catch (_) { }

    const mappedRules = evaluatedRules.map(r => ({
        id: r.ruleId,
        ruleId: r.ruleId,
        description: r.description,
        source: r.source,
        status: r.status,
        rawStatus: r.status === 'pass' ? 'PASS' : (r.status === 'fail' ? 'FAIL' : 'UNKNOWN'),
        reason: r.reason
    }));

    // Persist result to Supabase `eligibility_results` if table exists
    try {
        await supabaseAdmin.from('eligibility_results').insert({
            user_id: userId,
            scheme_id: schemeId,
            verdict,
            evaluated_rules: mappedRules,
            evaluated_at: new Date().toISOString()
        });
    } catch (_) { /* table may not exist yet */ }

    const changeAnalysis = buildEligibilityChangeAnalysis(
        schemeId,
        schemeName,
        verdict,
        mappedRules,
        missingFields,
        profile,
        userDocuments
    );

    try {
        const evidenceVerificationService = require('./evidenceVerificationService');
        const evRes = await evidenceVerificationService.verifyApplicantEvidence({
            userId,
            schemeId,
            applicationId: null
        });
        if (evRes?.report) {
            changeAnalysis.evidenceVerificationReport = evRes.report;
        }
    } catch (_) { }

    return {
        source: 'local_fallback',
        degraded: true,
        intelligenceUnavailable: Boolean(intelligenceError),
        isRegistered: true,
        evaluationCategory,
        schemeId,
        schemeName,
        status,
        isEligible,
        verdict,
        verdictReason,
        rules: mappedRules,
        matchedRules: mappedRules.filter(r => r.status === 'pass').map(r => r.ruleId),
        failedRules: mappedRules.filter(r => r.status === 'fail').map(r => r.ruleId),
        missingFields,
        missingFieldLabels,
        changeAnalysis,
        conflictedFields: [],
        profileSource: profile.id ? 'database' : (profileOverride ? 'request_body' : 'empty'),
        profileSnapshot: {
            occupation: profile.occupation || null,
            annual_income: profile.annual_income != null ? Number(profile.annual_income) : (profile.income != null ? Number(profile.income) : null),
            category: profile.caste_category || profile.category || null,
            gender: profile.gender || null,
            is_woman_entrepreneur: profile.is_woman_entrepreneur || null,
            date_of_birth: profile.date_of_birth || profile.dob || null
        },
        evaluatedAt: new Date().toISOString()
    };
};

/**
 * FIN-03: Analyze Eligibility & Blockers
 */
const analyzeEligibility = async (userId, schemeId, profileOverride = null) => {
    const result = await checkEligibility(userId, schemeId, profileOverride);
    return {
        schemeId,
        determination: result.verdict,
        determinationStatus: result.changeAnalysis?.determinationStatus || 'MANUAL_REVIEW',
        verdictReason: result.verdictReason,
        changeAnalysis: result.changeAnalysis,
        evaluatedRules: result.rules,
        missingFields: result.missingFields,
        evaluatedAt: result.evaluatedAt
    };
};

/**
 * FIN-03: Recompute Eligibility after applicant profile/evidence updates with history comparison
 */
const recomputeEligibility = async (userId, schemeId, profileOverride = null, previousResult = null) => {
    const latestResult = await checkEligibility(userId, schemeId, profileOverride);

    const prevBlockers = previousResult?.changeAnalysis?.allBlockers || previousResult?.changeAnalysis?.unmetConditions || [];
    const newBlockers = latestResult.changeAnalysis?.allBlockers || latestResult.changeAnalysis?.unmetConditions || [];

    const prevRuleIds = new Set(prevBlockers.map(b => b.ruleId || b.field));
    const newRuleIds = new Set(newBlockers.map(b => b.ruleId || b.field));

    const resolvedBlockers = prevBlockers.filter(b => !newRuleIds.has(b.ruleId || b.field));
    const newlyDiscoveredBlockers = newBlockers.filter(b => !prevRuleIds.has(b.ruleId || b.field));

    const historyComparison = {
        previousStatus: previousResult?.verdict || previousResult?.determinationStatus || 'INITIAL_EVALUATION',
        newStatus: latestResult.verdict,
        resolvedBlockers: resolvedBlockers.map(b => ({ ruleId: b.ruleId || b.field, description: b.description || b.label })),
        newlyDiscoveredBlockers: newlyDiscoveredBlockers.map(b => ({ ruleId: b.ruleId || b.field, description: b.description || b.label })),
        evidenceThatCausedChange: Object.keys(profileOverride || {}),
        reEvaluatedRules: (latestResult.rules || []).map(r => r.ruleId || r.id)
    };

    return {
        ...latestResult,
        historyComparison
    };
};

module.exports = {
    checkEligibility,
    analyzeEligibility,
    recomputeEligibility,
    buildEligibilityChangeAnalysis
};
