const { translateBatch, fetchGoogleTranslation } = require('../services/googleTranslateService');

async function handleTranslateBatch(req, res) {
  try {
    const { texts, targetLang, sourceLang = 'en' } = req.body;

    if (!targetLang) {
      return res.status(400).json({
        success: false,
        message: 'targetLang is required',
      });
    }

    if (!Array.isArray(texts)) {
      return res.status(400).json({
        success: false,
        message: 'texts must be an array of strings',
      });
    }

    const translations = await translateBatch(texts, targetLang, sourceLang);

    return res.status(200).json({
      success: true,
      targetLang,
      translations,
    });
  } catch (err) {
    console.error('Error in handleTranslateBatch:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to process translations',
    });
  }
}

async function handleTranslateSingle(req, res) {
  try {
    const { text, targetLang, sourceLang = 'en' } = req.body;
    if (!text || !targetLang) {
      return res.status(400).json({
        success: false,
        message: 'text and targetLang are required',
      });
    }

    const translated = await fetchGoogleTranslation(text, targetLang, sourceLang);

    return res.status(200).json({
      success: true,
      targetLang,
      originalText: text,
      translatedText: translated,
    });
  } catch (err) {
    console.error('Error in handleTranslateSingle:', err);
    return res.status(500).json({
      success: false,
      message: 'Failed to translate text',
    });
  }
}

module.exports = {
  handleTranslateBatch,
  handleTranslateSingle,
};
