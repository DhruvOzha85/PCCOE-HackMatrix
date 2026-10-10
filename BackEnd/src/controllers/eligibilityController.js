const eligibilityService = require('../services/eligibilityService');

const checkEligibility = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const { schemeId, profile } = req.body;

        if (!schemeId) {
            return res.status(400).json({ success: false, message: 'schemeId is required in request body' });
        }

        const result = await eligibilityService.checkEligibility(userId, schemeId, profile);
        return res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const analyzeEligibility = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const { schemeId, profile } = req.body;

        if (!schemeId) {
            return res.status(400).json({ success: false, message: 'schemeId is required in request body' });
        }

        const result = await eligibilityService.analyzeEligibility(userId, schemeId, profile);
        return res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const getChangeAnalysis = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const schemeId = req.params.schemeId || req.query.schemeId;

        if (!schemeId) {
            return res.status(400).json({ success: false, message: 'schemeId parameter is required' });
        }

        const result = await eligibilityService.analyzeEligibility(userId, schemeId);
        return res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

const recomputeEligibility = async (req, res, next) => {
    try {
        const userId = req.user.id;
        const schemeId = req.params.schemeId || req.body.schemeId;
        const { profile, previousResult } = req.body;

        if (!schemeId) {
            return res.status(400).json({ success: false, message: 'schemeId is required' });
        }

        const result = await eligibilityService.recomputeEligibility(userId, schemeId, profile, previousResult);
        return res.status(200).json({
            success: true,
            data: result
        });
    } catch (error) {
        next(error);
    }
};

module.exports = {
    checkEligibility,
    analyzeEligibility,
    getChangeAnalysis,
    recomputeEligibility
};
