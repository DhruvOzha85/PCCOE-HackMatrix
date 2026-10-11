/**
 * Dynamic DOM Translation Engine powered by Google Translation API
 * 
 * Dynamically traverses the entire DOM, collects all live text nodes & placeholders,
 * batches them to Google Translate via fast HTTP POST, and replaces all text
 * dynamically across the application within 2-4 seconds.
 * 
 * Zero static dictionaries — 100% dynamic Google Translation.
 */

// Memory cache: { [langCode]: { [origText]: translatedText } }
const memoryCache = {};

// Active translation state
let currentLanguage = 'en';
let domObserver = null;
let debounceTimer = null;
let isTranslating = false;

// Variants of "FIN" in Indian languages that Google sometimes generates
const FORBIDDEN_BRAND_VARIANTS = [
  'पंख',
  'फिन',
  'पंखा',
  'પાંખ',
  'செட்டை',
  'రెక్క',
  'ডানা',
  'ರೆಕ್ಕೆ',
  'ചിറക്',
  'ਖੰਭ',
  'پر',
  'ଡେଣା',
  'পাখিৰ ডেউকা',
];

/**
 * Loads cached translations from localStorage
 */
function loadLanguageCache(langCode) {
  if (memoryCache[langCode]) {
    return memoryCache[langCode];
  }

  const cache = {};
  try {
    const raw = localStorage.getItem(`fin_google_trans_${langCode}`);
    if (raw) {
      Object.assign(cache, JSON.parse(raw));
    }
  } catch (_) {
    // Ignore storage parse issues
  }

  memoryCache[langCode] = cache;
  return cache;
}

/**
 * Saves cache to localStorage
 */
function saveLanguageCache(langCode) {
  try {
    if (memoryCache[langCode]) {
      localStorage.setItem(
        `fin_google_trans_${langCode}`,
        JSON.stringify(memoryCache[langCode])
      );
    }
  } catch (_) {
    // Ignore storage quota issues
  }
}

/**
 * Strictly protects the portal brand "FIN"
 */
function sanitizeBrandName(text) {
  if (!text || typeof text !== 'string') return text;
  let result = text;
  for (const variant of FORBIDDEN_BRAND_VARIANTS) {
    if (result.includes(variant)) {
      result = result.replace(new RegExp(variant, 'g'), 'FIN');
    }
  }
  return result;
}

/**
 * Checks if a string should be translated
 */
function shouldTranslateText(text) {
  if (!text || typeof text !== 'string') return false;
  const trimmed = text.trim();
  if (trimmed.length < 2) return false;

  // Never translate the portal brand FIN or FIN IDs
  if (trimmed.toUpperCase() === 'FIN' || trimmed === 'FIN-03' || /^FIN-\d+$/i.test(trimmed)) {
    return false;
  }

  // Skip purely numeric or punctuation strings (e.g. "₹ 1,500", "0", "12/05/2024", "/", "-")
  if (/^[\d\s.,/\\+\-₹$%:;!@#&*()—_|[\]{}<>]+$/.test(trimmed)) {
    return false;
  }

  return true;
}

/**
 * Checks if a DOM element should be excluded
 */
function isIgnoredElement(el) {
  if (!el || !el.tagName) return false;
  const tag = el.tagName.toUpperCase();
  if (['SCRIPT', 'STYLE', 'NOSCRIPT', 'SVG', 'PATH', 'CODE', 'PRE'].includes(tag)) {
    return true;
  }
  if (el.classList && (
    el.classList.contains('notranslate') ||
    el.classList.contains('fin-brand-hero-title') ||
    el.classList.contains('signup-logo-text') ||
    el.classList.contains('footer-brand-title') ||
    el.classList.contains('language-selector-wrapper')
  )) {
    return true;
  }
  if (el.getAttribute && el.getAttribute('translate') === 'no') {
    return true;
  }
  if (el.closest && (
    el.closest('.notranslate') ||
    el.closest('[translate="no"]') ||
    el.closest('.language-selector-wrapper')
  )) {
    return true;
  }
  return false;
}

/**
 * Fetches translations dynamically from backend Google Translate endpoint
 */
async function fetchDynamicBatchTranslations(texts, targetLang) {
  if (!texts || texts.length === 0 || targetLang === 'en') return {};

  const needed = Array.from(new Set(texts));
  const results = {};

  try {
    const res = await fetch('/api/translate/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ texts: needed, targetLang, sourceLang: 'en' }),
    });

    if (res.ok) {
      const data = await res.json();
      if (data && data.translations) {
        for (const [k, v] of Object.entries(data.translations)) {
          results[k] = sanitizeBrandName(v);
        }
        return results;
      }
    }
  } catch (err) {
    console.warn('Backend translation call notice, falling back:', err);
  }

  // Fallback: direct Google Translate batch with delimiter
  const DELIMITER = ' ||| ';
  const CHUNK_SIZE = 25;
  for (let i = 0; i < needed.length; i += CHUNK_SIZE) {
    const chunk = needed.slice(i, i + CHUNK_SIZE);
    try {
      const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=${encodeURIComponent(targetLang)}&dt=t&q=${encodeURIComponent(chunk.join(DELIMITER))}`;
      const res = await fetch(url);
      if (res.ok) {
        const parsed = await res.json();
        if (Array.isArray(parsed) && Array.isArray(parsed[0])) {
          const joinedTrans = parsed[0].map((item) => item[0]).join('');
          const split = joinedTrans.split(/\s*\|\|\|\s*/);
          if (split.length === chunk.length) {
            chunk.forEach((orig, idx) => {
              results[orig] = sanitizeBrandName(split[idx] ? split[idx].trim() : orig);
            });
            continue;
          }
        }
      }
    } catch (_) {}

    chunk.forEach((s) => { results[s] = s; });
  }

  return results;
}

/**
 * Restores original English text across the entire DOM instantly
 */
export function restoreOriginalDom() {
  currentLanguage = 'en';

  if (domObserver) {
    domObserver.disconnect();
  }

  const root = document.getElementById('root') || document.body;
  if (!root) return;

  const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);

  let node;
  while ((node = walker.nextNode())) {
    if (node.__finOriginalText !== undefined) {
      node.nodeValue = node.__finOriginalText;
      node.__finCurrentLang = 'en';
    }
  }

  // Restore placeholders
  document.querySelectorAll('[data-fin-original-ph]').forEach((input) => {
    input.placeholder = input.getAttribute('data-fin-original-ph');
  });

  // Restore titles
  document.querySelectorAll('[data-fin-original-title]').forEach((el) => {
    el.title = el.getAttribute('data-fin-original-title');
  });
}

/**
 * Traverses the entire DOM tree, fetches every text node & placeholder,
 * and updates them dynamically using Google Translation API.
 */
export async function translateDom(targetLang) {
  if (!targetLang || targetLang === 'en') {
    restoreOriginalDom();
    return;
  }

  currentLanguage = targetLang;
  const langCache = loadLanguageCache(targetLang);

  if (isTranslating) return;
  isTranslating = true;

  // Disconnect observer during translation pass
  if (domObserver) {
    domObserver.disconnect();
  }

  try {
    const root = document.getElementById('root') || document.body;
    if (!root) return;

    // Traverse ALL text nodes across the entire page
    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, {
      acceptNode(node) {
        if (!node.parentElement || isIgnoredElement(node.parentElement)) {
          return NodeFilter.FILTER_REJECT;
        }
        if (!node.nodeValue || !node.nodeValue.trim()) {
          return NodeFilter.FILTER_SKIP;
        }
        return NodeFilter.FILTER_ACCEPT;
      },
    });

    const pendingStrings = new Set();
    const nodeMapping = []; // Array of { node, orig, clean }

    let node;
    while ((node = walker.nextNode())) {
      // Preserve original English text on first encounter
      if (node.__finOriginalText === undefined) {
        node.__finOriginalText = node.nodeValue;
      }

      // If already translated to targetLang, skip
      if (node.__finCurrentLang === targetLang) {
        continue;
      }

      const orig = node.__finOriginalText;
      const clean = orig.trim();

      if (!shouldTranslateText(clean)) {
        continue;
      }

      nodeMapping.push({ node, orig, clean });

      if (langCache[clean]) {
        // Instant cache hit
        node.nodeValue = orig.replace(clean, langCache[clean]);
        node.__finCurrentLang = targetLang;
      } else {
        pendingStrings.add(clean);
      }
    }

    // Traverse all inputs & textareas for placeholders
    const phInputs = document.querySelectorAll('input[placeholder], textarea[placeholder]');
    const phMapping = [];
    phInputs.forEach((input) => {
      if (isIgnoredElement(input)) return;
      if (!input.hasAttribute('data-fin-original-ph')) {
        input.setAttribute('data-fin-original-ph', input.placeholder);
      }
      const origPh = input.getAttribute('data-fin-original-ph').trim();
      if (shouldTranslateText(origPh)) {
        phMapping.push({ input, origPh });
        if (langCache[origPh]) {
          input.placeholder = langCache[origPh];
        } else {
          pendingStrings.add(origPh);
        }
      }
    });

    // If there are uncached strings, fetch them via Google Translate batch
    if (pendingStrings.size > 0) {
      const textsToFetch = Array.from(pendingStrings);
      const fetched = await fetchDynamicBatchTranslations(textsToFetch, targetLang);

      // Merge into cache and save to localStorage
      Object.assign(langCache, fetched);
      saveLanguageCache(targetLang);

      // Apply to text nodes
      for (const { node, orig, clean } of nodeMapping) {
        if (node.__finCurrentLang !== targetLang && langCache[clean]) {
          node.nodeValue = orig.replace(clean, langCache[clean]);
          node.__finCurrentLang = targetLang;
        }
      }

      // Apply to placeholders
      for (const { input, origPh } of phMapping) {
        if (langCache[origPh]) {
          input.placeholder = langCache[origPh];
        }
      }
    }
  } catch (err) {
    console.error('Error during dynamic DOM translation:', err);
  } finally {
    isTranslating = false;
    setupDomObserver();
  }
}

/**
 * Mutation observer to dynamically translate new elements rendered by React
 */
function setupDomObserver() {
  if (currentLanguage === 'en') return;
  if (domObserver) domObserver.disconnect();

  const root = document.getElementById('root') || document.body;
  if (!root) return;

  domObserver = new MutationObserver(() => {
    if (isTranslating || currentLanguage === 'en') return;
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      translateDom(currentLanguage);
    }, 180);
  });

  domObserver.observe(root, {
    childList: true,
    subtree: true,
    characterData: false,
  });
}

/**
 * Initialize translation on application startup
 */
export function initDomTranslation() {
  const saved = localStorage.getItem('fin_user_language') || 'en';
  if (saved && saved !== 'en') {
    setTimeout(() => {
      translateDom(saved);
    }, 200);
  }
}
