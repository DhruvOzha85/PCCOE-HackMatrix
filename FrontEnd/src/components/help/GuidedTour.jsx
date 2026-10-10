import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Compass,
  ArrowRight,
  ArrowLeft,
  X,
  CheckCircle2,
  Sparkles,
  ShieldCheck,
  FileText,
  Search,
  User,
  Inbox
} from 'lucide-react';
import { getStoredUser } from '../../services/authService';

export const TOUR_STEPS = [
  {
    id: 'dashboard-overview',
    title: 'Dashboard Overview',
    badge: 'Step 1: Your Command Center',
    icon: Compass,
    path: '/dashboard',
    selector: '.hero-banner, .dashboard-layout, .metric-cards-grid',
    description: 'Welcome to HackMatrix! Your dashboard aggregates your verified eligibility metrics, potential financial benefits, and top government welfare opportunities tailored to your profile.'
  },
  {
    id: 'complete-profile',
    title: 'Complete Your Profile',
    badge: 'Step 2: Credential Accuracy',
    icon: User,
    path: '/dashboard',
    selector: '.profile-banner, .user-profile-badge, .header-profile-wrapper',
    description: 'Accurate demographic and income attributes are essential. The FIN-03 engine uses your age, state, caste category, and annual income to determine statutory eligibility without arbitrary assumptions.'
  },
  {
    id: 'discover-schemes',
    title: 'Discover Government Schemes',
    badge: 'Step 3: Policy Discovery',
    icon: Search,
    path: '/dashboard',
    selector: 'a[href="/discover"], .sidebar-nav',
    description: 'Explore the national catalog of Central and State welfare policies. Use precise filters for state jurisdiction, administering department, and targeted beneficiary groups like Students, Farmers, or Women.'
  },
  {
    id: 'eligibility-analysis',
    title: 'FIN-03 Eligibility Analysis',
    badge: 'Step 4: Evidence-Based Decisions',
    icon: ShieldCheck,
    path: '/dashboard',
    selector: '.top-opportunities-section, .opportunities-grid',
    description: 'Understand exactly WHY you qualify or what is missing. The system categorizes rules into Satisfied, Unmet, Missing Evidence, and Conflicting Evidence with actionable steps to resolve blockers.'
  },
  {
    id: 'document-vault',
    title: 'Document Vault & AI OCR',
    badge: 'Step 5: Digital Vault',
    icon: FileText,
    path: '/dashboard',
    selector: 'a[href="/documents"], .sidebar-nav',
    description: 'Store statutory certificates securely. Layered AI OCR extracts facts (DOB, income, caste) and verifies document readiness against policy rules before you submit an application.'
  },
  {
    id: 'apply-for-scheme',
    title: 'Seamless Application Flow',
    badge: 'Step 6: One-Click Application',
    icon: Sparkles,
    path: '/dashboard',
    selector: 'a[href="/documents"], .sidebar-nav',
    description: 'When all required documents in your vault are verified, apply with one click. Pre-selected schemes lock in automatically, linking your verified vault documents directly to the submission.'
  },
  {
    id: 'track-applications',
    title: 'Track Application Lifecycles',
    badge: 'Step 7: Real-Time Status',
    icon: Inbox,
    path: '/dashboard',
    selector: 'a[href="/applications"], .sidebar-nav',
    description: 'Monitor your submissions from Under Review to Sanctioned Approval. If an officer requests changes, read the remarks and re-upload revised certificates right from your vault.'
  }
];

export default function GuidedTour({ isOpen, onClose, onFinish }) {
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [targetRect, setTargetRect] = useState(null);
  const popoverRef = useRef(null);
  const navigate = useNavigate();
  const location = useLocation();

  const currentStep = TOUR_STEPS[currentStepIndex] || TOUR_STEPS[0];
  const totalSteps = TOUR_STEPS.length;
  const isFirstStep = currentStepIndex === 0;
  const isLastStep = currentStepIndex === totalSteps - 1;

  // Persist tour completion safely scoped to this user
  const markTourCompleted = useCallback(() => {
    try {
      const user = getStoredUser();
      const userKey = user?.id || user?.email || 'default_user';
      localStorage.setItem(`fin_onboarding_completed_${userKey}`, 'true');
      sessionStorage.removeItem('fin_just_registered');
      window.dispatchEvent(new CustomEvent('fin_onboarding_completed'));
    } catch (_) {}
  }, []);

  // Find target element and measure its bounding box
  const updateTargetPosition = useCallback(() => {
    if (!isOpen) return;

    const step = TOUR_STEPS[currentStepIndex];
    if (!step) return;

    let targetEl = null;
    if (step.selector) {
      const selectors = step.selector.split(',').map((s) => s.trim());
      for (const sel of selectors) {
        const found = document.querySelector(sel);
        if (found) {
          targetEl = found;
          break;
        }
      }
    }

    if (targetEl) {
      targetEl.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
      const rect = targetEl.getBoundingClientRect();
      setTargetRect({
        top: rect.top,
        left: rect.left,
        width: rect.width,
        height: rect.height,
        bottom: rect.bottom,
        right: rect.right
      });
    } else {
      // Fallback center position if target element is not present on current viewport
      setTargetRect(null);
    }
  }, [isOpen, currentStepIndex]);

  useEffect(() => {
    if (isOpen) {
      updateTargetPosition();
      const timer = setTimeout(updateTargetPosition, 150);
      window.addEventListener('resize', updateTargetPosition);
      window.addEventListener('scroll', updateTargetPosition, true);
      return () => {
        clearTimeout(timer);
        window.removeEventListener('resize', updateTargetPosition);
        window.removeEventListener('scroll', updateTargetPosition, true);
      };
    }
  }, [isOpen, currentStepIndex, updateTargetPosition]);

  // Handle keyboard navigation (ArrowRight, ArrowLeft, Escape)
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        handleSkip();
      } else if (e.key === 'ArrowRight') {
        handleNext();
      } else if (e.key === 'ArrowLeft' && !isFirstStep) {
        handlePrev();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, currentStepIndex, isFirstStep]);

  const handleNext = () => {
    if (isLastStep) {
      markTourCompleted();
      if (typeof onFinish === 'function') onFinish();
      if (typeof onClose === 'function') onClose();
    } else {
      setCurrentStepIndex((prev) => prev + 1);
    }
  };

  const handlePrev = () => {
    if (!isFirstStep) {
      setCurrentStepIndex((prev) => prev - 1);
    }
  };

  const handleSkip = () => {
    markTourCompleted();
    if (typeof onClose === 'function') onClose();
  };

  if (!isOpen) return null;

  const StepIcon = currentStep.icon || Compass;

  return (
    <div className="guided-tour-overlay" role="dialog" aria-modal="true" aria-label="Guided Onboarding Tour">
      {/* Dimmed backdrop with spotlight cutout if target exists */}
      <div className="tour-backdrop" onClick={handleSkip} />

      {/* Target element spotlight highlight ring */}
      {targetRect && (
        <div
          className="tour-spotlight-ring"
          style={{
            top: `${Math.max(0, targetRect.top - 6)}px`,
            left: `${Math.max(0, targetRect.left - 6)}px`,
            width: `${targetRect.width + 12}px`,
            height: `${targetRect.height + 12}px`
          }}
          aria-hidden="true"
        />
      )}

      {/* Popover Step Card */}
      <div
        className={`tour-popover-card ${targetRect ? 'positioned' : 'centered'}`}
        ref={popoverRef}
        style={
          targetRect
            ? {
                top: `${Math.min(window.innerHeight - 340, Math.max(20, targetRect.bottom + 14))}px`,
                left: `${Math.min(window.innerWidth - 380, Math.max(16, targetRect.left))}px`
              }
            : undefined
        }
      >
        {/* Header */}
        <div className="tour-card-header">
          <div className="tour-badge-row">
            <span className="tour-step-badge">{currentStep.badge}</span>
            <span className="tour-counter">
              {currentStepIndex + 1} of {totalSteps}
            </span>
          </div>
          <button
            type="button"
            className="tour-close-btn"
            onClick={handleSkip}
            aria-label="Skip guided tour"
            title="Skip tour (Esc)"
          >
            <X size={15} />
          </button>
        </div>

        {/* Body */}
        <div className="tour-card-body">
          <div className="tour-title-group">
            <div className="tour-step-icon-wrap" aria-hidden="true">
              <StepIcon size={18} color="#005B50" />
            </div>
            <h3 className="tour-step-title">{currentStep.title}</h3>
          </div>
          <p className="tour-step-desc">{currentStep.description}</p>
        </div>

        {/* Progress Bar */}
        <div className="tour-progress-track" aria-hidden="true">
          <div
            className="tour-progress-fill"
            style={{ width: `${((currentStepIndex + 1) / totalSteps) * 100}%` }}
          />
        </div>

        {/* Footer Navigation */}
        <div className="tour-card-footer">
          <button
            type="button"
            className="tour-btn-skip"
            onClick={handleSkip}
          >
            Skip Tour
          </button>

          <div className="tour-btn-group">
            {!isFirstStep && (
              <button
                type="button"
                className="tour-btn-prev"
                onClick={handlePrev}
                aria-label="Previous tour step"
              >
                <ArrowLeft size={13} />
                <span>Previous</span>
              </button>
            )}

            <button
              type="button"
              className="tour-btn-next"
              onClick={handleNext}
              aria-label={isLastStep ? 'Finish guided tour' : 'Next tour step'}
            >
              <span>{isLastStep ? 'Finish Tour' : 'Next'}</span>
              {isLastStep ? <CheckCircle2 size={14} /> : <ArrowRight size={14} />}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
