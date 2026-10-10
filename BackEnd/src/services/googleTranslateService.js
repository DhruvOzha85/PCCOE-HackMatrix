const https = require('https');

// In-memory cache: { [targetLang]: { [cleanText]: translatedText } }
const translationCache = {};

/**
 * Translates a single text string using Google Translate API endpoint
 */
function fetchGoogleTranslation(text, targetLang = 'en', sourceLang = 'en') {
  return new Promise((resolve) => {
    if (!text || typeof text !== 'string' || !text.trim()) {
      return resolve(text);
    }

    const trimmed = text.trim();
    // Strictly preserve portal brand FIN and FIN-03 across all languages
    if (trimmed.toUpperCase() === 'FIN' || trimmed === 'FIN-03') {
      return resolve(trimmed);
    }

    if (translationCache[targetLang] && translationCache[targetLang][trimmed]) {
      return resolve(translationCache[targetLang][trimmed]);
    }

    const encoded = encodeURIComponent(trimmed);
    const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=${encodeURIComponent(sourceLang)}&tl=${encodeURIComponent(targetLang)}&dt=t&q=${encoded}`;

    https.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          if (Array.isArray(parsed) && Array.isArray(parsed[0])) {
            const translated = parsed[0].map((item) => item[0]).join('');
            if (translated) {
              if (!translationCache[targetLang]) {
                translationCache[targetLang] = {};
              }
              translationCache[targetLang][trimmed] = translated;
              return resolve(translated);
            }
          }
          resolve(text);
        } catch (e) {
          resolve(text);
        }
      });
    }).on('error', () => {
      resolve(text);
    });
  });
}

/**
 * Translates an array of texts with concurrency throttling
 */
async function translateBatch(texts = [], targetLang = 'en', sourceLang = 'en') {
  if (!Array.isArray(texts) || texts.length === 0 || targetLang === 'en') {
    const directMap = {};
    for (const t of texts) directMap[t] = t;
    return directMap;
  }

  const results = {};
  const toFetch = [];

  // 1. Check in-memory cache first
  for (const rawText of texts) {
    if (!rawText || typeof rawText !== 'string') continue;
    const clean = rawText.trim();
    if (!clean) continue;

    if (translationCache[targetLang] && translationCache[targetLang][clean]) {
      results[clean] = translationCache[targetLang][clean];
    } else {
      toFetch.push(clean);
    }
  }

  // 2. Fetch uncached items in parallel chunks of 15
  const uniqueToFetch = Array.from(new Set(toFetch));
  const chunkSize = 15;

  for (let i = 0; i < uniqueToFetch.length; i += chunkSize) {
    const chunk = uniqueToFetch.slice(i, i + chunkSize);
    const promises = chunk.map(async (str) => {
      const translated = await fetchGoogleTranslation(str, targetLang, sourceLang);
      results[str] = translated;
    });
    await Promise.all(promises);
  }

  return results;
}

module.exports = {
  fetchGoogleTranslation,
  translateBatch,
};
