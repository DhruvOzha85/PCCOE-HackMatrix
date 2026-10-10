const express = require('express');
const router = express.Router();
const { handleTranslateBatch, handleTranslateSingle } = require('../controllers/translateController');

router.post('/batch', handleTranslateBatch);
router.post('/single', handleTranslateSingle);

module.exports = router;
