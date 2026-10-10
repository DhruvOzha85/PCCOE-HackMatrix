import React, { useState } from 'react';
import {
  Compass,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  ShieldCheck,
  FileText,
  Search,
  X,
  CheckCircle2,
  AlertTriangle,
  FileSearch,
  Landmark,
  User,
  ExternalLink
} from 'lucide-react';
import { FinLogoClear } from '../common/BrandAssets';
import ashokStambhGold from '../../assets/ashok_stambh_gold.png';
import { getStoredUser } from '../../services/authService';

export default function WelcomeOnboardingModal({ isOpen, onStartTour, onDismiss }) {
  const [currentStep, setCurrentStep] = useState(0);

  if (!isOpen) return null;

  const user = getStoredUser();
  const userName = user?.fullName || user?.name || 'Citizen';

  const markCompleted = () => {
    try {
      const userKey = user?.id || user?.email || 'default_user';
      localStorage.setItem(`fin_onboarding_completed_${userKey}`, 'true');
      sessionStorage.removeItem('fin_just_registered');
      window.dispatchEvent(new CustomEvent('fin_onboarding_completed'));
    } catch (_) {}
  };

  const handleSkip = () => {
    markCompleted();
    if (typeof onDismiss === 'function') onDismiss();
  };

  const handleStartTour = () => {
    markCompleted();
    if (typeof onStartTour === 'function') onStartTour();
  };

  const handleFinish = () => {
    markCompleted();
    if (typeof onDismiss === 'function') onDismiss();
  };

  const handleNext = () => {
    if (currentStep < 4) {
      setCurrentStep((prev) => prev + 1);
    } else {
      handleFinish();
    }
  };

  const handlePrev = () => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
    }
  };

  const stepsMeta = [
    { label: 'Welcome', icon: Sparkles },
    { label: 'Profile', icon: User },
    { label: 'Schemes', icon: Search },
    { label: 'Eligibility', icon: ShieldCheck },
    { label: 'Apply', icon: FileText }
  ];

  return (
    <div
      className="welcome-modal-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="welcome-modal-title"
    >
      <div className="welcome-modal-backdrop" onClick={handleSkip} />

      <div className="welcome-modal-card welcome-modal-card-stepper">
        {/* Top Header Bar */}
        <div className="welcome-modal-header">
          <div className="welcome-brand-row">
            <FinLogoClear />
            <div className="welcome-header-actions-right">
              <div className="welcome-emblem-badge" aria-hidden="true">
                <img src={ashokStambhGold} alt="National Emblem" className="welcome-emblem-img" />
                <span>Govt. of India</span>
              </div>
              <button
                type="button"
                className="welcome-close-btn"
                onClick={handleSkip}
                aria-label="Close onboarding guide"
                title="Skip and close"
              >
                <X size={17} />
              </button>
            </div>
          </div>

          {/* Stepper Navigation Dots */}
          <div className="welcome-stepper-tracker" aria-label="Onboarding Progress">
            <div className="welcome-stepper-dots">
              {stepsMeta.map((s, idx) => {
                const isCurrent = idx === currentStep;
                const isDone = idx < currentStep;
                return (
                  <button
                    key={s.label}
                    type="button"
                    className={`welcome-stepper-dot-btn ${isCurrent ? 'active' : ''} ${isDone ? 'completed' : ''}`}
                    onClick={() => setCurrentStep(idx)}
                    aria-label={`Jump to Step ${idx + 1}: ${s.label}`}
                  >
                    <span className="welcome-stepper-dot-circle">
                      {isDone ? '✓' : idx + 1}
                    </span>
                    <span className="welcome-stepper-dot-label">{s.label}</span>
                  </button>
                );
              })}
            </div>
            <div className="welcome-stepper-counter-tag">
              Step {currentStep + 1} of 5
            </div>
          </div>
        </div>

        {/* Dynamic Step Content */}
        <div className="welcome-modal-body">
          {/* STEP 1: WELCOME & OVERVIEW */}
          {currentStep === 0 && (
            <div className="welcome-step-slide" key="step-0">
              <div className="welcome-heading-group">
                <span className="welcome-pill">
                  <Sparkles size={12} color="#005B50" />
                  <span>Namaste, {userName}!</span>
                </span>
                <h2 id="welcome-modal-title" className="welcome-title">
                  Welcome to HackMatrix
                </h2>
                <p className="welcome-subtitle">
                  Your simple, trusted guide to discovering government schemes, checking what benefits you qualify for, and applying without paperwork headaches.
                </p>
              </div>

              <div className="welcome-features-grid">
                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle green">
                    <Search size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Official Government Schemes</h4>
                    <p className="welcome-feature-desc">
                      Browse verified welfare programs from Central ministries and State departments in one place.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle blue">
                    <ShieldCheck size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Instant Eligibility Checks</h4>
                    <p className="welcome-feature-desc">
                      Find out in seconds whether you qualify for a scheme based on your details and documents.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle amber">
                    <FileText size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Safe Digital Document Vault</h4>
                    <p className="welcome-feature-desc">
                      Upload your certificates once. Keep them organized and reuse them anytime you want to apply.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 2: PROFILE SETUP */}
          {currentStep === 1 && (
            <div className="welcome-step-slide" key="step-1">
              <div className="welcome-heading-group">
                <span className="welcome-pill">
                  <User size={12} color="#005B50" />
                  <span>Step 2 • Profile Setup</span>
                </span>
                <h2 id="welcome-modal-title" className="welcome-title">
                  Why Your Profile Details Matter
                </h2>
                <p className="welcome-subtitle">
                  Government welfare schemes have specific criteria. Keeping your details accurate helps us show only the benefits you actually qualify for.
                </p>
              </div>

              <div className="welcome-features-grid">
                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle green">
                    <Landmark size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">State and District</h4>
                    <p className="welcome-feature-desc">
                      Many schemes depend on your home state (such as Gujarat or Himachal Pradesh). Your location unlocks local state benefits.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle blue">
                    <User size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Category &amp; Occupation</h4>
                    <p className="welcome-feature-desc">
                      Special programs are designed for farmers, students, construction workers, women, and senior citizens.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle amber">
                    <FileSearch size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Annual Family Income</h4>
                    <p className="welcome-feature-desc">
                      Entering your true annual income ensures the system matches you with the right financial assistance slabs.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 3: SCHEME DISCOVERY */}
          {currentStep === 2 && (
            <div className="welcome-step-slide" key="step-2">
              <div className="welcome-heading-group">
                <span className="welcome-pill">
                  <Search size={12} color="#005B50" />
                  <span>Step 3 • Scheme Discovery</span>
                </span>
                <h2 id="welcome-modal-title" className="welcome-title">
                  Explore Policies &amp; Financial Benefits
                </h2>
                <p className="welcome-subtitle">
                  Search through official government welfare programs with clear summaries, transparent rules, and direct benefit details.
                </p>
              </div>

              <div className="welcome-features-grid">
                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle green">
                    <Search size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Central and State Filters</h4>
                    <p className="welcome-feature-desc">
                      Switch easily between All-India Central programs and specific welfare schemes from your home state.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle blue">
                    <CheckCircle2 size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Direct Benefit Breakdown</h4>
                    <p className="welcome-feature-desc">
                      See exact financial assistance amounts—such as monthly pensions, student stipends, or one-time equipment grants.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle amber">
                    <FileText size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Plain English Summaries</h4>
                    <p className="welcome-feature-desc">
                      No legal jargon. We explain the scheme purpose, who qualifies, and how funds are transferred in plain language.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {/* STEP 4: ELIGIBILITY & CONFLICTING EVIDENCE */}
          {currentStep === 3 && (
            <div className="welcome-step-slide" key="step-3">
              <div className="welcome-heading-group">
                <span className="welcome-pill">
                  <ShieldCheck size={12} color="#005B50" />
                  <span>Step 4 • Eligibility Checks</span>
                </span>
                <h2 id="welcome-modal-title" className="welcome-title">
                  Know Exactly Where You Stand
                </h2>
                <p className="welcome-subtitle">
                  Our FIN-03 engine tests your information against authentic official rules and explains your status in simple words.
                </p>
              </div>

              {/* Status Tags Mini Grid */}
              <div className="welcome-status-summary-grid">
                <div className="welcome-status-mini-card status-satisfied">
                  <span className="welcome-status-badge green">Satisfied</span>
                  <p className="welcome-status-mini-text">You meet all rules and have proof ready.</p>
                </div>
                <div className="welcome-status-mini-card status-missing">
                  <span className="welcome-status-badge blue">Missing Evidence</span>
                  <p className="welcome-status-mini-text">You qualify, but just need to upload a certificate.</p>
                </div>
                <div className="welcome-status-mini-card status-conflict">
                  <span className="welcome-status-badge amber">Conflicting Evidence</span>
                  <p className="welcome-status-mini-text">Profile numbers differ from certificate numbers.</p>
                </div>
                <div className="welcome-status-mini-card status-unmet">
                  <span className="welcome-status-badge red">Unmet</span>
                  <p className="welcome-status-mini-text">You do not meet the age or income limit.</p>
                </div>
              </div>

              {/* Highlighted Conflict Callout in Simple Natural Format */}
              <div className="welcome-callout-card">
                <div className="welcome-callout-icon">
                  <AlertTriangle size={18} color="#D97706" />
                </div>
                <div className="welcome-callout-content">
                  <h4 className="welcome-callout-title">Helpful Tip: Fixing Conflicting Evidence</h4>
                  <p className="welcome-callout-text">
                    If you declared ₹1,00,000 in your profile, but your uploaded Income Certificate shows ₹1,80,000, the FIN-03 engine flags a <strong className="welcome-highlight-text">Conflicting Evidence</strong> condition. To fix this, update your profile income to match your official certificate, or upload your latest valid certificate.
                  </p>
                </div>
              </div>
            </div>
          )}

          {/* STEP 5: DOCUMENT VAULT & APPLYING */}
          {currentStep === 4 && (
            <div className="welcome-step-slide" key="step-4">
              <div className="welcome-heading-group">
                <span className="welcome-pill">
                  <FileText size={12} color="#005B50" />
                  <span>Step 5 • Ready to Apply</span>
                </span>
                <h2 id="welcome-modal-title" className="welcome-title">
                  Upload Once, Apply Anytime
                </h2>
                <p className="welcome-subtitle">
                  Store your certificates securely in your vault. Our smart reader pulls the facts automatically so you never have to retype them.
                </p>
              </div>

              <div className="welcome-features-grid">
                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle green">
                    <ShieldCheck size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Document Readiness Score</h4>
                    <p className="welcome-feature-desc">
                      Reach a 100% readiness score to make sure your application has everything required before you hit submit.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle blue">
                    <FileText size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">One-Click Application Dossier</h4>
                    <p className="welcome-feature-desc">
                      Submit directly to government review officers with all your verified documents neatly attached.
                    </p>
                  </div>
                </div>

                <div className="welcome-feature-card">
                  <div className="welcome-feature-icon-circle amber">
                    <Sparkles size={18} />
                  </div>
                  <div>
                    <h4 className="welcome-feature-title">Real-Time Application Tracking</h4>
                    <p className="welcome-feature-desc">
                      Track the status of your applications from submission to officer verification and final sanction.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Stepper Footer Controls */}
        <div className="welcome-modal-footer welcome-stepper-footer">
          <div className="welcome-footer-left">
            <button
              type="button"
              className="welcome-btn-skip"
              onClick={handleSkip}
              title="Skip this walkthrough"
            >
              Skip
            </button>
          </div>

          <div className="welcome-footer-right">
            {currentStep > 0 && (
              <button
                type="button"
                className="welcome-btn-prev"
                onClick={handlePrev}
              >
                <ArrowLeft size={14} />
                <span>Previous</span>
              </button>
            )}

            {currentStep < 4 ? (
              <button
                type="button"
                className="welcome-btn-next"
                onClick={handleNext}
                autoFocus
              >
                <span>Next</span>
                <ArrowRight size={14} />
              </button>
            ) : (
              <div className="welcome-final-actions-group">
                <button
                  type="button"
                  className="welcome-btn-primary"
                  onClick={handleFinish}
                  autoFocus
                >
                  <Sparkles size={15} />
                  <span>Go to Dashboard</span>
                </button>
                <button
                  type="button"
                  className="welcome-btn-secondary"
                  onClick={handleStartTour}
                  title="Take a live tour of each screen"
                >
                  <Compass size={15} />
                  <span>Take Interactive Tour</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
