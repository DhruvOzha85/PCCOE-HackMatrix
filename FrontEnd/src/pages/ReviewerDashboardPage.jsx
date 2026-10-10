import React, { useState, useEffect, useMemo, useRef } from 'react';
import { Link } from 'react-router-dom';
import PageContainer from '../components/layout/PageContainer';
import {
  fetchReviewerDashboardStats,
  fetchReviewerApplicationsQueue
} from '../services/reviewerService';
import {
  Clock,
  CalendarDays,
  CheckCircle2,
  CircleX,
  TriangleAlert,
  RotateCw,
  ArrowRight,
  FileText,
  Search,
  Filter,
  MoreVertical,
  ChevronRight,
  ShieldCheck,
  ClipboardCheck,
  AlertCircle,
  ExternalLink
} from 'lucide-react';
import indiaGateHero from '../assets/india_gate_hero.jpg';
import tricolorFlag from '../assets/tricolor_flag_perfect.png';
import '../styles/reviewerDashboard.css';

/**
 * Render compact, dignified status badge matching FIN design system
 */
function renderStatusBadge(status) {
  const s = String(status || 'under_review').toLowerCase();
  if (s === 'under_review' || s === 'pending' || s === 'submitted') {
    return <span className="reviewer-status-badge under-review">Under Review</span>;
  }
  if (s === 'documents_pending' || s === 'documents_required') {
    return <span className="reviewer-status-badge documents-pending">Documents Pending</span>;
  }
  if (s === 'action_required') {
    return <span className="reviewer-status-badge action-required">Action Required</span>;
  }
  if (s === 'approved' || s === 'sanctioned') {
    return <span className="reviewer-status-badge approved">Approved</span>;
  }
  if (s === 'rejected') {
    return <span className="reviewer-status-badge rejected">Rejected</span>;
  }
  return <span className="reviewer-status-badge neutral">{s.replace(/_/g, ' ')}</span>;
}

export default function ReviewerDashboardPage() {
  const [stats, setStats] = useState({
    pending_reviews: 0,
    reviewed_today: 0,
    approved: 0,
    rejected: 0,
    action_required: 0,
    total_applications: 0
  });
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState(null);

  // Queue toolbar interaction states
  const [filterTab, setFilterTab] = useState('all'); // 'all' | 'under_review' | 'documents_pending' | 'action_required' | 'rejected'
  const [searchQuery, setSearchQuery] = useState('');
  const [activeMenuId, setActiveMenuId] = useState(null);
  const menuRef = useRef(null);

  // Close three-dot action popovers on click outside
  useEffect(() => {
    const handleDocumentClick = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setActiveMenuId(null);
      }
    };
    document.addEventListener('click', handleDocumentClick);
    return () => {
      document.removeEventListener('click', handleDocumentClick);
    };
  }, []);

  const loadData = async (isManualRefresh = false) => {
    try {
      if (isManualRefresh) {
        setIsRefreshing(true);
      } else {
        setLoading(true);
      }
      setError(null);

      const [statsData, queueData] = await Promise.all([
        fetchReviewerDashboardStats(),
        fetchReviewerApplicationsQueue('all')
      ]);

      if (statsData) {
        setStats(statsData);
      }
      if (Array.isArray(queueData)) {
        setApplications(queueData);
      }
    } catch (err) {
      console.error('Failed to load reviewer dashboard data:', err);
      setError(err.message || 'Failed to load reviewer dashboard metrics.');
    } finally {
      setLoading(false);
      setIsRefreshing(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  // Filtered applications computed dynamically from live review queue
  const filteredApps = useMemo(() => {
    return applications.filter((app) => {
      // 1. Status Filter Tab
      if (filterTab !== 'all') {
        const s = String(app.status || '').toLowerCase();
        if (filterTab === 'under_review' && !['under_review', 'submitted', 'pending'].includes(s)) {
          return false;
        }
        if (filterTab === 'documents_pending' && !['documents_pending', 'documents_required'].includes(s)) {
          return false;
        }
        if (filterTab === 'action_required' && s !== 'action_required') {
          return false;
        }
        if (filterTab === 'rejected' && s !== 'rejected') {
          return false;
        }
      }

      // 2. Search Query Matching
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const appId = String(app.application_number || app.id || '').toLowerCase();
        const applicant = String(app.applicant_name || app.user_name || '').toLowerCase();
        const scheme = String(app.scheme_name || '').toLowerCase();
        const ministry = String(app.ministry || app.department || '').toLowerCase();

        if (
          !appId.includes(q) &&
          !applicant.includes(q) &&
          !scheme.includes(q) &&
          !ministry.includes(q)
        ) {
          return false;
        }
      }

      return true;
    });
  }, [applications, filterTab, searchQuery]);

  // Dynamic formatted date string matching "Sun, 11 Oct 2026"
  const currentDate = new Date().toLocaleDateString('en-GB', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric'
  });

  return (
    <PageContainer>
      <div className="reviewer-dashboard-container">
        {/* Error notification banner if service connection degrades */}
        {error && (
          <div className="reviewer-error-banner" role="alert">
            <div className="reviewer-error-left">
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
            <button
              type="button"
              onClick={() => loadData(true)}
              className="reviewer-error-retry-btn"
            >
              <RotateCw size={14} />
              <span>Retry</span>
            </button>
          </div>
        )}

        {/* 1. Light, Elegant Hero Banner matching India Gate treatment */}
        <section className="reviewer-hero-card" aria-label="Reviewer Control Desk">
          <div
            className="reviewer-hero-bg-visual"
            style={{ backgroundImage: `url(${indiaGateHero})` }}
            aria-hidden="true"
          />

          <div className="reviewer-hero-left-content">
            <div className="reviewer-hero-tag-row">
              <span className="reviewer-hero-date">{currentDate}</span>
              <span className="reviewer-hero-badge">GOVERNMENT APPLICATION REVIEW PORTAL</span>
            </div>

            <h1 className="reviewer-hero-title">Reviewer Dashboard</h1>

            <p className="reviewer-hero-subtitle">
              Review citizen applications, validate supporting documents, and record evidence-based decisions.
            </p>

            <div className="reviewer-hero-actions-row">
              <Link to="/reviewer/applications" className="reviewer-btn-primary">
                <span>Open Review Queue</span>
                <ArrowRight size={15} />
              </Link>
              <button
                type="button"
                onClick={() => loadData(true)}
                className="reviewer-btn-secondary"
                disabled={loading || isRefreshing}
                title="Refresh Queue Metrics"
              >
                <RotateCw size={14} className={isRefreshing ? 'animate-spin' : ''} />
                <span>{isRefreshing ? 'Refreshing...' : 'Refresh Metrics'}</span>
              </button>
            </div>
          </div>

          <div className="reviewer-hero-right-editorial">
            <blockquote className="reviewer-hero-quote-text">
              “Empowered governance for a stronger India.”
            </blockquote>
            <cite className="reviewer-hero-quote-author">— Government of India</cite>
            <img
              src={tricolorFlag}
              alt="National Flag of India"
              className="reviewer-hero-flag-img"
              aria-hidden="true"
            />
          </div>
        </section>

        {/* 2. Five Dynamic Reviewer Metric Cards */}
        <div className="reviewer-metrics-grid" role="region" aria-label="Key Reviewer Metrics">
          {/* Card 1: Pending Reviews */}
          <Link
            to="/reviewer/applications?status=under_review"
            className="reviewer-metric-card"
            title="View pending reviews"
          >
            <div className="reviewer-metric-content-wrap">
              <div className="reviewer-metric-icon-box amber" aria-hidden="true">
                <Clock size={22} />
              </div>
              <div className="reviewer-metric-text-col">
                <span className="reviewer-metric-title-txt">Pending Reviews</span>
                <div className="reviewer-metric-val-row">
                  <span className="reviewer-metric-number-val">{stats.pending_reviews}</span>
                  <ChevronRight size={18} className="reviewer-metric-chevron" aria-hidden="true" />
                </div>
                <span className="reviewer-metric-sub-txt">Awaiting officer action</span>
              </div>
            </div>
          </Link>

          {/* Card 2: Reviewed Today */}
          <div
            className="reviewer-metric-card"
            title="Reviewed in current shift"
          >
            <div className="reviewer-metric-content-wrap">
              <div className="reviewer-metric-icon-box blue" aria-hidden="true">
                <CalendarDays size={22} />
              </div>
              <div className="reviewer-metric-text-col">
                <span className="reviewer-metric-title-txt">Reviewed Today</span>
                <div className="reviewer-metric-val-row">
                  <span className="reviewer-metric-number-val">{stats.reviewed_today}</span>
                  <ChevronRight size={18} className="reviewer-metric-chevron" aria-hidden="true" />
                </div>
                <span className="reviewer-metric-sub-txt">Processed in current shift</span>
              </div>
            </div>
          </div>

          {/* Card 3: Approved */}
          <Link
            to="/reviewer/applications?status=approved"
            className="reviewer-metric-card"
            title="View approved applications"
          >
            <div className="reviewer-metric-content-wrap">
              <div className="reviewer-metric-icon-box green" aria-hidden="true">
                <CheckCircle2 size={22} />
              </div>
              <div className="reviewer-metric-text-col">
                <span className="reviewer-metric-title-txt">Approved</span>
                <div className="reviewer-metric-val-row">
                  <span className="reviewer-metric-number-val">{stats.approved}</span>
                  <ChevronRight size={18} className="reviewer-metric-chevron" aria-hidden="true" />
                </div>
                <span className="reviewer-metric-sub-txt">Sanctioned & verified</span>
              </div>
            </div>
          </Link>

          {/* Card 4: Action Required */}
          <Link
            to="/reviewer/applications?status=action_required"
            className="reviewer-metric-card"
            title="View applications requiring action"
          >
            <div className="reviewer-metric-content-wrap">
              <div className="reviewer-metric-icon-box purple" aria-hidden="true">
                <TriangleAlert size={22} />
              </div>
              <div className="reviewer-metric-text-col">
                <span className="reviewer-metric-title-txt">Action Required</span>
                <div className="reviewer-metric-val-row">
                  <span className="reviewer-metric-number-val">{stats.action_required}</span>
                  <ChevronRight size={18} className="reviewer-metric-chevron" aria-hidden="true" />
                </div>
                <span className="reviewer-metric-sub-txt">Changes requested to citizen</span>
              </div>
            </div>
          </Link>

          {/* Card 5: Rejected */}
          <Link
            to="/reviewer/applications?status=rejected"
            className="reviewer-metric-card"
            title="View rejected applications"
          >
            <div className="reviewer-metric-content-wrap">
              <div className="reviewer-metric-icon-box red" aria-hidden="true">
                <CircleX size={22} />
              </div>
              <div className="reviewer-metric-text-col">
                <span className="reviewer-metric-title-txt">Rejected</span>
                <div className="reviewer-metric-val-row">
                  <span className="reviewer-metric-number-val">{stats.rejected}</span>
                  <ChevronRight size={18} className="reviewer-metric-chevron" aria-hidden="true" />
                </div>
                <span className="reviewer-metric-sub-txt">Ineligible / incomplete</span>
              </div>
            </div>
          </Link>
        </div>

        {/* 3. Active Review Queue Section */}
        <section className="reviewer-queue-card" aria-label="Active Review Queue">
          {/* Queue Header Row */}
          <div className="reviewer-queue-header-row">
            <div className="reviewer-queue-title-wrap">
              <FileText size={22} className="reviewer-queue-header-icon" aria-hidden="true" />
              <div>
                <h2 className="reviewer-queue-title">Active Review Queue</h2>
                <p className="reviewer-queue-sub">
                  Applications submitted by citizens requiring administrative review
                </p>
              </div>
            </div>

            <Link to="/reviewer/applications" className="reviewer-queue-view-all-btn">
              <span>View Full Queue ({stats.total_applications || applications.length})</span>
              <ArrowRight size={14} />
            </Link>
          </div>

          {/* Toolbar with Filter Tabs and Search Input */}
          <div className="reviewer-toolbar-row">
            <div className="reviewer-filter-tabs" role="tablist">
              <button
                type="button"
                role="tab"
                aria-selected={filterTab === 'all'}
                onClick={() => setFilterTab('all')}
                className={`reviewer-tab-pill ${filterTab === 'all' ? 'active' : ''}`}
              >
                <span>All</span>
                <span className="reviewer-tab-count-badge">{applications.length}</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={filterTab === 'under_review'}
                onClick={() => setFilterTab('under_review')}
                className={`reviewer-tab-pill ${filterTab === 'under_review' ? 'active' : ''}`}
              >
                <span>Under Review</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={filterTab === 'documents_pending'}
                onClick={() => setFilterTab('documents_pending')}
                className={`reviewer-tab-pill ${filterTab === 'documents_pending' ? 'active' : ''}`}
              >
                <span>Documents Pending</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={filterTab === 'action_required'}
                onClick={() => setFilterTab('action_required')}
                className={`reviewer-tab-pill ${filterTab === 'action_required' ? 'active' : ''}`}
              >
                <span className="reviewer-tab-dot amber" aria-hidden="true" />
                <span>Action Required</span>
              </button>

              <button
                type="button"
                role="tab"
                aria-selected={filterTab === 'rejected'}
                onClick={() => setFilterTab('rejected')}
                className={`reviewer-tab-pill ${filterTab === 'rejected' ? 'active' : ''}`}
              >
                <span className="reviewer-tab-dot red" aria-hidden="true" />
                <span>Rejected</span>
              </button>
            </div>

            <div className="reviewer-search-filter-wrap">
              <div className="reviewer-search-input-box">
                <Search size={15} className="reviewer-search-icon" aria-hidden="true" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Search applications..."
                  className="reviewer-search-input"
                  aria-label="Search applications"
                />
                {searchQuery && (
                  <button
                    type="button"
                    onClick={() => setSearchQuery('')}
                    className="reviewer-search-clear-btn"
                    aria-label="Clear search"
                  >
                    ×
                  </button>
                )}
              </div>

              <button
                type="button"
                onClick={() => {
                  if (filterTab !== 'all' || searchQuery) {
                    setFilterTab('all');
                    setSearchQuery('');
                  }
                }}
                className={`reviewer-filter-btn ${filterTab !== 'all' || searchQuery ? 'active' : ''}`}
                title="Reset or toggle filters"
              >
                <Filter size={14} />
                <span>Filter</span>
              </button>
            </div>
          </div>

          {/* Queue Applications Table */}
          {loading ? (
            <div className="reviewer-table-loading-state">
              <RotateCw size={24} className="animate-spin" />
              <span>Loading review queue applications...</span>
            </div>
          ) : filteredApps.length === 0 ? (
            <div className="reviewer-table-empty-state">
              <ClipboardCheck size={36} className="reviewer-empty-icon" />
              <p className="reviewer-empty-title">No applications in queue</p>
              <span className="reviewer-empty-sub">
                {searchQuery || filterTab !== 'all'
                  ? 'No applications match your active search or filter criteria.'
                  : 'All applications have been reviewed or no pending submissions exist.'}
              </span>
              {(searchQuery || filterTab !== 'all') && (
                <button
                  type="button"
                  onClick={() => {
                    setFilterTab('all');
                    setSearchQuery('');
                  }}
                  className="reviewer-empty-reset-btn"
                >
                  Reset Filters
                </button>
              )}
            </div>
          ) : (
            <div className="reviewer-table-wrap" ref={menuRef}>
              <table className="reviewer-table">
                <thead>
                  <tr>
                    <th>Application ID</th>
                    <th>Applicant</th>
                    <th>Scheme</th>
                    <th>Submission Date</th>
                    <th>Current Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredApps.map((app) => {
                    const appId = app.application_number || (app.id ? `FIN-${String(app.id).slice(0, 8).toUpperCase()}` : 'FIN-APP');
                    const dateStr = app.submitted_at || app.created_at
                      ? new Date(app.submitted_at || app.created_at).toLocaleDateString('en-GB')
                      : 'Recent';
                    const applicantName = app.applicant_name || app.user_name || 'Govt Reviewer Officer';
                    const applicantSubtitle = app.applicant_role || (app.applicant_id ? 'Citizen Applicant' : '');
                    const schemeName = app.scheme_name || (app.scheme && app.scheme.name) || 'Scheme Catalog Record';
                    const schemeSubtitle = app.ministry || app.department || app.scheme_category || 'Government of India';

                    return (
                      <tr key={app.id}>
                        <td className="col-app-id">
                          <span className="reviewer-app-id-text">{appId}</span>
                        </td>
                        <td className="col-applicant">
                          <div className="reviewer-applicant-cell">
                            <span className="reviewer-applicant-name">{applicantName}</span>
                            {applicantSubtitle && (
                              <span className="reviewer-applicant-sub">{applicantSubtitle}</span>
                            )}
                          </div>
                        </td>
                        <td className="col-scheme">
                          <div className="reviewer-scheme-cell">
                            <span className="reviewer-scheme-title">{schemeName}</span>
                            <span className="reviewer-scheme-ministry">{schemeSubtitle}</span>
                          </div>
                        </td>
                        <td className="col-date">
                          <span className="reviewer-date-text">{dateStr}</span>
                        </td>
                        <td className="col-status">
                          {renderStatusBadge(app.status)}
                        </td>
                        <td className="col-actions">
                          <div className="reviewer-actions-cell">
                            <Link
                              to={`/reviewer/applications?open=${app.id}`}
                              className="reviewer-inspect-btn"
                              title="Inspect & Review Application"
                            >
                              <span>Inspect & Review</span>
                              <ArrowRight size={13} />
                            </Link>

                            <div className="reviewer-row-menu-container">
                              <button
                                type="button"
                                className={`reviewer-more-menu-btn ${activeMenuId === app.id ? 'active' : ''}`}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  setActiveMenuId(activeMenuId === app.id ? null : app.id);
                                }}
                                aria-label="More application actions"
                                title="More actions"
                              >
                                <MoreVertical size={16} />
                              </button>

                              {activeMenuId === app.id && (
                                <div className="reviewer-row-popover" role="menu">
                                  <Link
                                    to={`/reviewer/applications?open=${app.id}`}
                                    className="reviewer-popover-item"
                                    onClick={() => setActiveMenuId(null)}
                                  >
                                    <ExternalLink size={14} />
                                    <span>Inspect Full Dossier</span>
                                  </Link>
                                  {app.scheme_id && (
                                    <Link
                                      to={`/schemes/${app.scheme_id}`}
                                      className="reviewer-popover-item"
                                      onClick={() => setActiveMenuId(null)}
                                    >
                                      <FileText size={14} />
                                      <span>View Scheme Guidelines</span>
                                    </Link>
                                  )}
                                  <button
                                    type="button"
                                    className="reviewer-popover-item"
                                    onClick={() => {
                                      if (navigator.clipboard) {
                                        navigator.clipboard.writeText(appId);
                                      }
                                      setActiveMenuId(null);
                                    }}
                                  >
                                    <ClipboardCheck size={14} />
                                    <span>Copy Application ID</span>
                                  </button>
                                </div>
                              )}
                            </div>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* 4. Statutory Evidence Banner (Bottom Governance Invariant) */}
        <aside className="reviewer-evidence-banner" aria-label="Governance Principles">
          <div className="reviewer-evidence-icon-box">
            <ShieldCheck size={22} />
          </div>
          <div className="reviewer-evidence-body">
            <h4 className="reviewer-evidence-title">
              Evidence-First Governance Principle
            </h4>
            <p className="reviewer-evidence-desc">
              FIN AI provides assisted fact-extraction and policy eligibility analysis.
              In accordance with administrative regulations, AI will never mutate application
              status or finalize decisions automatically. Human review officers remain the final
              statutory decision authority.
            </p>
          </div>
        </aside>
      </div>
    </PageContainer>
  );
}
