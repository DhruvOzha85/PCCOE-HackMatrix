const https = require('https');
const querystring = require('querystring');

// In-memory persistent cache: { [targetLang]: { [cleanText]: translatedText } }
const translationCache = {};

// Clean brand name so FIN is never corrupted
function sanitizeBrand(text) {
  if (!text || typeof text !== 'string') return text;
  return text
    .replace(/पंख/g, 'FIN')
    .replace(/फिन/g, 'FIN')
    .replace(/पंखा/g, 'FIN')
    .replace(/પાંખ/g, 'FIN')
    .replace(/செட்டை/g, 'FIN')
    .replace(/రెక్క/g, 'FIN')
    .replace(/ডানা/g, 'FIN');
}

/**
 * Translates a single text string using Google Translate POST
 */
function fetchGoogleTranslation(text, targetLang = 'en', sourceLang = 'en') {
  return new Promise((resolve) => {
    if (!text || typeof text !== 'string' || !text.trim()) {
      return resolve(text);
    }

    const trimmed = text.trim();
    if (trimmed.toUpperCase() === 'FIN' || trimmed === 'FIN-03') {
      return resolve('FIN');
    }

    if (translationCache[targetLang] && translationCache[targetLang][trimmed]) {
      return resolve(translationCache[targetLang][trimmed]);
    }

    const postData = querystring.stringify({
      client: 'gtx',
      sl: sourceLang,
      tl: targetLang,
      dt: 't',
      q: trimmed,
    });

    const options = {
      hostname: 'translate.googleapis.com',
      path: '/translate_a/single',
      method: 'POST',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        'Content-Length': Buffer.byteLength(postData),
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      },
      timeout: 3000,
    };

    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        try {
          if (res.statusCode === 200) {
            const parsed = JSON.parse(data);
            if (Array.isArray(parsed) && Array.isArray(parsed[0])) {
              const translated = sanitizeBrand(parsed[0].map((item) => item[0]).join(''));
              if (translated) {
                if (!translationCache[targetLang]) translationCache[targetLang] = {};
                translationCache[targetLang][trimmed] = translated;
                return resolve(translated);
              }
            }
          }
          resolve(text);
        } catch (_) {
          resolve(text);
        }
      });
    });

    req.on('timeout', () => { req.destroy(); resolve(text); });
    req.on('error', () => { resolve(text); });
    req.write(postData);
    req.end();
  });
}

/**
 * Translates a batch of texts using delimiter joining in parallel chunks
 * Handles 100+ strings in ~500ms
 */
async function translateBatch(texts = [], targetLang = 'en', sourceLang = 'en') {
  if (!Array.isArray(texts) || texts.length === 0 || targetLang === 'en') {
    const direct = {};
    for (const t of texts) direct[t] = t;
    return direct;
  }

  const results = {};
  const toFetch = [];

  // 1. Check in-memory cache
  for (const raw of texts) {
    if (!raw || typeof raw !== 'string') continue;
    const clean = raw.trim();
    if (!clean) continue;

    if (clean.toUpperCase() === 'FIN' || clean === 'FIN-03') {
      results[clean] = 'FIN';
      continue;
    }

    if (translationCache[targetLang] && translationCache[targetLang][clean]) {
      results[clean] = translationCache[targetLang][clean];
    } else {
      toFetch.push(clean);
    }
  }

  const unique = Array.from(new Set(toFetch));
  if (unique.length === 0) return results;

  const DELIMITER = ' ||| ';
  const CHUNK_SIZE = 25; // 25 strings per HTTP request for fast execution
  const chunks = [];

  for (let i = 0; i < unique.length; i += CHUNK_SIZE) {
    chunks.push(unique.slice(i, i + CHUNK_SIZE));
  }

  // Execute all chunk requests in parallel
  await Promise.all(
    chunks.map(async (chunk) => {
      try {
        const joined = chunk.join(DELIMITER);
        const postData = querystring.stringify({
          client: 'gtx',
          sl: sourceLang,
          tl: targetLang,
          dt: 't',
          q: joined,
        });

        const translatedFull = await new Promise((resolve) => {
          const options = {
            hostname: 'translate.googleapis.com',
            path: '/translate_a/single',
            method: 'POST',
            headers: {
              'Content-Type': 'application/x-www-form-urlencoded',
              'Content-Length': Buffer.byteLength(postData),
              'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            },
            timeout: 4000,
          };

          const req = https.request(options, (res) => {
            let data = '';
            res.on('data', (c) => { data += c; });
            res.on('end', () => {
              try {
                if (res.statusCode === 200) {
                  const parsed = JSON.parse(data);
                  if (Array.isArray(parsed) && Array.isArray(parsed[0])) {
                    const joinedTrans = parsed[0].map((item) => item[0]).join('');
                    return resolve(joinedTrans);
                  }
                }
                resolve(null);
              } catch (_) {
                resolve(null);
              }
            });
          });

          req.on('timeout', () => { req.destroy(); resolve(null); });
          req.on('error', () => { resolve(null); });
          req.write(postData);
          req.end();
        });

        if (translatedFull) {
          const split = translatedFull.split(/\s*\|\|\|\s*/);
          if (split.length === chunk.length) {
            chunk.forEach((orig, idx) => {
              const trans = sanitizeBrand(split[idx] ? split[idx].trim() : orig);
              results[orig] = trans;
              if (!translationCache[targetLang]) translationCache[targetLang] = {};
              translationCache[targetLang][orig] = trans;
            });
            return;
          }
        }

        // Fallback for this chunk if delimiter splitting was uneven
        const fallbackPromises = chunk.map(async (str) => {
          const trans = await fetchGoogleTranslation(str, targetLang, sourceLang);
          results[str] = trans;
        });
        await Promise.all(fallbackPromises);
      } catch (err) {
        chunk.forEach((str) => { results[str] = str; });
      }
    })
  );

  return results;
}

module.exports = {
  fetchGoogleTranslation,
  translateBatch,
};
