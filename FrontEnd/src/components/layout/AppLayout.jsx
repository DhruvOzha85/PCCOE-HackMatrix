import React, { useState, useEffect } from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';
import SessionTimeoutManager from '../common/SessionTimeoutManager';
import FinAssistantModal from '../chat/FinAssistantModal';
import Footer from './Footer';
import HelpPanel from '../help/HelpPanel';
import WelcomeOnboardingModal from '../help/WelcomeOnboardingModal';
import GuidedTour from '../help/GuidedTour';
import HelpButton from '../help/HelpButton';
import { getStoredUser } from '../../services/authService';

export default function AppLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [isChatMinimized, setIsChatMinimized] = useState(false);

  // Help & Onboarding States
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isWelcomeOpen, setIsWelcomeOpen] = useState(false);
  const [isTourOpen, setIsTourOpen] = useState(false);

  const toggleSidebar = () => setIsSidebarOpen((prev) => !prev);
  const closeSidebar = () => setIsSidebarOpen(false);

  const toggleChat = () => {
    setIsChatOpen((prev) => !prev);
    setIsChatMinimized(false);
  };

  const closeChat = () => {
    setIsChatOpen(false);
    setIsChatMinimized(false);
  };

  const minimizeChat = () => {
    setIsChatMinimized((prev) => !prev);
  };

  const toggleHelp = () => {
    setIsHelpOpen((prev) => !prev);
  };

  // Check for first-time account registration onboarding
  useEffect(() => {
    try {
      // If already on the dedicated onboarding page, don't show the duplicate overlay modal
      if (location.pathname === '/onboarding') {
        setIsWelcomeOpen(false);
        return;
      }

      const user = getStoredUser();
      const userKey = user?.id || user?.email || 'citizen';
      const isCompleted = localStorage.getItem(`fin_onboarding_completed_${userKey}`);
      const justRegistered = sessionStorage.getItem('fin_just_registered');
      const stateRequested = location.state?.showOnboarding;

      // Pop open if requested or if onboarding has never been completed
      if (stateRequested || justRegistered === 'true' || !isCompleted) {
        setIsWelcomeOpen(true);
      }
    } catch (_) {}
  }, [location.pathname, location.state]);

  // Global listeners for Chat & Tour triggers
  useEffect(() => {
    const handleOpenChat = () => {
      setIsChatOpen(true);
      setIsChatMinimized(false);
    };

    const handleStartTour = () => {
      setIsHelpOpen(false);
      setIsWelcomeOpen(false);
      setIsTourOpen(true);
    };

    const handleOpenHelp = () => {
      setIsHelpOpen(true);
    };

    const handleOpenWelcome = () => {
      setIsWelcomeOpen(true);
    };

    window.addEventListener('open_fin_chat', handleOpenChat);
    window.addEventListener('open-ai-assistant', handleOpenChat);
    window.addEventListener('start_guided_tour', handleStartTour);
    window.addEventListener('open_help_panel', handleOpenHelp);
    window.addEventListener('open_welcome_onboarding', handleOpenWelcome);

    return () => {
      window.removeEventListener('open_fin_chat', handleOpenChat);
      window.removeEventListener('open-ai-assistant', handleOpenChat);
      window.removeEventListener('start_guided_tour', handleStartTour);
      window.removeEventListener('open_help_panel', handleOpenHelp);
      window.removeEventListener('open_welcome_onboarding', handleOpenWelcome);
    };
  }, []);

  return (
    <div className="app-shell">
      <SessionTimeoutManager />
      <Sidebar isOpen={isSidebarOpen} onClose={closeSidebar} />
      <div className="app-main-canvas">
        <Header
          onToggleSidebar={toggleSidebar}
          onToggleChat={toggleChat}
          onOpenChat={toggleChat}
          isChatOpen={isChatOpen}
          onToggleHelp={toggleHelp}
          isHelpOpen={isHelpOpen}
        />
        <Outlet />
        <Footer />
      </div>

      {/* Floating Help Button on Mobile/Responsive */}
      <HelpButton
        isFloating={true}
        onClick={toggleHelp}
        isOpen={isHelpOpen}
        className="app-floating-help"
      />

      {/* Slide-over Contextual Help Panel */}
      <HelpPanel
        isOpen={isHelpOpen}
        onClose={() => setIsHelpOpen(false)}
        onStartTour={() => {
          setIsHelpOpen(false);
          setIsTourOpen(true);
        }}
      />

      {/* First-login Welcome & Onboarding Modal */}
      <WelcomeOnboardingModal
        isOpen={isWelcomeOpen}
        onStartTour={() => {
          setIsWelcomeOpen(false);
          setIsTourOpen(true);
        }}
        onDismiss={() => setIsWelcomeOpen(false)}
      />

      {/* Multi-step Interactive Guided Onboarding Tour */}
      <GuidedTour
        isOpen={isTourOpen}
        onClose={() => setIsTourOpen(false)}
        onFinish={() => setIsTourOpen(false)}
      />

      {/* FIN Assistant (AI) Pop-up Modal */}
      <FinAssistantModal
        isOpen={isChatOpen}
        isMinimized={isChatMinimized}
        onClose={closeChat}
        onMinimize={minimizeChat}
        onToggleMinimize={minimizeChat}
      />
    </div>
  );
}
