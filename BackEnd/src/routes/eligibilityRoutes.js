const express = require('express');
const {
    checkEligibility,
    analyzeEligibility,
    getChangeAnalysis,
    recomputeEligibility
} = require('../controllers/eligibilityController');
const evidenceController = require('../controllers/evidenceController');
const authMiddleware = require('../middleware/authMiddleware');

const router = express.Router();

// Existing & FIN-03 Endpoints (All require authentication)
router.post('/check', authMiddleware, checkEligibility);
router.post('/analyze', authMiddleware, analyzeEligibility);
router.get('/:schemeId/change-analysis', authMiddleware, getChangeAnalysis);
router.post('/:schemeId/recompute', authMiddleware, recomputeEligibility);

// Addition 2: Evidence-Based Verification Endpoints
router.post('/:schemeId/verify-evidence', authMiddleware, evidenceController.verifyEvidence);
router.get('/:schemeId/evidence-results', authMiddleware, evidenceController.getEvidenceResults);
router.post('/evaluate-baselines', authMiddleware, evidenceController.evaluateBaselines);

module.exports = router;
