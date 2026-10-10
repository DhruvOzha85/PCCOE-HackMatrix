/**
 * Evidence Verification Controller (FIN-03 Addition 2)
 * Handles API endpoints for:
 *   - POST /api/eligibility/:schemeId/verify-evidence
 *   - GET  /api/eligibility/:schemeId/evidence-results
 *   - POST /api/eligibility/evaluate-baselines
 *   - GET  /api/reviewer/evidence/conflicts
 *   - POST /api/reviewer/evidence/conflicts/:conflictId/resolve
 */

const evidenceVerificationService = require('../services/evidenceVerificationService');

/**
 * Starts or re-runs evidence verification for an applicant against a scheme.
 */
const verifyEvidence = async (req, res, next) => {
    try {
        const userId = req.user?.id;
        const { schemeId } = req.params;
        const { applicationId, assessmentDate } = req.body || {};

        if (!schemeId) {
            return res.status(400).json({
                success: false,
                message: 'Scheme ID is required in URL parameter'
            });
        }

        const result = await evidenceVerificationService.verifyApplicantEvidence({
            userId,
            schemeId,
            applicationId,
            assessmentDate
        });

        return res.status(200).json(result);
    } catch (err) {
        return next(err);
    }
};

/**
 * Retrieves the condition-level evidence results for an application or scheme.
 */
const getEvidenceResults = async (req, res, next) => {
    try {
        const userId = req.user?.id;
        const { schemeId } = req.params;
        const { applicationId } = req.query;

        const targetId = applicationId || `${userId}_${schemeId}`;
        const report = await evidenceVerificationService.getEvidenceResults(targetId, userId);

        if (!report) {
            // If not yet verified, run verification now
            const newRes = await evidenceVerificationService.verifyApplicantEvidence({
                userId,
                schemeId,
                applicationId
            });
            return res.status(200).json({
                success: true,
                report: newRes.report
            });
        }

        return res.status(200).json({
            success: true,
            report
        });
    } catch (err) {
        return next(err);
    }
};

/**
 * Lists cross-document conflicts for reviewer review queue.
 */
const getReviewerConflicts = async (req, res, next) => {
    try {
        const { status } = req.query;
        const conflicts = await evidenceVerificationService.listReviewerConflicts({ status });
        return res.status(200).json({
            success: true,
            count: conflicts.length,
            conflicts
        });
    } catch (err) {
        return next(err);
    }
};

/**
 * Resolves an open cross-document conflict (Reviewer / Caseworker action).
 */
const resolveConflict = async (req, res, next) => {
    try {
        const { conflictId } = req.params;
        const reviewerUser = req.user;
        const { reason, selectedSource, authoritativeValue } = req.body || {};

        if (!reason || typeof reason !== 'string' || !reason.trim()) {
            return res.status(400).json({
                success: false,
                message: 'A mandatory resolution reason / justification is required.'
            });
        }

        const resolved = await evidenceVerificationService.resolveConflict(
            conflictId,
            reviewerUser,
            {
                reason: reason.trim(),
                selectedSource: selectedSource || 'DOCUMENT',
                authoritativeValue
            }
        );

        return res.status(200).json({
            success: true,
            message: `Conflict ${conflictId} resolved successfully.`,
            conflict: resolved
        });
    } catch (err) {
        return next(err);
    }
};

/**
 * Runs reproducible benchmark comparing Baseline A, Baseline B, and Proposed NLP Verifier.
 */
const evaluateBaselines = async (req, res, next) => {
    try {
        const { split } = req.body || {};
        const result = await evidenceVerificationService.evaluateBaselines(split || 'held_out_test');
        return res.status(200).json(result);
    } catch (err) {
        return next(err);
    }
};

module.exports = {
    verifyEvidence,
    getEvidenceResults,
    getReviewerConflicts,
    resolveConflict,
    evaluateBaselines
};
