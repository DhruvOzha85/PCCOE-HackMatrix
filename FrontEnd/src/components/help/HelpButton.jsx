import React from 'react';
import { HelpCircle } from 'lucide-react';

export default function HelpButton({ onClick, isOpen, className = '', isFloating = false }) {
  if (isFloating) {
    return (
      <button
        type="button"
        className={`floating-help-btn ${isOpen ? 'active' : ''} ${className}`}
        onClick={onClick}
        aria-label="Help & Guidance"
        aria-expanded={isOpen}
        title="Open Help & Guidance"
      >
        <HelpCircle size={20} strokeWidth={2.2} />
        <span className="floating-help-tooltip">Need Help?</span>
      </button>
    );
  }

  return (
    <button
      type="button"
      className={`header-help-btn ${isOpen ? 'active' : ''} ${className}`}
      onClick={onClick}
      aria-label="Help & Support Documentation"
      aria-expanded={isOpen}
      title="Help & Support"
    >
      <HelpCircle size={17} strokeWidth={2.2} />
      <span className="header-help-badge" aria-hidden="true">Help</span>
    </button>
  );
}
