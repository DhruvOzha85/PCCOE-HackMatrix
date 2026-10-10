import React, { useState, useRef, useEffect } from 'react';
import { HelpCircle, X, ExternalLink } from 'lucide-react';
import { Link } from 'react-router-dom';

/**
 * ContextualHelp
 * Accessible micro-popover explaining complex concepts throughout the app.
 */
export default function ContextualHelp({
  title = 'Information',
  content = '',
  linkText = '',
  linkHref = '',
  placement = 'top',
  className = ''
}) {
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef(null);

  useEffect(() => {
    if (!isOpen) return;
    const handleClickOutside = (e) => {
      if (containerRef.current && !containerRef.current.contains(e.target)) {
        setIsOpen(false);
      }
    };
    const handleKeyDown = (e) => {
      if (e.key === 'Escape') {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    document.addEventListener('keydown', handleKeyDown);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen]);

  return (
    <div className={`contextual-help-wrapper ${className}`} ref={containerRef}>
      <button
        type="button"
        className={`contextual-help-trigger ${isOpen ? 'active' : ''}`}
        onClick={() => setIsOpen((prev) => !prev)}
        aria-expanded={isOpen}
        aria-label={`Help info: ${title}`}
        title={`Learn more: ${title}`}
      >
        <HelpCircle size={13.5} strokeWidth={2.2} />
      </button>

      {isOpen && (
        <div className={`contextual-help-popover placement-${placement}`} role="tooltip">
          <div className="contextual-help-header">
            <h5 className="contextual-help-title">{title}</h5>
            <button
              type="button"
              className="contextual-help-close"
              onClick={() => setIsOpen(false)}
              aria-label="Close explanation"
            >
              <X size={12} />
            </button>
          </div>
          <div className="contextual-help-body">
            <p className="contextual-help-text">{content}</p>
            {linkText && linkHref && (
              <div className="contextual-help-link-box">
                {linkHref.startsWith('/') ? (
                  <Link
                    to={linkHref}
                    className="contextual-help-link"
                    onClick={() => setIsOpen(false)}
                  >
                    <span>{linkText}</span>
                    <ExternalLink size={11} />
                  </Link>
                ) : (
                  <a
                    href={linkHref}
                    target="_blank"
                    rel="noreferrer"
                    className="contextual-help-link"
                  >
                    <span>{linkText}</span>
                    <ExternalLink size={11} />
                  </a>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
