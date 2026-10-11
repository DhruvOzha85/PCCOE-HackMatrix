import React, { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import PageContainer from '../components/layout/PageContainer';
import {
  fetchReviewerApplicationsQueue,
  fetchApplicationReviewDetails,
  submitReviewDecision,
  verifyDocumentStatus
} from '../services/reviewerService';
import { resolveEvidenceConflict } from '../services/schemeService';
import {
  ShieldCheck,
  Search,
  Filter,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  RotateCw,
  FileText,
  User,
  ExternalLink,
  X,
  Clock,
  Check,
  ArrowRight,
  Info,
  Calendar,
  Building,
  Award,
  Download
} from 'lucide-react';
import '../styles/reviewApplications.css';

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

export default function ReviewApplicationsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const initialStatus = searchParams.get('status') || 'all';
  const openAppId = searchParams.get('open');

  const [statusFilter, setStatusFilter] = useState(initialStatus);
  const [searchQuery, setSearchQuery] = useState('');
  const [applications, setApplications] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Detail Modal State
  const [selectedAppId, setSelectedAppId] = useState(openAppId || null);
  const [appDetails, setAppDetails] = useState(null);
  const [loadingDetails, setLoadingDetails] = useState(false);

  // Decision Modal State
  const [decisionModal, setDecisionModal] = useState({
    isOpen: false,
    decisionType: null, // 'approve' | 'request_changes' | 'reject'
    remark: '',
    error: ''
  });
  const [submittingDecision, setSubmittingDecision] = useState(false);
  const [successToast, setSuccessToast] = useState(null);
  const [verifyingDocId, setVerifyingDocId] = useState(null);
  const [docActionError, setDocActionError] = useState(null);

  // Addition 2: Conflict resolution state
  const [resolvingConflictId, setResolvingConflictId] = useState(null);
  const [conflictReason, setConflictReason] = useState('');
  const [selectedAuthoritativeSource, setSelectedAuthoritativeSource] = useState('DOCUMENT');
  const [isSubmittingConflictResolution, setIsSubmittingConflictResolution] = useState(false);

  // Fetch queue
  const loadQueue = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await fetchReviewerApplicationsQueue(statusFilter);
      setApplications(data);
    } catch (err) {
      console.error('Error fetching reviewer queue:', err);
      setError(err.message || 'Failed to load applications');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadQueue();
  }, [statusFilter]);

  // Load details if selected
  useEffect(() => {
    if (!selectedAppId) {
      setAppDetails(null);
      return;
    }
    const loadDetails = async () => {
      try {
        setLoadingDetails(true);
        const data = await fetchApplicationReviewDetails(selectedAppId);
        setAppDetails(data);
      } catch (err) {
        console.error('Error loading application details:', err);
        setError(`Failed to load details: ${err.message}`);
      } finally {
        setLoadingDetails(false);
      }
    };
    loadDetails();
  }, [selectedAppId]);

  // Filtered applications
  const filteredApps = applications.filter((app) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase();
    const id = String(app.id || '').toLowerCase();
    const name = String(app.applicant_name || '').toLowerCase();
    const scheme = String(app.scheme_name || '').toLowerCase();
    return id.includes(q) || name.includes(q) || scheme.includes(q);
  });

  const handleOpenDecision = (decisionType) => {
    setDecisionModal({
      isOpen: true,
      decisionType,
      remark: decisionType === 'approve' ? 'Approved after statutory document verification' : '',
      error: ''
    });
  };

  const handleConfirmDecision = async () => {
    const { decisionType, remark } = decisionModal;
    if ((decisionType === 'reject' || decisionType === 'request_changes') && !remark.trim()) {
      setDecisionModal(prev => ({
        ...prev,
        error: `A mandatory remark is required to ${decisionType === 'reject' ? 'reject' : 'request changes'}.`
      }));
      return;
    }

    try {
      setSubmittingDecision(true);
      await submitReviewDecision(selectedAppId, {
        decision: decisionType,
        remark: remark.trim()
      });

      setSuccessToast(`Application successfully marked as ${decisionType === 'approve' ? 'Approved' : decisionType === 'request_changes' ? 'Action Required' : 'Rejected'}.`);
      setTimeout(() => setSuccessToast(null), 4000);

      setDecisionModal({ isOpen: false, decisionType: null, remark: '', error: '' });
      setSelectedAppId(null);
      setSearchParams({});
      loadQueue();
    } catch (err) {
      setDecisionModal(prev => ({
        ...prev,
        error: err.message || 'Failed to submit decision'
      }));
    } finally {
      setSubmittingDecision(false);
    }
  };

  return (
    <PageContainer>
      <div className="review-apps-container">
        {/* Header */}
        <div className="review-apps-header">
          <div>
            <h1 className="review-apps-title">Application Review Queue</h1>
            <p className="review-apps-subtitle">
              Verify submitted documents, evaluate scheme conditions, and record auditable government decisions.
            </p>
          </div>

          <button
            type="button"
            onClick={loadQueue}
            className="reviewer-btn-secondary"
            style={{ color: '#0F172A', borderColor: '#CBD5E1', background: '#FFFFFF' }}
            disabled={loading}
          >
            <RotateCw size={14} className={loading ? 'animate-spin' : ''} />
            <span>Refresh Queue</span>
          </button>
        </div>

        {/* Success Toast */}
        {successToast && (
          <div
            style={{
              padding: '12px 18px',
              backgroundColor: '#DCFCE7',
              border: '1px solid #86EFAC',
              color: '#15803D',
              borderRadius: '8px',
              marginBottom: '16px',
              fontWeight: 600,
              display: 'flex',
              alignItems: 'center',
              gap: '10px'
            }}
          >
            <CheckCircle2 size={18} />
            <span>{successToast}</span>
          </div>
        )}

        {/* Filter Controls Row */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '16px',
            marginBottom: '20px',
            flexWrap: 'wrap'
          }}
        >
          {/* Status Tabs */}
          <div className="review-filter-tabs">
            {[
              { id: 'all', label: 'All Applications' },
              { id: 'under_review', label: 'Pending Review' },
              { id: 'action_required', label: 'Action Required' },
              { id: 'approved', label: 'Approved' },
              { id: 'rejected', label: 'Rejected' }
            ].map(tab => (
              <button
                key={tab.id}
                type="button"
                className={`review-tab-btn ${statusFilter === tab.id ? 'active' : ''}`}
                onClick={() => {
                  setStatusFilter(tab.id);
                  setSearchParams(tab.id === 'all' ? {} : { status: tab.id });
                }}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Search Box */}
          <div style={{ position: 'relative', width: '280px' }}>
            <Search
              size={15}
              style={{
                position: 'absolute',
                left: '12px',
                top: '50%',
                transform: 'translateY(-50%)',
                color: '#94A3B8'
              }}
            />
            <input
              type="text"
              placeholder="Search by ID, applicant, scheme..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              style={{
                width: '100%',
                padding: '8px 12px 8px 34px',
                border: '1px solid #CBD5E1',
                borderRadius: '8px',
                fontSize: '13px',
                boxSizing: 'border-box'
              }}
            />
          </div>
        </div>

        {/* Queue Table */}
        <div className="reviewer-section" style={{ padding: 0, overflow: 'hidden' }}>
          {loading ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#64748B' }}>
              Loading review queue...
            </div>
          ) : filteredApps.length === 0 ? (
            <div style={{ textAlign: 'center', padding: '40px', color: '#64748B' }}>
              No applications matching the selected criteria.
            </div>
          ) : (
            <div className="reviewer-table-wrap">
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
                            <button
                              type="button"
                              onClick={() => {
                                setSelectedAppId(app.id);
                                setSearchParams({ open: app.id });
                              }}
                              className="reviewer-inspect-btn"
                              title="Inspect & Review Application"
                            >
                              <span>Inspect & Review</span>
                              <ArrowRight size={13} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Application Inspection Drawer/Modal */}
        {selectedAppId && (
          <div
            className="review-modal-backdrop"
            onClick={() => {
              setSelectedAppId(null);
              setSearchParams({});
            }}
          >
            <div
              className="review-modal-card"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Modal Header */}
              <div className="review-modal-header">
                <div>
                  <h3 className="review-modal-title">
                    Application Review: FIN-{String(selectedAppId).slice(0, 8).toUpperCase()}
                  </h3>
                  <p className="review-modal-sub">
                    {appDetails?.application?.scheme_name || 'Statutory Scheme Review'}
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => {
                    setSelectedAppId(null);
                    setSearchParams({});
                  }}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748B' }}
                >
                  <X size={20} />
                </button>
              </div>

              {/* Modal Body */}
              <div className="review-modal-body">
                {loadingDetails ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: '#64748B' }}>
                    Loading full application dossier & documents...
                  </div>
                ) : !appDetails ? (
                  <div style={{ textAlign: 'center', padding: '40px', color: '#DC2626' }}>
                    Failed to load application details.
                  </div>
                ) : (
                  <>
                    {/* Top 2 Columns: Scheme & Applicant Profile */}
                    <div className="review-grid-2col">
                      {/* Left Panel: Scheme Info & Benefits */}
                      <div className="review-card-panel">
                        <h4 className="review-panel-title">
                          <Building size={16} />
                          <span>Scheme Information</span>
                        </h4>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Scheme Title</span>
                          <span className="review-fact-val">{appDetails.application.scheme_name || 'N/A'}</span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Benefit Category</span>
                          <span className="review-fact-val">{appDetails.application.benefit_subtitle || 'Statutory Assistance'}</span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Financial Value</span>
                          <span className="review-fact-val" style={{ color: '#16A34A' }}>
                            {appDetails.application.benefit_display || 'Not specified'}
                          </span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Current Status</span>
                          <span className="review-fact-val">
                            <span className={`reviewer-badge ${appDetails.application.status}`}>
                              {String(appDetails.application.status).replace(/_/g, ' ')}
                            </span>
                          </span>
                        </div>
                        {appDetails.application.decision_notes && (
                          <div style={{ marginTop: '12px', padding: '10px', background: '#F1F5F9', borderRadius: '6px', fontSize: '12px' }}>
                            <strong>Existing Reviewer Note:</strong> {appDetails.application.decision_notes}
                          </div>
                        )}
                      </div>

                      {/* Right Panel: Applicant Profile Facts */}
                      <div className="review-card-panel">
                        <h4 className="review-panel-title">
                          <User size={16} />
                          <span>Applicant Profile Dossier</span>
                        </h4>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Full Name</span>
                          <span className="review-fact-val">{appDetails.applicant.fullName || 'Applicant'}</span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Email</span>
                          <span className="review-fact-val">{appDetails.applicant.email || 'N/A'}</span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Phone</span>
                          <span className="review-fact-val">{appDetails.applicant.phone || 'N/A'}</span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Occupation</span>
                          <span className="review-fact-val">{appDetails.applicant.profile?.occupation || 'Not specified'}</span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">State / District</span>
                          <span className="review-fact-val">
                            {[appDetails.applicant.profile?.district, appDetails.applicant.profile?.state].filter(Boolean).join(', ') || 'Not specified'}
                          </span>
                        </div>
                        <div className="review-fact-row">
                          <span className="review-fact-label">Annual Income</span>
                          <span className="review-fact-val">
                            {appDetails.applicant.profile?.annual_income ? `₹${Number(appDetails.applicant.profile.annual_income).toLocaleString('en-IN')}` : 'Not specified'}
                          </span>
                        </div>
                      </div>
                    </div>

                    {/* Associated Documents Section */}
                    <div style={{ marginBottom: '24px' }}>
                      <h4 className="review-panel-title">
                        <FileText size={16} />
                        <span>Associated Verification Documents ({appDetails.documents.length})</span>
                      </h4>

                      {docActionError && (
                        <div style={{ padding: '8px 12px', background: '#FEE2E2', color: '#B91C1C', borderRadius: '6px', marginBottom: '12px', fontSize: '13px' }}>
                          <AlertTriangle size={14} style={{ display: 'inline', marginRight: '6px', verticalAlign: 'text-bottom' }} />
                          {docActionError}
                        </div>
                      )}

                      {appDetails.documents.length === 0 ? (
                        <div style={{ padding: '16px', background: '#F8FAFC', borderRadius: '8px', color: '#64748B', fontSize: '13px' }}>
                          No documents uploaded for this application.
                        </div>
                      ) : (
                        <div className="review-docs-list">
                          {appDetails.documents.map((doc) => (
                            <div key={doc.id} className="review-doc-card">
                              <div className="review-doc-info">
                                <div className="review-doc-icon">
                                  <FileText size={18} />
                                </div>
                                <div>
                                  <h5 className="review-doc-name">{doc.fileName || doc.documentType}</h5>
                                  <div className="review-doc-meta">
                                    <span>Type: {doc.documentType}</span>
                                    <span>Status: <strong style={{ color: doc.verificationStatus === 'VERIFIED' ? '#16A34A' : '#D97706' }}>{doc.verificationStatus}</strong></span>
                                    {doc.ocr?.confidence && (
                                      <span className="ocr-chip">
                                        OCR Confidence: {Math.round(doc.ocr.confidence * 100)}%
                                      </span>
                                    )}
                                  </div>
                                </div>
                              </div>

                              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                                {doc.verificationStatus !== 'VERIFIED' ? (
                                  <button
                                    type="button"
                                    disabled={verifyingDocId === doc.id}
                                    onClick={async (e) => {
                                      e.preventDefault();
                                      e.stopPropagation();
                                      setDocActionError(null);
                                      setVerifyingDocId(doc.id);
                                      try {
                                        await verifyDocumentStatus(doc.id, 'VERIFIED');
                                        setAppDetails(prev => prev ? ({
                                          ...prev,
                                          documents: prev.documents.map(d => d.id === doc.id ? { ...d, verificationStatus: 'VERIFIED', isVerified: true } : d)
                                        }) : prev);
                                        setSuccessToast(`Document "${doc.fileName || doc.documentType}" marked as VERIFIED.`);
                                        setTimeout(() => setSuccessToast(null), 3500);
                                      } catch (err) {
                                        console.warn('Failed to verify document:', err.message);
                                        setDocActionError(err.message || 'Failed to verify document');
                                        setTimeout(() => setDocActionError(null), 5000);
                                      } finally {
                                        setVerifyingDocId(null);
                                      }
                                    }}
                                    className="reviewer-action-btn"
                                    style={{
                                      background: '#DCFCE7',
                                      color: '#15803D',
                                      borderColor: '#86EFAC',
                                      fontSize: '11.5px',
                                      padding: '5px 10px',
                                      cursor: verifyingDocId === doc.id ? 'wait' : 'pointer',
                                      opacity: verifyingDocId === doc.id ? 0.7 : 1,
                                      display: 'inline-flex',
                                      alignItems: 'center',
                                      gap: '4px'
                                    }}
                                  >
                                    <CheckCircle2 size={13} className={verifyingDocId === doc.id ? 'animate-spin' : ''} />
                                    <span>{verifyingDocId === doc.id ? 'Verifying...' : 'Verify Document'}</span>
                                  </button>
                                ) : (
                                  <span
                                    style={{
                                      fontSize: '11.5px',
                                      fontWeight: 700,
                                      color: '#15803D',
                                      backgroundColor: '#DCFCE7',
                                      border: '1px solid #86EFAC',
                                      padding: '3px 8px',
                                      borderRadius: '4px',
                                      display: 'inline-flex',
                                      alignItems: 'center',
                                      gap: '4px'
                                    }}
                                  >
                                    <Check size={12} />
                                    <span>Verified</span>
                                  </span>
                                )}
                                {doc.fileUrl ? (
                                  <a
                                    href={doc.fileUrl}
                                    target="_blank"
                                    rel="noreferrer"
                                    className="reviewer-action-btn"
                                  >
                                    <span>View Document</span>
                                    <ExternalLink size={12} />
                                  </a>
                                ) : (
                                  <span style={{ fontSize: '12px', color: '#94A3B8' }}>Vault Encrypted</span>
                                )}
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>

                    {/* Addition 2: Cross-Document Conflict Resolution Workflow */}
                    {appDetails.evidenceConflicts && appDetails.evidenceConflicts.length > 0 && (
                      <div style={{ marginTop: '20px', padding: '16px', backgroundColor: '#FFF1F2', borderRadius: '10px', border: '1px solid #FECDD3' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '10px' }}>
                          <AlertTriangle size={18} color="#E11D48" />
                          <h4 style={{ margin: 0, fontSize: '14.5px', fontWeight: 700, color: '#BE123C' }}>
                            Cross-Document Discrepancies Requiring Review ({appDetails.evidenceConflicts.length})
                          </h4>
                        </div>
                        <p style={{ margin: '0 0 12px 0', fontSize: '12.5px', color: '#9F1239' }}>
                          Discrepant factual assertions detected across applicant documents. Official reviewer resolution is required to establish authoritative evidence.
                        </p>

                        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
                          {appDetails.evidenceConflicts.map((conf) => {
                            const isResolved = conf.status === 'RESOLVED';
                            const isBeingResolved = resolvingConflictId === conf.conflict_id;

                            return (
                              <div
                                key={conf.conflict_id}
                                style={{
                                  padding: '14px',
                                  backgroundColor: '#FFFFFF',
                                  borderRadius: '8px',
                                  border: `1px solid ${isResolved ? '#BBF7D0' : '#FDA4AF'}`
                                }}
                              >
                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
                                  <div style={{ fontWeight: 700, fontSize: '13px', color: '#881337' }}>
                                    Category: {conf.field_category}
                                  </div>
                                  <span
                                    style={{
                                      fontSize: '11px',
                                      fontWeight: 700,
                                      padding: '3px 8px',
                                      borderRadius: '4px',
                                      backgroundColor: isResolved ? '#DCFCE7' : '#FEE2E2',
                                      color: isResolved ? '#15803D' : '#BE123C',
                                      border: `1px solid ${isResolved ? '#86EFAC' : '#FCA5A5'}`
                                    }}
                                  >
                                    {conf.status} ({conf.severity || 'HIGH'} PRIORITY)
                                  </span>
                                </div>

                                <p style={{ margin: '0 0 10px 0', fontSize: '12.5px', color: '#4C0519', lineHeight: 1.4 }}>
                                  {conf.explanation}
                                </p>

                                {/* Conflicting excerpts comparison */}
                                {conf.source_excerpts && (
                                  <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '10px', marginBottom: '10px' }}>
                                    {conf.source_excerpts.map((src, sIdx) => (
                                      <div key={sIdx} style={{ padding: '10px', backgroundColor: '#F8FAFC', borderRadius: '6px', border: '1px solid #E2E8F0', fontSize: '12px' }}>
                                        <div style={{ fontWeight: 700, color: '#334155', marginBottom: '4px' }}>
                                          Source {sIdx + 1}: {src.source || 'Document'}
                                        </div>
                                        <div style={{ fontStyle: 'italic', color: '#475569', marginBottom: '4px' }}>
                                          &ldquo;{src.excerpt}&rdquo;
                                        </div>
                                        {src.value !== undefined && (
                                          <div style={{ fontWeight: 600, color: '#0F766E' }}>
                                            Extracted: {typeof src.value === 'number' ? `₹${src.value.toLocaleString('en-IN')}` : String(src.value)}
                                          </div>
                                        )}
                                      </div>
                                    ))}
                                  </div>
                                )}

                                {/* Resolution Details or Form */}
                                {isResolved ? (
                                  <div style={{ padding: '8px 12px', backgroundColor: '#F0FDF4', borderRadius: '6px', border: '1px solid #BBF7D0', fontSize: '12px', color: '#166534' }}>
                                    <strong>Resolved By:</strong> {conf.resolved_by || 'Caseworker'} | <strong>Audit Reason:</strong> {conf.resolution_reason}
                                  </div>
                                ) : isBeingResolved ? (
                                  <div style={{ padding: '12px', backgroundColor: '#FFF7ED', borderRadius: '6px', border: '1px solid #FED7AA', marginTop: '8px' }}>
                                    <div style={{ fontWeight: 600, fontSize: '12.5px', color: '#9A3412', marginBottom: '8px' }}>
                                      Caseworker Resolution Justification (Mandatory)
                                    </div>
                                    <div style={{ marginBottom: '8px' }}>
                                      <label style={{ display: 'block', fontSize: '11.5px', color: '#7C2D12', marginBottom: '4px' }}>
                                        Authoritative Source Selected:
                                      </label>
                                      <select
                                        value={selectedAuthoritativeSource}
                                        onChange={(e) => setSelectedAuthoritativeSource(e.target.value)}
                                        style={{ width: '100%', padding: '6px 8px', borderRadius: '4px', border: '1px solid #CBD5E1', fontSize: '12px' }}
                                      >
                                        <option value="DOCUMENT">Verified Official Document Excerpt</option>
                                        <option value="USER_INPUT">Citizen Self-Declaration</option>
                                        <option value="FIELD_INSPECTION">Physical Field Inspection Report</option>
                                      </select>
                                    </div>
                                    <textarea
                                      rows={2}
                                      placeholder="State verified official rationale (e.g. Verified latest Tehsildar certificate over self-declared profile estimate)..."
                                      value={conflictReason}
                                      onChange={(e) => setConflictReason(e.target.value)}
                                      style={{ width: '100%', padding: '8px', borderRadius: '4px', border: '1px solid #CBD5E1', fontSize: '12px', boxSizing: 'border-box' }}
                                    />
                                    <div style={{ display: 'flex', gap: '8px', marginTop: '8px' }}>
                                      <button
                                        type="button"
                                        disabled={isSubmittingConflictResolution || !conflictReason.trim()}
                                        onClick={async () => {
                                          setIsSubmittingConflictResolution(true);
                                          try {
                                            await resolveEvidenceConflict(conf.conflict_id, {
                                              reason: conflictReason.trim(),
                                              selectedSource: selectedAuthoritativeSource
                                            });
                                            conf.status = 'RESOLVED';
                                            conf.resolution_reason = conflictReason.trim();
                                            conf.resolved_by = 'Caseworker';
                                            setResolvingConflictId(null);
                                            setConflictReason('');
                                            setSuccessToast(`Discrepancy resolved successfully.`);
                                            setTimeout(() => setSuccessToast(null), 3500);
                                          } catch (err) {
                                            alert(`Failed to resolve conflict: ${err.message}`);
                                          } finally {
                                            setIsSubmittingConflictResolution(false);
                                          }
                                        }}
                                        style={{ padding: '6px 14px', backgroundColor: '#0F766E', color: '#FFFFFF', border: 'none', borderRadius: '4px', fontSize: '12px', fontWeight: 600, cursor: 'pointer' }}
                                      >
                                        {isSubmittingConflictResolution ? 'Saving...' : 'Submit Official Resolution'}
                                      </button>
                                      <button
                                        type="button"
                                        onClick={() => { setResolvingConflictId(null); setConflictReason(''); }}
                                        style={{ padding: '6px 12px', backgroundColor: '#FFFFFF', border: '1px solid #CBD5E1', borderRadius: '4px', fontSize: '12px', cursor: 'pointer' }}
                                      >
                                        Cancel
                                      </button>
                                    </div>
                                  </div>
                                ) : (
                                  <button
                                    type="button"
                                    onClick={() => { setResolvingConflictId(conf.conflict_id); setConflictReason(''); }}
                                    style={{
                                      padding: '5px 12px',
                                      backgroundColor: '#BE123C',
                                      color: '#FFFFFF',
                                      border: 'none',
                                      borderRadius: '4px',
                                      fontSize: '11.5px',
                                      fontWeight: 600,
                                      cursor: 'pointer',
                                      marginTop: '4px'
                                    }}
                                  >
                                    Resolve Discrepancy
                                  </button>
                                )}
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}

                    {/* Review History / Timeline */}
                    <div>
                      <h4 className="review-panel-title">
                        <Clock size={16} />
                        <span>Auditable Review Timeline</span>
                      </h4>

                      {appDetails.reviewHistory.length === 0 ? (
                        <div style={{ padding: '12px', background: '#F8FAFC', borderRadius: '8px', color: '#64748B', fontSize: '12.5px' }}>
                          No prior decisions recorded. Application is awaiting initial officer review.
                        </div>
                      ) : (
                        <div className="review-timeline">
                          {appDetails.reviewHistory.map((rev) => (
                            <div key={rev.id} className={`review-timeline-item ${rev.status || rev.decision}`}>
                              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                                <strong>Decision: {String(rev.decision || rev.status).toUpperCase()}</strong>
                                <span style={{ color: '#64748B' }}>{new Date(rev.createdAt).toLocaleString('en-IN')}</span>
                              </div>
                              <div><strong>Reviewer:</strong> {rev.reviewerName}</div>
                              {rev.remark && (
                                <div style={{ marginTop: '4px', fontStyle: 'italic', color: '#334155' }}>
                                  "{rev.remark}"
                                </div>
                              )}
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  </>
                )}
              </div>

              {/* Modal Decision Actions Bar */}
              {appDetails && (
                <div className="review-actions-bar">
                  <div style={{ fontSize: '12.5px', color: '#64748B' }}>
                    Reviewer actions alter application state and generate persistent citizen notifications.
                  </div>

                  <div style={{ display: 'flex', gap: '10px' }}>
                    <button
                      type="button"
                      onClick={() => handleOpenDecision('request_changes')}
                      className="btn-request-changes"
                    >
                      <AlertTriangle size={15} />
                      <span>Request Changes</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleOpenDecision('reject')}
                      className="btn-reject"
                    >
                      <XCircle size={15} />
                      <span>Reject</span>
                    </button>

                    <button
                      type="button"
                      onClick={() => handleOpenDecision('approve')}
                      className="btn-approve"
                    >
                      <CheckCircle2 size={15} />
                      <span>Approve</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Decision Remark Dialog */}
        {decisionModal.isOpen && (
          <div className="decision-dialog-backdrop">
            <div className="decision-dialog-card">
              <h3 className="decision-dialog-title">
                {decisionModal.decisionType === 'approve' && 'Approve Government Application'}
                {decisionModal.decisionType === 'request_changes' && 'Request Application Modifications'}
                {decisionModal.decisionType === 'reject' && 'Reject Government Application'}
              </h3>

              <p className="decision-dialog-sub">
                {decisionModal.decisionType === 'approve' &&
                  'Application will transition to Approved status. Add an optional administrative approval remark for the citizen.'}
                {decisionModal.decisionType === 'request_changes' &&
                  'Application will transition to Action Required. You MUST provide a clear remark instructing the applicant on what documents or facts need rectification.'}
                {decisionModal.decisionType === 'reject' &&
                  'Application will transition to Rejected. You MUST provide an official administrative justification for rejection.'}
              </p>

              {decisionModal.error && (
                <div style={{ padding: '8px 12px', background: '#FEE2E2', color: '#B91C1C', borderRadius: '6px', marginBottom: '12px', fontSize: '13px' }}>
                  {decisionModal.error}
                </div>
              )}

              <label style={{ display: 'block', fontSize: '12.5px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                Reviewer Remark {decisionModal.decisionType !== 'approve' && <span style={{ color: '#DC2626' }}>* (Mandatory)</span>}
              </label>

              <textarea
                className="decision-textarea"
                placeholder={
                  decisionModal.decisionType === 'approve'
                    ? 'Enter approval notes (optional)...'
                    : 'Enter detailed statutory reason or instructions for the citizen (required)...'
                }
                value={decisionModal.remark}
                onChange={(e) => setDecisionModal(prev => ({ ...prev, remark: e.target.value, error: '' }))}
              />

              <div className="decision-dialog-footer">
                <button
                  type="button"
                  className="reviewer-btn-secondary"
                  style={{ color: '#0F172A', borderColor: '#CBD5E1', background: '#FFFFFF' }}
                  onClick={() => setDecisionModal({ isOpen: false, decisionType: null, remark: '', error: '' })}
                  disabled={submittingDecision}
                >
                  Cancel
                </button>
                <button
                  type="button"
                  className={
                    decisionModal.decisionType === 'approve' ? 'btn-approve' :
                    decisionModal.decisionType === 'request_changes' ? 'btn-request-changes' : 'btn-reject'
                  }
                  onClick={handleConfirmDecision}
                  disabled={submittingDecision}
                >
                  <span>{submittingDecision ? 'Submitting...' : 'Confirm Decision'}</span>
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </PageContainer>
  );
}
