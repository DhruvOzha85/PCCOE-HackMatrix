/**
 * DOM Translation Engine powered by Google Translation API
 * Traverses DOM text nodes, preserves original English text, and dynamically
 * replaces content across all views, modals, cards, and navigation.
 */

// Memory cache of translations: { [langCode]: { [originalText]: translatedText } }
const translationMemoryCache = {};

// Load cached translations from storage
function loadStoredCache(langCode) {
  if (translationMemoryCache[langCode]) {
    return translationMemoryCache[langCode];
  }
  try {
    const raw = sessionStorage.getItem(`fin_trans_cache_${langCode}`);
    if (raw) {
      translationMemoryCache[langCode] = JSON.parse(raw);
      return translationMemoryCache[langCode];
    }
  } catch (e) {
    // Ignore storage parse issues
  }
  translationMemoryCache[langCode] = {};
  return translationMemoryCache[langCode];
}

// Save cached translations to storage
function saveStoredCache(langCode) {
  try {
    if (translationMemoryCache[langCode]) {
      sessionStorage.setItem(
        `fin_trans_cache_${langCode}`,
        JSON.stringify(translationMemoryCache[langCode])
      );
    }
  } catch (e) {
    // Ignore quota issues
  }
}

// Active observer reference
let domObserver = null;
let currentLanguage = 'en';
let isTranslating = false;
let debounceTimer = null;

/**
 * Checks if a string is eligible for translation
 */
function shouldTranslateText(text) {
  if (!text || typeof text !== 'string') return false;
  const trimmed = text.trim();
  if (trimmed.length < 2) return false;

  // Never translate the portal brand name FIN or FIN-03
  if (trimmed.toUpperCase() === 'FIN' || trimmed === 'FIN-03') {
    return false;
  }

  // Skip purely numeric or single symbol strings (e.g. "₹", "0", "123", "/", "-")
  if (/^[\d\s.,/\\+\-₹$%:;!@#&*()—_]+$/.test(trimmed)) {
    return false;
  }

  return true;
}

/**
 * Checks if an element should be ignored from translation
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
    el.classList.contains('footer-brand-title')
  )) {
    return true;
  }
  if (el.getAttribute && el.getAttribute('translate') === 'no') {
    return true;
  }
  if (el.closest && (
    el.closest('.notranslate') ||
    el.closest('[translate="no"]') ||
    el.closest('.fin-brand-hero-title') ||
    el.closest('.signup-logo-text') ||
    el.closest('.footer-brand-title') ||
    el.closest('.fin-brand-top-row')
  )) {
    return true;
  }
  return false;
}

/**
 * Fetch batch translations from the backend Google Translate endpoint
 */
async function fetchBatchTranslations(texts, targetLang) {
  if (!texts || texts.length === 0 || targetLang === 'en') return {};

  try {
    const res = await fetch('/api/translate/batch', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ texts, targetLang, sourceLang: 'en' }),
    });

    if (res.ok) {
      const data = await res.json();
      return data.translations || {};
    }
  } catch (err) {
    console.warn('Backend translation proxy failed, using direct client fallback:', err);
  }

  // Fallback: direct Google Translate API call if backend proxy is unavailable
  const fallbackResults = {};
  for (const str of texts) {
    try {
      const url = `https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=${encodeURIComponent(targetLang)}&dt=t&q=${encodeURIComponent(str)}`;
      const directRes = await fetch(url);
      if (directRes.ok) {
        const parsed = await directRes.json();
        if (Array.isArray(parsed) && Array.isArray(parsed[0])) {
          fallbackResults[str] = parsed[0].map((item) => item[0]).join('');
        }
      }
    } catch (_) {
      // Fallback silently skips
    }
  }
  return fallbackResults;
}

/**
 * Restores original English text across the entire DOM
 */
export function restoreOriginalDom() {
  currentLanguage = 'en';
  if (domObserver) {
    domObserver.disconnect();
  }

  const root = document.getElementById('root') || document.body;
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
 * Traverses the DOM, collects English text nodes, and applies Google translations
 */
export async function translateDom(targetLang) {
  if (!targetLang || targetLang === 'en') {
    restoreOriginalDom();
    return;
  }

  currentLanguage = targetLang;
  const langCache = loadStoredCache(targetLang);

  if (isTranslating) return;
  isTranslating = true;

  // Pause observer while making DOM edits
  if (domObserver) {
    domObserver.disconnect();
  }

  try {
    const root = document.getElementById('root') || document.body;
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
    const nodeMapping = []; // Array of { node, origStr, cleanStr }

    let node;
    while ((node = walker.nextNode())) {
      // Save original text on first encounter
      if (node.__finOriginalText === undefined) {
        node.__finOriginalText = node.nodeValue;
      }

      // If already translated to this language, skip
      if (node.__finCurrentLang === targetLang) {
        continue;
      }

      const orig = node.__finOriginalText;
      const clean = orig.trim();

      if (!shouldTranslateText(clean)) {
        continue;
      }

      nodeMapping.push({ node, orig, clean });

      // If not in cache, queue for translation
      if (!langCache[clean]) {
        pendingStrings.add(clean);
      }
    }

    // Also collect placeholders on input/textarea
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
        if (!langCache[origPh]) {
          pendingStrings.add(origPh);
        }
      }
    });

    // Fetch translations for any newly seen strings
    if (pendingStrings.size > 0) {
      const textsToFetch = Array.from(pendingStrings);
      const fetched = await fetchBatchTranslations(textsToFetch, targetLang);
      Object.assign(langCache, fetched);
      saveStoredCache(targetLang);
    }

    // Apply translations to DOM text nodes
    for (const { node, orig, clean } of nodeMapping) {
      const translated = langCache[clean];
      if (translated) {
        node.nodeValue = orig.replace(clean, translated);
        node.__finCurrentLang = targetLang;
      }
    }

    // Apply translations to placeholders
    for (const { input, origPh } of phMapping) {
      const translatedPh = langCache[origPh];
      if (translatedPh) {
        input.placeholder = translatedPh;
      }
    }
  } catch (err) {
    console.error('Error during DOM translation:', err);
  } finally {
    isTranslating = false;
    // Re-attach observer for dynamic SPA re-renders
    setupDomObserver();
  }
}

/**
 * Mutation observer to translate new elements rendered by React
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
    }, 140);
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
    // Wait for initial render to settle
    setTimeout(() => {
      translateDom(saved);
    }, 250);
  }
}
