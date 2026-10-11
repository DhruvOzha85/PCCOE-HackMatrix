-- ==============================================================================
-- FIN Platform - Database Migration 001
-- Tables: scheme_faqs, evidence_verification_reports, chat_conversations, notifications
-- Purpose: Complete missing schema tables and provide database persistence for state
-- Target: PostgreSQL 15+ / Supabase
-- Execution: Run in Supabase SQL Editor by an authorized database owner.
-- ==============================================================================

-- 1. SCHEME FAQs TABLE
-- Provides storage for scheme-specific frequently asked questions
CREATE TABLE IF NOT EXISTS public.scheme_faqs (
    id TEXT PRIMARY KEY,
    scheme_slug TEXT NOT NULL,
    faq_number INTEGER NOT NULL DEFAULT 1,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    source_dataset TEXT,
    source_authority TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_scheme_faqs_slug ON public.scheme_faqs (scheme_slug);
CREATE INDEX IF NOT EXISTS idx_scheme_faqs_slug_num ON public.scheme_faqs (scheme_slug, faq_number);

-- 2. EVIDENCE VERIFICATION REPORTS TABLE
-- Stores deterministic policy-to-evidence verification reports
CREATE TABLE IF NOT EXISTS public.evidence_verification_reports (
    id TEXT PRIMARY KEY,
    user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
    application_id TEXT,
    scheme_id TEXT,
    overall_status TEXT NOT NULL DEFAULT 'INSUFFICIENT',
    report_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    verified_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_ev_reports_user ON public.evidence_verification_reports (user_id);
CREATE INDEX IF NOT EXISTS idx_ev_reports_app ON public.evidence_verification_reports (application_id);
CREATE INDEX IF NOT EXISTS idx_ev_reports_scheme ON public.evidence_verification_reports (scheme_id);

-- 3. PERSISTENT CHAT CONVERSATIONS TABLE
-- Replaces ephemeral local JSON file storage for chat history
CREATE TABLE IF NOT EXISTS public.chat_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL DEFAULT 'New Conversation',
    messages JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now()),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_chat_conversations_user ON public.chat_conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_chat_conversations_updated ON public.chat_conversations (updated_at DESC);

-- 4. PERSISTENT NOTIFICATIONS TABLE
-- Replaces ephemeral local JSON file storage for notifications
CREATE TABLE IF NOT EXISTS public.notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    message TEXT NOT NULL,
    type TEXT NOT NULL DEFAULT 'INFO',
    read BOOLEAN NOT NULL DEFAULT false,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT timezone('utc'::text, now())
);

CREATE INDEX IF NOT EXISTS idx_notifications_recipient ON public.notifications (recipient_user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON public.notifications (recipient_user_id, read);

-- Enable Row Level Security (RLS) on user-specific tables
ALTER TABLE public.evidence_verification_reports ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chat_conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notifications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.scheme_faqs ENABLE ROW LEVEL SECURITY;

-- Public read access for FAQs
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE tablename = 'scheme_faqs' AND policyname = 'Allow public read access for scheme FAQs'
    ) THEN
        CREATE POLICY "Allow public read access for scheme FAQs"
            ON public.scheme_faqs FOR SELECT
            USING (true);
    END IF;
END $$;
