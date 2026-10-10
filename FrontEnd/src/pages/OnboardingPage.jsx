import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Compass,
  ArrowRight,
  ArrowLeft,
  Sparkles,
  ShieldCheck,
  FileText,
  Search,
  CheckCircle2,
  AlertTriangle,
  FileSearch,
  Landmark,
  User,
  ArrowUpRight,
  HelpCircle
} from 'lucide-react';
import { FinLogoClear } from '../components/common/BrandAssets';
import ashokStambhGold from '../assets/ashok_stambh_gold.png';
import { getStoredUser } from '../services/authService';

export default function OnboardingPage() {
  const [currentStep, setCurrentStep] = useState(0);
  const navigate = useNavigate();

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

  const handleSkipToDashboard = () => {
    markCompleted();
    navigate('/dashboard', { replace: true });
  };

  const handleNext = () => {
    if (currentStep < 4) {
      setCurrentStep((prev) => prev + 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    } else {
      markCompleted();
      navigate('/dashboard', { replace: true });
    }
  };

  const handlePrev = () => {
    if (currentStep > 0) {
      setCurrentStep((prev) => prev - 1);
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const stepsMeta = [
    { label: 'Welcome', icon: Sparkles },
    { label: 'Profile Setup', icon: User },
    { label: 'Find Schemes', icon: Search },
    { label: 'Check Eligibility', icon: ShieldCheck },
    { label: 'Apply & Vault', icon: FileText }
  ];

  return (
    <div className="onboarding-page-container">
      {/* Top Banner Navigation */}
      <header className="onboarding-page-header">
        <div className="onboarding-page-brand">
          <FinLogoClear />
          <div className="onboarding-page-emblem" aria-hidden="true">
            <img src={ashokStambhGold} alt="National Emblem" className="welcome-emblem-img" />
            <span>Govt. of India • HackMatrix FIN-03</span>
          </div>
        </div>

        <div className="onboarding-header-actions">
          <button
            type="button"
            className="onboarding-skip-header-btn"
            onClick={handleSkipToDashboard}
            title="Skip onboarding and go directly to Dashboard"
          >
            <span>Skip Tour</span>
            <ArrowUpRight size={14} />
          </button>
        </div>
      </header>

      {/* Stepper Navigation Progress Bar */}
      <section className="onboarding-stepper-nav-card" aria-label="Onboarding Sections">
        <div className="onboarding-stepper-segments">
          {stepsMeta.map((s, idx) => {
            const isCurrent = idx === currentStep;
            const isDone = idx < currentStep;
            const StepIcon = s.icon;
            return (
              <button
                key={s.label}
                type="button"
                className={`onboarding-step-segment ${isCurrent ? 'active' : ''} ${isDone ? 'completed' : ''}`}
                onClick={() => setCurrentStep(idx)}
                aria-label={`Jump to Section ${idx + 1}: ${s.label}`}
              >
                <div className="onboarding-step-segment-icon">
                  {isDone ? '✓' : <StepIcon size={14} />}
                </div>
                <div className="onboarding-step-segment-text">
                  <span className="onboarding-step-seq">Section {idx + 1}</span>
                  <span className="onboarding-step-name">{s.label}</span>
                </div>
              </button>
            );
          })}
        </div>
      </section>

      {/* Main Content Section Card */}
      <main className="onboarding-content-card">
        {/* SECTION 1: WELCOME & OVERVIEW */}
        {currentStep === 0 && (
          <div className="onboarding-section-slide" key="sec-0">
            <div className="onboarding-heading-box">
              <span className="welcome-pill">
                <Sparkles size={13} color="#005B50" />
                <span>Namaste, {userName}!</span>
              </span>
              <h1 className="onboarding-main-title">
                Welcome to HackMatrix
              </h1>
              <p className="onboarding-main-subtitle">
                Your simple, trusted guide to discovering official government schemes, understanding what benefits you qualify for, and applying with confidence.
              </p>
            </div>

            <div className="onboarding-cards-grid">
              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle green">
                  <Search size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Official Government Schemes</h3>
                  <p className="onboarding-info-desc">
                    Explore verified welfare programs directly from Central Ministries and State Government policy gazettes without intermediaries.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle blue">
                  <ShieldCheck size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Instant Eligibility Checks</h3>
                  <p className="onboarding-info-desc">
                    Find out in seconds whether you meet statutory conditions based on your age, domicile, occupation, and family income.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle amber">
                  <FileText size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Secure Document Vault</h3>
                  <p className="onboarding-info-desc">
                    Upload your certificates once. Our layered AI OCR extracts key facts so you can reuse them for any welfare application.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* SECTION 2: PROFILE SETUP */}
        {currentStep === 1 && (
          <div className="onboarding-section-slide" key="sec-1">
            <div className="onboarding-heading-box">
              <span className="welcome-pill">
                <User size={13} color="#005B50" />
                <span>Section 2 of 5 • Profile Setup</span>
              </span>
              <h1 className="onboarding-main-title">
                Why Your Profile Details Matter
              </h1>
              <p className="onboarding-main-subtitle">
                Government policies have specific rules based on who you are. Keeping your profile details accurate unlocks the exact benefits tailored for you.
              </p>
            </div>

            <div className="onboarding-cards-grid">
              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle green">
                  <Landmark size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">State &amp; District Domicile</h3>
                  <p className="onboarding-info-desc">
                    Many schemes depend on your residence (like Gujarat or Himachal Pradesh). Your location unlocks local state benefits and regional quotas.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle blue">
                  <User size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Social Category &amp; Occupation</h3>
                  <p className="onboarding-info-desc">
                    Targeted welfare programs exist for farmers, students, construction workers, artisans, women, and senior citizens.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle amber">
                  <FileSearch size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Accurate Family Income</h3>
                  <p className="onboarding-info-desc">
                    Enter your genuine annual household income to match statutory income thresholds (e.g. up to ₹1,50,000 or ₹2,50,000).
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* SECTION 3: SCHEME DISCOVERY */}
        {currentStep === 2 && (
          <div className="onboarding-section-slide" key="sec-2">
            <div className="onboarding-heading-box">
              <span className="welcome-pill">
                <Search size={13} color="#005B50" />
                <span>Section 3 of 5 • Scheme Discovery</span>
              </span>
              <h1 className="onboarding-main-title">
                Explore Policies &amp; Financial Benefits
              </h1>
              <p className="onboarding-main-subtitle">
                Search through authentic policy gazettes with clear summaries, transparent rules, and direct benefit details.
              </p>
            </div>

            <div className="onboarding-cards-grid">
              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle green">
                  <Search size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Central and State Filters</h3>
                  <p className="onboarding-info-desc">
                    Switch easily between All-India Central programs and welfare schemes issued by your home state government.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle blue">
                  <CheckCircle2 size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Direct Benefit Breakdown (DBT)</h3>
                  <p className="onboarding-info-desc">
                    See exact grant amounts—such as monthly pensions, student stipends, or one-time equipment subsidies.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle amber">
                  <FileText size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Plain English Guidelines</h3>
                  <p className="onboarding-info-desc">
                    No confusing legal jargon. We present the policy goals, eligibility rules, and required proofs in clear, simple language.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* SECTION 4: ELIGIBILITY & CONFLICTING EVIDENCE */}
        {currentStep === 3 && (
          <div className="onboarding-section-slide" key="sec-3">
            <div className="onboarding-heading-box">
              <span className="welcome-pill">
                <ShieldCheck size={13} color="#005B50" />
                <span>Section 4 of 5 • Eligibility Checks</span>
              </span>
              <h1 className="onboarding-main-title">
                Know Exactly Where You Stand
              </h1>
              <p className="onboarding-main-subtitle">
                Our FIN-03 engine tests your information against official rules and explains your status in simple words.
              </p>
            </div>

            {/* 4 Status Badges Explained */}
            <div className="welcome-status-summary-grid">
              <div className="welcome-status-mini-card status-satisfied">
                <span className="welcome-status-badge green">Satisfied</span>
                <p className="welcome-status-mini-text">You meet all statutory conditions and your proof is ready.</p>
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
                <p className="welcome-status-mini-text">You do not meet the age, state, or income limit.</p>
              </div>
            </div>

            {/* Callout Box without raw markdown asterisks */}
            <div className="welcome-callout-card" style={{ marginTop: '16px' }}>
              <div className="welcome-callout-icon">
                <AlertTriangle size={20} color="#D97706" />
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

        {/* SECTION 5: DOCUMENT VAULT & READY TO APPLY */}
        {currentStep === 4 && (
          <div className="onboarding-section-slide" key="sec-4">
            <div className="onboarding-heading-box">
              <span className="welcome-pill">
                <FileText size={13} color="#005B50" />
                <span>Section 5 of 5 • Ready to Apply</span>
              </span>
              <h1 className="onboarding-main-title">
                Upload Once, Apply Anytime
              </h1>
              <p className="onboarding-main-subtitle">
                Store your certificates in your secure digital vault. Our smart reader pulls facts automatically so you never retype them.
              </p>
            </div>

            <div className="onboarding-cards-grid">
              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle green">
                  <ShieldCheck size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Document Readiness Score</h3>
                  <p className="onboarding-info-desc">
                    Reach 100% readiness to make sure your application dossier has every required proof before you submit.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle blue">
                  <FileText size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">1-Click Application Dossier</h3>
                  <p className="onboarding-info-desc">
                    Submit applications directly to government review desks with verified documents neatly packaged.
                  </p>
                </div>
              </div>

              <div className="onboarding-info-card">
                <div className="welcome-feature-icon-circle amber">
                  <Sparkles size={20} />
                </div>
                <div>
                  <h3 className="onboarding-info-title">Real-Time Application Tracking</h3>
                  <p className="onboarding-info-desc">
                    Track your submissions through every step—from review to verification and final sanction.
                  </p>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Bottom Section Controls */}
        <div className="onboarding-stepper-footer">
          <div className="onboarding-footer-left">
            <button
              type="button"
              className="welcome-btn-skip"
              onClick={handleSkipToDashboard}
            >
              Skip for Now
            </button>
            <span className="onboarding-step-counter">
              Section {currentStep + 1} of 5
            </span>
          </div>

          <div className="onboarding-footer-right">
            {currentStep > 0 && (
              <button
                type="button"
                className="welcome-btn-prev"
                onClick={handlePrev}
              >
                <ArrowLeft size={15} />
                <span>Previous Section</span>
              </button>
            )}

            {currentStep < 4 ? (
              <button
                type="button"
                className="welcome-btn-next"
                onClick={handleNext}
                autoFocus
              >
                <span>Next Section</span>
                <ArrowRight size={15} />
              </button>
            ) : (
              <button
                type="button"
                className="welcome-btn-primary"
                onClick={handleNext}
                autoFocus
              >
                <Sparkles size={16} />
                <span>Go to Dashboard</span>
                <ArrowRight size={15} />
              </button>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
