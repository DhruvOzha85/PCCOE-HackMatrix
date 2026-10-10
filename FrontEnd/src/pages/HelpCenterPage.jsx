import React, { useState, useEffect, useMemo } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import {
  Compass,
  Search,
  BookOpen,
  ArrowRight,
  ArrowLeft,
  ChevronDown,
  ChevronUp,
  ShieldCheck,
  FileText,
  Inbox,
  User,
  ClipboardCheck,
  HelpCircle,
  Banknote,
  CheckCircle2,
  ExternalLink,
  Sparkles,
  X,
  Home
} from 'lucide-react';
import PageContainer from '../components/layout/PageContainer';
import { HELP_CATEGORIES, formatHelpText } from '../data/helpContent';
import { getStoredRole, getStoredUser } from '../services/authService';

const ICON_MAP = {
  Compass,
  Search,
  ShieldCheck,
  FileText,
  CheckCircle2,
  Inbox,
  Banknote,
  User,
  ClipboardCheck,
  HelpCircle
};

export default function HelpCenterPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const user = getStoredUser();
  const userRole = getStoredRole();
  const isReviewer = userRole === 'reviewer';

  const initialCategory = searchParams.get('topic') || 'getting-started';
  const initialArticle = searchParams.get('article') || '';

  const [activeCategoryId, setActiveCategoryId] = useState(initialCategory);
  const [activeArticleId, setActiveArticleId] = useState(initialArticle);
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedFaqId, setExpandedFaqId] = useState(null);

  // Sync category from URL param
  useEffect(() => {
    const topic = searchParams.get('topic');
    if (topic && HELP_CATEGORIES.some((c) => c.id === topic)) {
      setActiveCategoryId(topic);
    }
    const art = searchParams.get('article');
    if (art) {
      setActiveArticleId(art);
    }
  }, [searchParams]);

  // Filter categories based on user role (reviewer section only visible to reviewers)
  const visibleCategories = useMemo(() => {
    return HELP_CATEGORIES.filter((cat) => {
      if (cat.audience === 'reviewer') {
        return isReviewer;
      }
      return true;
    });
  }, [isReviewer]);

  // Active Category Object
  const currentCategory = useMemo(() => {
    return (
      visibleCategories.find((c) => c.id === activeCategoryId) ||
      visibleCategories[0] ||
      HELP_CATEGORIES[0]
    );
  }, [visibleCategories, activeCategoryId]);

  // Real-time search across all visible categories
  const searchResults = useMemo(() => {
    if (!searchQuery.trim()) return null;
    const q = searchQuery.toLowerCase().trim();
    const results = [];

    visibleCategories.forEach((cat) => {
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
  }, [searchQuery, visibleCategories]);

  const handleSelectCategory = (categoryId) => {
    setActiveCategoryId(categoryId);
    setActiveArticleId('');
    setSearchParams({ topic: categoryId }, { replace: true });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleStartTour = () => {
    window.dispatchEvent(new CustomEvent('start_guided_tour'));
  };

  return (
    <PageContainer>
      <div className="help-center-page">
        {/* Top Hero Banner */}
        <section className="help-hero-banner" aria-label="Help Center Header">
          <div className="help-hero-content">
            <div className="help-hero-badge">
              <BookOpen size={14} color="#005B50" />
              <span>Official Documentation &amp; User Guides</span>
            </div>
            <h1 className="help-hero-title">Help &amp; Support</h1>
            <p className="help-hero-subtitle">
              Learn how to discover government schemes, understand evidence-based eligibility (FIN-03), manage your document vault, and track applications.
            </p>

            {/* Quick Actions Header Bar */}
            <div className="help-hero-actions-bar">
              <button
                type="button"
                className="help-hero-btn-tour"
                onClick={() => navigate('/onboarding')}
                style={{ backgroundColor: '#005B50', color: '#FFFFFF', borderColor: '#005B50' }}
              >
                <Sparkles size={16} />
                <span>Onboarding Walkthrough</span>
              </button>

              <button
                type="button"
                className="help-hero-btn-tour"
                onClick={handleStartTour}
              >
                <Compass size={16} />
                <span>Interactive Guided Tour</span>
              </button>

              <button
                type="button"
                className="help-hero-btn-dash"
                onClick={() => navigate(isReviewer ? '/reviewer/dashboard' : '/dashboard')}
              >
                <Home size={15} />
                <span>Back to Dashboard</span>
              </button>
            </div>

            {/* Live Search Input */}
            <div className="help-hero-search-box">
              <Search size={18} color="#667085" className="help-hero-search-icon" aria-hidden="true" />
              <input
                type="search"
                className="help-hero-search-input"
                placeholder="Search across all guides, FIN-03 rules, vault FAQs, and statutory terms..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                aria-label="Search help center"
              />
              {searchQuery && (
                <button
                  type="button"
                  className="help-hero-search-clear"
                  onClick={() => setSearchQuery('')}
                  aria-label="Clear search"
                >
                  <X size={15} />
                </button>
              )}
            </div>
          </div>
        </section>

        {/* Search Results Display */}
        {searchResults ? (
          <section className="help-search-results-wrapper" aria-label="Search Results">
            <div className="help-search-results-header">
              <h2 className="help-results-heading">
                Search Results for &quot;{searchQuery}&quot;
              </h2>
              <span className="help-results-count">
                {searchResults.length} {searchResults.length === 1 ? 'article' : 'articles'} found
              </span>
            </div>

            {searchResults.length === 0 ? (
              <div className="help-empty-search-card">
                <HelpCircle size={36} color="#667085" />
                <h3 className="help-empty-title">No matching guides found</h3>
                <p className="help-empty-desc">
                  We couldn&apos;t find any articles matching &quot;{searchQuery}&quot;. Try searching for &quot;income&quot;, &quot;readiness&quot;, &quot;What-If&quot;, &quot;OCR&quot;, or &quot;status&quot;.
                </p>
                <button
                  type="button"
                  className="help-btn-clear-search"
                  onClick={() => setSearchQuery('')}
                >
                  Clear search and view all topics
                </button>
              </div>
            ) : (
              <div className="help-search-results-grid">
                {searchResults.map((item) => (
                  <article key={item.id} className="help-search-card">
                    <div className="help-card-category-tag">{item.categoryTitle}</div>
                    <h3 className="help-card-title">{item.title}</h3>
                    <p className="help-card-summary">{item.summary}</p>
                    <div className="help-card-content-preview">
                      <p>{formatHelpText(item.content.slice(0, 220))}...</p>
                    </div>
                    <button
                      type="button"
                      className="help-card-jump-btn"
                      onClick={() => {
                        setSearchQuery('');
                        handleSelectCategory(item.categoryId);
                        setActiveArticleId(item.id);
                      }}
                    >
                      <span>Read Full Guide</span>
                      <ArrowRight size={13} />
                    </button>
                  </article>
                ))}
              </div>
            )}
          </section>
        ) : (
          /* Normal Topic Categorization View */
          <div className="help-main-layout">
            {/* Left Column: Category Navigation Tabs */}
            <aside className="help-categories-sidebar" aria-label="Help Categories">
              <h3 className="help-sidebar-title">Help Categories</h3>
              <nav className="help-category-nav">
                {visibleCategories.map((cat) => {
                  const CatIcon = ICON_MAP[cat.icon] || HelpCircle;
                  const isActive = cat.id === activeCategoryId;
                  return (
                    <button
                      key={cat.id}
                      type="button"
                      className={`help-category-tab ${isActive ? 'active' : ''}`}
                      onClick={() => handleSelectCategory(cat.id)}
                    >
                      <CatIcon size={17} className="help-category-tab-icon" />
                      <div className="help-category-tab-text">
                        <span className="help-tab-title">{cat.title}</span>
                        <span className="help-tab-count">
                          {cat.articles.length} {cat.id === 'faqs' ? 'Questions' : 'Articles'}
                        </span>
                      </div>
                    </button>
                  );
                })}
              </nav>

              {/* Quick Jump Links to App Pages */}
              <div className="help-quick-links-box">
                <h4 className="help-quick-links-title">Quick Links</h4>
                <ul className="help-quick-links-list">
                  <li>
                    <Link to="/discover" className="help-app-link">
                      <Search size={13} />
                      <span>Discover Schemes</span>
                    </Link>
                  </li>
                  <li>
                    <Link to="/documents" className="help-app-link">
                      <FileText size={13} />
                      <span>Document Vault</span>
                    </Link>
                  </li>
                  <li>
                    <Link to="/applications" className="help-app-link">
                      <Inbox size={13} />
                      <span>Track Applications</span>
                    </Link>
                  </li>
                  <li>
                    <Link to="/profile" className="help-app-link">
                      <User size={13} />
                      <span>Citizen Profile</span>
                    </Link>
                  </li>
                  {isReviewer && (
                    <li>
                      <Link to="/reviewer/applications" className="help-app-link">
                        <ClipboardCheck size={13} />
                        <span>Reviewer Desk</span>
                      </Link>
                    </li>
                  )}
                </ul>
              </div>
            </aside>

            {/* Right Column: Category Content & Articles */}
            <main className="help-content-canvas" aria-label="Category Documentation">
              <div className="help-category-header-card">
                <div className="help-category-header-top">
                  <h2 className="help-category-main-title">{currentCategory.title}</h2>
                  {currentCategory.audience === 'reviewer' && (
                    <span className="help-reviewer-only-badge">Government Reviewer Portal</span>
                  )}
                </div>
                <p className="help-category-main-desc">{currentCategory.description}</p>
              </div>

              {/* Special FAQs Accordion Layout if faqs is selected */}
              {currentCategory.id === 'faqs' ? (
                <div className="help-faqs-accordion-container">
                  {currentCategory.articles.map((faq) => {
                    const isExpanded = expandedFaqId === faq.id;
                    return (
                      <div key={faq.id} className="help-faq-accordion-card">
                        <button
                          type="button"
                          className={`help-faq-accordion-header ${isExpanded ? 'open' : ''}`}
                          onClick={() => setExpandedFaqId(isExpanded ? null : faq.id)}
                          aria-expanded={isExpanded}
                        >
                          <div className="help-faq-header-left">
                            <HelpCircle size={17} color="#005B50" className="help-faq-icon" />
                            <h3 className="help-faq-title">{faq.title}</h3>
                          </div>
                          {isExpanded ? <ChevronUp size={16} /> : <ChevronDown size={16} />}
                        </button>

                        {isExpanded && (
                          <div className="help-faq-accordion-body">
                            <div className="help-faq-content-formatted">
                              {faq.content.split('\n\n').map((para, pIdx) => (
                                <p key={pIdx}>{formatHelpText(para)}</p>
                              ))}
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              ) : (
                /* Regular Articles Layout */
                <div className="help-articles-stack">
                  {currentCategory.articles.map((article) => {
                    const isHighlighted = activeArticleId === article.id;
                    return (
                      <article
                        key={article.id}
                        id={article.id}
                        className={`help-article-card ${isHighlighted ? 'highlighted' : ''}`}
                      >
                        <div className="help-article-header">
                          <h3 className="help-article-title">{article.title}</h3>
                          {article.summary && (
                            <span className="help-article-summary">{formatHelpText(article.summary)}</span>
                          )}
                        </div>

                        <div className="help-article-body">
                          {article.content.split('\n\n').map((block, bIdx) => {
                            if (block.startsWith('- ')) {
                              const bullets = block.split('\n- ').map((b) => b.replace(/^- /, ''));
                              return (
                                <ul key={bIdx} className="help-article-bullets">
                                  {bullets.map((bullet, buIdx) => (
                                    <li key={buIdx}>{formatHelpText(bullet)}</li>
                                  ))}
                                </ul>
                              );
                            }
                            if (block.startsWith('1. ') || block.startsWith('2. ') || block.startsWith('3. ')) {
                              const numbered = block.split('\n').filter(Boolean);
                              return (
                                <ol key={bIdx} className="help-article-ordered">
                                  {numbered.map((numItem, nIdx) => (
                                    <li key={nIdx}>{formatHelpText(numItem.replace(/^\d+\.\s*/, ''))}</li>
                                  ))}
                                </ol>
                              );
                            }
                            return <p key={bIdx}>{formatHelpText(block)}</p>;
                          })}
                        </div>
                      </article>
                    );
                  })}
                </div>
              )}
            </main>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
