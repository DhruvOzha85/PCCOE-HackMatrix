import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import {
  X,
  Search,
  HelpCircle,
  Compass,
  ArrowRight,
  ExternalLink,
  ChevronDown,
  ChevronUp,
  Sparkles,
  BookOpen,
  CheckCircle2,
  Lightbulb
} from 'lucide-react';
import {
  HELP_CATEGORIES,
  getContextualHelpForRoute,
  formatHelpText
} from '../../data/helpContent';

export default function HelpPanel({ isOpen, onClose, onStartTour }) {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedFaqId, setExpandedFaqId] = useState(null);
  const panelRef = useRef(null);
  const searchInputRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

  // Get route contextual guidance
  const contextualHelp = useMemo(() => {
    return getContextualHelpForRoute(location.pathname);
  }, [location.pathname]);

  // Focus search input when panel opens
  useEffect(() => {
    if (isOpen) {
      const timer = setTimeout(() => {
        searchInputRef.current?.focus();
      }, 100);
      return () => clearTimeout(timer);
    } else {
      setSearchQuery('');
      setExpandedFaqId(null);
    }
  }, [isOpen]);

  // Dismiss on Escape key
  useEffect(() => {
    if (!isOpen) return;
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  // Search filtering across all articles & FAQs
  const searchResults = useMemo(() => {
    if (!searchQuery.trim()) return null;
    const q = searchQuery.toLowerCase().trim();
    const results = [];

    HELP_CATEGORIES.forEach((cat) => {
      cat.articles.forEach((art) => {
        const titleMatch = art.title.toLowerCase().includes(q);
        const summaryMatch = (art.summary || '').toLowerCase().includes(q);
        const contentMatch = art.content.toLowerCase().includes(q);
        if (titleMatch || summaryMatch || contentMatch) {
          results.push({
            ...art,
            categoryTitle: cat.title,
            categoryId: cat.id
          });
        }
      });
    });

    return results;
  }, [searchQuery]);

  // Extract FAQs for the current route
  const relevantFaqs = useMemo(() => {
    const faqCategory = HELP_CATEGORIES.find((c) => c.id === 'faqs');
    if (!faqCategory) return [];
    return faqCategory.articles.slice(0, 4);
  }, []);

  const handleStartTourClick = () => {
    onClose();
    if (typeof onStartTour === 'function') {
      onStartTour();
    } else {
      window.dispatchEvent(new CustomEvent('start_guided_tour'));
    }
  };

  const handleNavigateHelpCenter = () => {
    onClose();
    navigate('/help');
  };

  if (!isOpen) return null;

  return (
    <div className="help-panel-overlay" role="dialog" aria-modal="true" aria-label="Help and Guidance Panel">
      <div className="help-panel-backdrop" onClick={onClose} />

      <aside className="help-panel-drawer" ref={panelRef}>
        {/* Drawer Header */}
        <div className="help-panel-header">
          <div className="help-header-title-box">
            <div className="help-header-icon" aria-hidden="true">
              <HelpCircle size={18} color="#005B50" />
            </div>
            <div>
              <h2 className="help-drawer-title">Help &amp; Guidance</h2>
              <span className="help-drawer-subtitle">
                {contextualHelp.badge || 'Contextual Assistance'}
              </span>
            </div>
          </div>

          <button
            type="button"
            className="help-drawer-close-btn"
            onClick={onClose}
            aria-label="Close help panel"
          >
            <X size={17} />
          </button>
        </div>

        {/* Live Search Bar */}
        <div className="help-search-container">
          <div className="help-search-box">
            <Search size={15} color="#667085" className="help-search-icon" aria-hidden="true" />
            <input
              ref={searchInputRef}
              type="search"
              className="help-search-input"
              placeholder="Search help topics, FAQs, eligibility..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              aria-label="Search help documentation"
            />
            {searchQuery && (
              <button
                type="button"
                className="help-search-clear"
                onClick={() => setSearchQuery('')}
                aria-label="Clear search"
              >
                <X size={13} />
              </button>
            )}
          </div>
        </div>

        {/* Drawer Scrollable Content */}
        <div className="help-drawer-scroll-body">
          {/* Quick Action: Start Guided Tour */}
          <div className="help-quick-action-card">
            <div className="help-quick-action-text">
              <div className="help-quick-action-title">
                <Compass size={16} color="#005B50" />
                <span>Interactive Guided Tour</span>
              </div>
              <p className="help-quick-action-desc">
                Take a 7-step walkthrough of FIN features and workflows.
              </p>
            </div>
            <button
              type="button"
              className="help-btn-start-tour"
              onClick={handleStartTourClick}
            >
              <span>Start Tour</span>
              <ArrowRight size={13} />
            </button>
          </div>

          {/* Search Results Mode */}
          {searchResults ? (
            <div className="help-search-results-section">
              <h3 className="help-section-heading">
                Search Results ({searchResults.length})
              </h3>
              {searchResults.length === 0 ? (
                <div className="help-no-results">
                  <p className="help-no-results-text">No articles found matching &quot;{searchQuery}&quot;.</p>
                  <span className="help-no-results-sub">Try searching for keywords like &quot;income&quot;, &quot;OCR&quot;, &quot;readiness&quot;, or &quot;status&quot;.</span>
                </div>
              ) : (
                <div className="help-results-list">
                  {searchResults.map((item) => (
                    <div key={item.id} className="help-result-item">
                      <span className="help-result-category">{item.categoryTitle}</span>
                      <h4 className="help-result-title">{item.title}</h4>
                      <p className="help-result-summary">{item.summary}</p>
                      <button
                        type="button"
                        className="help-result-read-btn"
                        onClick={() => {
                          onClose();
                          navigate(`/help?topic=${item.categoryId}&article=${item.id}`);
                        }}
                      >
                        <span>Read in Help Center</span>
                        <ArrowRight size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          ) : (
            <>
              {/* Contextual Guidance Section */}
              <div className="help-contextual-section">
                <div className="help-context-badge-bar">
                  <Lightbulb size={14} color="#005B50" />
                  <span className="help-context-badge-text">Guidance for this page</span>
                </div>
                <h3 className="help-context-page-title">{contextualHelp.title}</h3>
                <p className="help-context-page-summary">{formatHelpText(contextualHelp.summary)}</p>

                {/* Practical Tips */}
                <div className="help-context-tips-box">
                  <h4 className="help-tips-heading">Pro Tips:</h4>
                  <ul className="help-tips-list">
                    {contextualHelp.tips?.map((tip, idx) => (
                      <li key={idx} className="help-tip-item">
                        <CheckCircle2 size={13} className="help-tip-bullet" />
                        <span>{formatHelpText(tip)}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>

              {/* Frequently Asked Questions */}
              <div className="help-faqs-section">
                <h3 className="help-section-heading">Common Questions</h3>
                <div className="help-faq-accordion-list">
                  {relevantFaqs.map((faq) => {
                    const isExpanded = expandedFaqId === faq.id;
                    return (
                      <div key={faq.id} className="help-faq-item">
                        <button
                          type="button"
                          className={`help-faq-trigger ${isExpanded ? 'active' : ''}`}
                          onClick={() => setExpandedFaqId(isExpanded ? null : faq.id)}
                          aria-expanded={isExpanded}
                        >
                          <span className="help-faq-trigger-text">{faq.title}</span>
                          {isExpanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                        </button>
                        {isExpanded && (
                          <div className="help-faq-answer">
                            <p>{formatHelpText(faq.content)}</p>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>
            </>
          )}
        </div>

        {/* Drawer Footer Links */}
        <div className="help-panel-footer" style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <button
            type="button"
            className="help-footer-all-topics-btn"
            onClick={() => {
              onClose();
              navigate('/onboarding');
            }}
            style={{ backgroundColor: '#EBF3F1', color: '#005B50', borderColor: '#BCDCD6' }}
          >
            <Sparkles size={15} color="#005B50" />
            <span>Open Onboarding Walkthrough</span>
            <ArrowRight size={13} />
          </button>

          <button
            type="button"
            className="help-footer-all-topics-btn"
            onClick={handleNavigateHelpCenter}
          >
            <BookOpen size={15} />
            <span>Browse Full Help Center (/help)</span>
            <ExternalLink size={14} />
          </button>
        </div>
      </aside>
    </div>
  );
}
