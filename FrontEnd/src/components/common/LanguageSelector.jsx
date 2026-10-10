import React, { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { Globe, ChevronDown, Check, Loader2 } from 'lucide-react';
import { translateDom, restoreOriginalDom, initDomTranslation } from '../../utils/domTranslator';

export const SUPPORTED_LANGUAGES = [
  { code: 'en', native: 'English', english: 'English', badge: 'EN' },
  { code: 'hi', native: 'हिन्दी', english: 'Hindi', badge: 'HI' },
  { code: 'mr', native: 'मराठी', english: 'Marathi', badge: 'MR' },
  { code: 'gu', native: 'ગુજરાતી', english: 'Gujarati', badge: 'GU' },
  { code: 'ta', native: 'தமிழ்', english: 'Tamil', badge: 'TA' },
  { code: 'te', native: 'తెలుగు', english: 'Telugu', badge: 'TE' },
  { code: 'bn', native: 'বাংলা', english: 'Bengali', badge: 'BN' },
  { code: 'kn', native: 'ಕನ್ನಡ', english: 'Kannada', badge: 'KN' },
  { code: 'ml', native: 'മലയാളം', english: 'Malayalam', badge: 'ML' },
  { code: 'pa', native: 'ਪੰਜਾਬੀ', english: 'Punjabi', badge: 'PA' },
  { code: 'ur', native: 'اردو', english: 'Urdu', badge: 'UR' },
  { code: 'or', native: 'ଓଡ଼ିଆ', english: 'Odia', badge: 'OR' },
  { code: 'as', native: 'অসমীয়া', english: 'Assamese', badge: 'AS' },
];

export default function LanguageSelector() {
  const [isOpen, setIsOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [isTranslating, setIsTranslating] = useState(false);
  const [currentLang, setCurrentLang] = useState(() => {
    return localStorage.getItem('fin_user_language') || 'en';
  });

  const wrapperRef = useRef(null);
  const location = useLocation();

  // Helper to sync Google Translate cookie & combo (if loaded)
  const syncGoogleCombo = (langCode) => {
    try {
      const cookieVal = `/en/${langCode}`;
      const host = window.location.hostname;

      if (langCode === 'en') {
        document.cookie = 'googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        if (host && !host.includes('localhost') && !host.includes('127.0.0.1')) {
          document.cookie = `googtrans=; expires=Thu, 01 Jan 1970 00:00:00 UTC; domain=.${host}; path=/;`;
        }
      } else {
        document.cookie = `googtrans=${cookieVal}; path=/;`;
        if (host && !host.includes('localhost') && !host.includes('127.0.0.1')) {
          document.cookie = `googtrans=${cookieVal}; domain=.${host}; path=/;`;
        }
      }

      const combo = document.querySelector('.goog-te-combo');
      if (combo) {
        combo.value = langCode;
        combo.dispatchEvent(new Event('change'));
      }
    } catch (_) {
      // Safe ignore
    }
  };

  // Execute full DOM translation
  const applyLanguage = async (langCode) => {
    setIsTranslating(true);
    syncGoogleCombo(langCode);

    if (langCode === 'en') {
      restoreOriginalDom();
      setIsTranslating(false);
    } else {
      await translateDom(langCode);
      setIsTranslating(false);
    }
  };

  // Change language handler
  const handleSelectLanguage = async (langCode) => {
    if (langCode === currentLang) {
      setIsOpen(false);
      return;
    }

    setCurrentLang(langCode);
    localStorage.setItem('fin_user_language', langCode);
    setIsOpen(false);
    setSearchQuery('');

    await applyLanguage(langCode);
  };

  // Re-apply translation upon SPA route transitions (skip standalone auth pages: signin & register)
  useEffect(() => {
    const isAuthRoute =
      location.pathname === '/login' ||
      location.pathname === '/signin' ||
      location.pathname === '/signup' ||
      location.pathname === '/register';

    if (isAuthRoute) return;

    if (currentLang && currentLang !== 'en') {
      const timeout = setTimeout(() => {
        translateDom(currentLang);
      }, 150);
      return () => clearTimeout(timeout);
    }
  }, [location.pathname, currentLang]);

  // Initial sync on mount
  useEffect(() => {
    const saved = localStorage.getItem('fin_user_language');
    if (saved && saved !== 'en') {
      initDomTranslation();
    }
  }, []);

  // Close dropdown on outside click or Esc
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') setIsOpen(false);
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleKeyDown);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  const activeLangObj = SUPPORTED_LANGUAGES.find((l) => l.code === currentLang) || SUPPORTED_LANGUAGES[0];

  const filteredLanguages = SUPPORTED_LANGUAGES.filter((lang) => {
    const q = searchQuery.toLowerCase().trim();
    if (!q) return true;
    return (
      lang.native.toLowerCase().includes(q) ||
      lang.english.toLowerCase().includes(q) ||
      lang.code.toLowerCase().includes(q)
    );
  });

  return (
    <div className="language-selector-wrapper notranslate" ref={wrapperRef} translate="no">
      <button
        type="button"
        className={`language-trigger-btn notranslate ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        aria-label={`Select Portal Language (Current: ${activeLangObj.english})`}
        aria-expanded={isOpen}
        title="Change Language (Google Translate)"
        translate="no"
      >
        {isTranslating ? (
          <Loader2 size={15} className="language-globe-icon spinning" aria-hidden="true" />
        ) : (
          <Globe size={15} className="language-globe-icon" aria-hidden="true" />
        )}
        <span className="language-trigger-text notranslate" translate="no">
          {activeLangObj.native}
        </span>
        <span className="language-trigger-badge notranslate" aria-hidden="true" translate="no">
          {activeLangObj.badge}
        </span>
        <ChevronDown
          size={14}
          className={`language-chevron-icon ${isOpen ? 'open' : ''}`}
          aria-hidden="true"
        />
      </button>

      {isOpen && (
        <div className="language-dropdown-popover notranslate" role="dialog" aria-label="Language selection modal" translate="no">
          <div className="language-popover-header">
            <div className="language-popover-title-row">
              <span className="language-popover-title">Portal Language / भाषा</span>
              <span className="language-google-badge">
                <span>Google Translate</span>
              </span>
            </div>
          </div>

          <div className="language-search-box">
            <input
              type="text"
              className="language-search-input notranslate"
              placeholder="Search / खोजें (e.g. Hindi, मराठी)..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              autoFocus
              translate="no"
            />
          </div>

          <div className="language-list-scroll">
            {filteredLanguages.map((lang) => {
              const isSelected = lang.code === currentLang;
              return (
                <button
                  key={lang.code}
                  type="button"
                  className={`language-option-item notranslate ${isSelected ? 'selected' : ''}`}
                  onClick={() => handleSelectLanguage(lang.code)}
                  translate="no"
                >
                  <div className="language-names-col">
                    <span className="language-native-name notranslate" translate="no">{lang.native}</span>
                    <span className="language-english-name notranslate" translate="no">{lang.english}</span>
                  </div>
                  {isSelected && (
                    <Check size={16} className="language-selected-check" aria-hidden="true" />
                  )}
                </button>
              );
            })}
            {filteredLanguages.length === 0 && (
              <div style={{ padding: '12px', textAlign: 'center', fontSize: '12px', color: 'var(--text-muted)' }}>
                No languages found
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
