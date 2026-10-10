const express = require('express');
const authMiddleware = require('../middleware/authMiddleware');
const { requireReviewer } = require('../middleware/roleMiddleware');
const reviewerController = require('../controllers/reviewerController');
const evidenceController = require('../controllers/evidenceController');

const router = express.Router();

// Strict server-side reviewer authorization guard
router.use(authMiddleware);
router.use(requireReviewer);

router.get('/dashboard', reviewerController.getDashboardStats);
router.get('/applications', reviewerController.getApplicationsQueue);
router.get('/applications/:id', reviewerController.getApplicationDetails);
router.post('/applications/:id/decision', reviewerController.submitDecision);
router.post('/documents/:id/verify', reviewerController.verifyDocument);

// Addition 2: Cross-Document Conflict Review Workflow
router.get('/evidence/conflicts', evidenceController.getReviewerConflicts);
router.post('/evidence/conflicts/:conflictId/resolve', evidenceController.resolveConflict);

module.exports = router;
