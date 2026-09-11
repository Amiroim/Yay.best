import React, { useEffect, useState } from 'react';
import '../styles/AlertToast.css';

const TOAST_TYPES = {
  error: {
    color: '#ef4444',
    bgColor: 'rgba(239, 68, 68, 0.08)',
    borderColor: 'rgba(239, 68, 68, 0.3)',
    glowColor: 'rgba(239, 68, 68, 0.4)',
    icon: (
      <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3Z M12 9v4 M12 17h.01" />
    )
  },
  success: {
    color: '#22c55e',
    bgColor: 'rgba(34, 197, 94, 0.08)',
    borderColor: 'rgba(34, 197, 94, 0.3)',
    glowColor: 'rgba(34, 197, 94, 0.4)',
    icon: (
      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3" />
    )
  },
  info: {
    color: '#3b82f6',
    bgColor: 'rgba(59, 130, 246, 0.08)',
    borderColor: 'rgba(59, 130, 246, 0.3)',
    glowColor: 'rgba(59, 130, 246, 0.4)',
    icon: (
      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M12 8v4 M12 16h.01" />
    )
  }
};

export default function AlertToast({ 
  title, 
  message, 
  onClose, 
  isOpen, 
  type = 'error', 
  duration = 4000 
}) {
  const [isRendered, setIsRendered] = useState(isOpen);
  const [isAnimatingOut, setIsAnimatingOut] = useState(false);

  const theme = TOAST_TYPES[type] || TOAST_TYPES.error;

  const handleClose = () => {
    setIsAnimatingOut(true);
    setTimeout(() => {
      setIsAnimatingOut(false);
      setIsRendered(false);
      if (onClose) onClose();
    }, 300);
  };

  useEffect(() => {
    if (isOpen) {
      setIsRendered(true);
      setIsAnimatingOut(false);

      const timer = setTimeout(() => {
        handleClose();
      }, duration);

      return () => clearTimeout(timer);
    } else if (isRendered) {
      handleClose();
    }
  }, [isOpen, duration]);

  if (!isRendered) return null;

  const cssVariables = {
    '--type-color': theme.color,
    '--bg-color': theme.bgColor,
    '--border-color': theme.borderColor,
    '--glow-color': theme.glowColor,
    '--toast-duration': `${duration}ms`,
  };

  return (
    
    <div dir="rtl" className="toast-fixed-wrapper">
      <div
        className={`toast-card ${isAnimatingOut ? 'toast-container-exit' : 'toast-container-enter'}`}
        style={cssVariables}
      >
        <div className="toast-glow-bar" />

        <div className="toast-content">
          <div className="toast-icon-wrapper">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="22"
              height="22"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              {theme.icon}
            </svg>
          </div>

          <div className="toast-text-wrapper">
            <span className="toast-title">{title}</span>
            <span className="toast-message">{message}</span>
          </div>
        </div>

        <div className="toast-close-btn" onClick={handleClose}>
          <svg className="toast-spinner-svg" width="32" height="32" viewBox="0 0 32 32">
            <circle className="toast-spinner-bg" cx="16" cy="16" r="14" />
            <circle className="toast-spinner-progress" cx="16" cy="16" r="14" />
          </svg>

          <span className="toast-close-icon">✕</span>
        </div>
      </div>
    </div>
  );
}