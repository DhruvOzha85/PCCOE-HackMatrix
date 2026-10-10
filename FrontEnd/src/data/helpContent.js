import React from 'react';

/**
 * Renders inline text safely converting any **bold** syntax to <strong> elements.
 * Prevents raw asterisks from ever appearing in the UI.
 */
export function formatHelpText(text) {
  if (!text || typeof text !== 'string') return text;
  const parts = text.split(/(\*\*[^*]+\*\*)/g);
  return parts.map((part, index) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return React.createElement(
        'strong',
        { key: index, className: 'help-text-bold' },
        part.slice(2, -2)
      );
    }
    return part;
  });
}

/**
 * Help Center Content Registry in simple, friendly, easy-to-understand language.
 */
export const HELP_CATEGORIES = [
  {
    id: 'getting-started',
    title: 'Getting Started',
    icon: 'Compass',
    description: 'Create your account, understand how FIN works, and explore your dashboard.',
    audience: 'all',
    articles: [
      {
        id: 'gs-account-creation',
        title: 'Creating an Account & Roles',
        summary: 'Learn about Citizen and Government Officer account types.',
        content: `FIN is designed for two types of users:

- Citizen (Applicant): Look for government schemes, upload and store certificates, test your eligibility, and submit direct applications.
- Review Officer: For authorized government staff to review submitted documents, verify certificates, and approve scheme applications.

Simply sign up with your email and password to begin exploring benefits you may qualify for.`
      },
      {
        id: 'gs-profile-setup',
        title: 'Filling in Your Profile',
        summary: 'Why your age, state, category, and income are important.',
        content: `Your profile helps FIN find the exact schemes you qualify for:

- Full Name & Date of Birth: Determines if you qualify for age-based benefits, like youth scholarships or senior citizen pensions.
- State and District: Helps show schemes for your home state along with Central Government policies.
- Social Category (General, OBC, SC, ST): Checks for statutory quotas and special welfare programs.
- Annual Family Income: Checks if your family meets income limits (for example, below ₹1.5 Lakh or ₹2.5 Lakh).
- Occupation: Finds schemes for your profession, like farmers, construction workers, students, or artisans.

Tip: Keeping your details accurate and matching your official certificates gives you the most reliable results.`
      },
      {
        id: 'gs-dashboard-navigation',
        title: 'Using Your Dashboard',
        summary: 'A quick tour of your home page and overview cards.',
        content: `Your Dashboard (/dashboard) is your home base in FIN:

1. Welcome Banner: Shows a greeting and quick orientation.
2. Metric Counters: Quickly see how many schemes you qualify for, total potential benefit money, verified documents in your vault, and your active applications.
3. Top Opportunities: Highlights the best schemes for you right now, with clear status badges like Eligible or Review Required.
4. Profile Completion Card: Tells you what information is still missing so you can unlock more scheme recommendations.`
      }
    ]
  },
  {
    id: 'finding-schemes',
    title: 'Finding Government Schemes',
    icon: 'Search',
    description: 'How to search, filter by state or department, and view scheme benefits.',
    audience: 'all',
    articles: [
      {
        id: 'fs-catalog-search',
        title: 'Searching & Filtering Schemes',
        summary: 'Find policies quickly using keywords and dropdown filters.',
        content: `The Discover Schemes page (/discover) lets you search hundreds of official welfare policies:

- Keyword Search: Type any scheme name or keyword, such as "housing", "pension", "scholarship", or "farm".
- State Filter: Select your state to see local state schemes, or choose All India to view Central Government programs.
- Department Filter: Filter by department, such as Agriculture, Education, Health, or Women & Child Development.
- Beneficiary Type: View schemes targeted for Students, Women, Farmers, Senior Citizens, or Small Businesses.`
      },
      {
        id: 'fs-suggested-schemes',
        title: 'Personalized Scheme Suggestions',
        summary: 'How we match schemes to your personal profile.',
        content: `The Suggested Schemes page (/suggested-schemes) matches policies to your verified profile:

- Best Matches: Schemes are ranked by how well they match your age, state, category, and occupation.
- Document Ready Labels: See whether you already have all the needed certificates verified in your vault (marked "Fully Document Ready") or if you still need one (marked "1 Document Needed").
- Click any scheme to read its criteria or test your document readiness.`
      },
      {
        id: 'fs-understanding-benefits',
        title: 'Understanding Policy Benefits',
        summary: 'How money, pensions, and free services are calculated.',
        content: `Every scheme clearly shows the official benefits available under the policy:

- Direct Monthly Pensions: For example, ₹1,500 per month under widow or senior citizen pensions.
- One-Time Grants: Financial assistance for buying farming tools, setting up a business, or building a home.
- Non-Cash Benefits: Such as free electricity connections, school uniforms, or health insurance coverage.

Please note: Benefit amounts are the statutory ceilings set by government rules. Final payment amounts are confirmed by the department upon official approval.`
      }
    ]
  },
  {
    id: 'eligibility-fin03',
    title: 'Eligibility Analysis (FIN-03)',
    icon: 'ShieldCheck',
    description: 'Learn why you qualify or what is missing, and test changes with the simulator.',
    audience: 'all',
    articles: [
      {
        id: 'el-four-classifications',
        title: 'The Four Eligibility Results Explained',
        summary: 'What Satisfied, Unmet, Missing Evidence, and Conflicting Evidence mean.',
        content: `When you check a scheme, FIN explains each condition in one of four simple categories:

1. Satisfied Conditions (Green):
You meet this condition based on your profile and verified certificates.

2. Unmet Conditions (Red):
You do not meet this rule. For example, your annual family income is ₹3,00,000, but the scheme limit is ₹1,50,000.

3. Missing Evidence (Amber):
You might be eligible, but we do not have your certificate in the vault yet (for example, an Income Certificate).
Important: Missing Evidence is not a rejection! It just means you need to upload proof before we can confirm.

4. Conflicting Evidence (Orange):
Your profile says one thing, but your certificate shows something else (for example, profile says Gujarat, but document shows Rajasthan). Update your profile or upload the correct document to match.`
      },
      {
        id: 'el-actionability-matrix',
        title: 'What Can You Do Next? (Action Types)',
        summary: 'Who controls the next step and how to resolve blockers.',
        content: `Every issue tells you who can fix it and what step to take:

- You Can Fix This (Applicant-Controlled): Steps you can take right now, such as uploading a missing Income Certificate or fixing a typo in your profile age.
- Waiting for Department (Authority-Controlled): Steps waiting on an external official, like a Tehsildar approving a certificate or a bank confirming account details.
- Hard Statutory Limits (Immutable): Rules set strictly by law that cannot be changed just by uploading documents (for example, you are older than the maximum age limit).`
      },
      {
        id: 'el-what-if-simulator',
        title: 'The What-If Simulator',
        summary: 'Test hypothetical changes safely without altering your real profile.',
        content: `On any Scheme Details page, you will find the What-If Simulator:

- Try changing your income, age, or category hypothetically to see how it affects your eligibility.
- See immediately which conditions would pass or fail.
- Completely Safe: Trying different numbers here does not change your real profile or your submitted applications.`
      }
    ]
  },
  {
    id: 'document-vault',
    title: 'Document Vault & AI OCR',
    icon: 'FileText',
    description: 'Upload documents, view AI-extracted facts, and check verification statuses.',
    audience: 'all',
    articles: [
      {
        id: 'dv-supported-types',
        title: 'Supported Files & Certificates',
        summary: 'Formats, file sizes, and accepted documents.',
        content: `The Document Vault (/documents) keeps your certificates safe in one place:

- Supported File Types: PDF, JPG, JPEG, and PNG.
- Maximum File Size: Up to 10 MB per document.
- Accepted Documents:
  - Aadhaar Card / National ID (Identity and Age proof)
  - PAN Card (Financial identification)
  - Income Certificate (Income limit proof)
  - Caste Certificate (SC, ST, OBC reservation proof)
  - Domicile Certificate (State residency proof)
  - Ration Card / BPL Card (Economic welfare proof)
  - Bank Passbook or Cancelled Cheque (Bank account for payments)
  - Disability or Special Category Certificate (Targeted programs)`
      },
      {
        id: 'dv-ocr-extraction',
        title: 'How AI Reads Your Documents (OCR)',
        summary: 'Automated fact extraction from your uploads.',
        content: `When you upload a document, our vision system automatically reads the text:

1. Text Extraction: Scans the document to find your name, date of birth, income number, and state.
2. Fact Summary: Click the Eye icon on any document to see the structured facts we extracted.
3. Important Note: AI fact extraction helps check your document readiness, but it is not the same as legal government verification. A reviewing officer confirms the document during official review.`
      },
      {
        id: 'dv-verification-statuses',
        title: 'Document Verification Statuses',
        summary: 'What Under Review, Verified, and Action Required mean.',
        content: `Your documents show one of three statuses:

- Under Review: Uploaded and read by AI; waiting for desk verification by an officer.
- Verified (Green): Checked and approved by an officer. This document is officially valid and ready for scheme applications.
- Action Required (Amber): The image was blurry, expired, or had mismatched information. Check the officer remarks and upload a fresh copy.`
      },
      {
        id: 'dv-dossier-export',
        title: 'Downloading Your Digital Dossier',
        summary: 'Download all your verified documents in one clean PDF.',
        content: `You can download a single compiled PDF of all your verified certificates.
Click the Export Dossier button on the My Documents page to get a clean package with timestamps and summary sheets, perfect for your personal records.`
      }
    ]
  },
  {
    id: 'scheme-readiness',
    title: 'Scheme Readiness & Applying',
    icon: 'CheckCircle2',
    description: 'Check if you have all required documents, and apply in one click.',
    audience: 'all',
    articles: [
      {
        id: 'sr-readiness-checker',
        title: 'Checking Your Document Readiness',
        summary: 'Make sure your vault has all mandatory certificates before applying.',
        content: `The Scheme Readiness Checker (on the right side of /documents) tests your vault against any scheme:

- Mandatory Requirements: Documents you must have (like Aadhaar and Income Certificate).
- Conditional Requirements: Documents needed only in special cases (like a spouse death certificate for a widow pension).
- Readiness Verdict: Shows if you have all required documents verified, uploaded, or missing.
- No Guesswork: Requirements come directly from authentic government rules.`
      },
      {
        id: 'sr-direct-application-flow',
        title: 'One-Click Scheme Application',
        summary: 'How pre-selected schemes lock in with verified documents.',
        content: `Once your document readiness is complete:

1. Click Apply for [Scheme Name].
2. The application window opens with that scheme already selected and locked in.
3. All verified documents from your vault are linked automatically—you do not have to upload them again!
4. Check your details and click Submit Application.
5. You will receive an official Application ID (like APP-F78912A0) to track your progress.`
      }
    ]
  },
  {
    id: 'tracking-applications',
    title: 'Tracking Applications',
    icon: 'Inbox',
    description: 'Application ID, review stages, officer comments, and re-submitting files.',
    audience: 'all',
    articles: [
      {
        id: 'ta-status-pipeline',
        title: 'The Application Status Stages',
        summary: 'What Draft, Under Review, Action Required, Approved, and Rejected mean.',
        content: `Every application moves through clear statutory stages:

- Draft: Started but not yet submitted.
- Under Review: Received by the department desk; an officer is reviewing your credentials.
- Action Required: An officer found a problem (like an unclear document scan). You will see a note explaining what to fix.
- Approved: Everything is verified and approved. Benefit disbursement is scheduled.
- Rejected: Did not meet official requirements. The official reason is recorded on your application card.`
      },
      {
        id: 'ta-responding-to-remarks',
        title: 'Fixing Issues & Re-Submitting',
        summary: 'How to answer officer remarks without starting over.',
        content: `If your application shows Action Required:

1. Go to Applications (/applications) and open the card to read the Reviewer Remarks.
2. Go to My Documents (/documents) and upload a clear, updated copy of the requested document.
3. Re-submit your application with the new document attached. The officer is automatically notified to review it again.`
      }
    ]
  },
  {
    id: 'benefits-payments',
    title: 'Benefits & Payments',
    icon: 'Banknote',
    description: 'Pensions, subsidies, Direct Benefit Transfer (DBT), and bank linking.',
    audience: 'all',
    articles: [
      {
        id: 'bp-dbt-guidelines',
        title: 'Direct Benefit Transfer (DBT) & Bank Accounts',
        summary: 'Make sure your bank account receives government payments.',
        content: `Most government cash transfers are sent through Direct Benefit Transfer (DBT):

- Make sure your bank account is linked with your Aadhaar number.
- Ensure your bank account is active and set up to receive DBT payments.
- Upload a copy of your Bank Passbook or Cancelled Cheque to your vault so your details are verified.`
      },
      {
        id: 'bp-benefit-disclaimers',
        title: 'Important Note on Benefit Estimates',
        summary: 'Why displayed numbers are official policy maximums.',
        content: `All benefit numbers shown in FIN (for example, "₹1,500/month" or "Up to ₹50,000") are official policy maximums published in government guidelines.

Actual payment depends on final department approval and budget clearance. FIN helps you discover schemes and apply easily, but the government department makes the final decision.`
      }
    ]
  },
  {
    id: 'profile-accuracy',
    title: 'Profile & Data Accuracy',
    icon: 'User',
    description: 'How your profile details affect your eligibility and matching.',
    audience: 'all',
    articles: [
      {
        id: 'pa-fields-and-impact',
        title: 'How Profile Fields Change Your Results',
        summary: 'Why age, income, state, and category matter.',
        content: `Your profile attributes directly control scheme matching:

- Date of Birth: Determines age limits (students, youth, senior citizens).
- Annual Family Income: Checks income cutoffs for financial assistance.
- State & District: Shows schemes open to residents of your region.
- Social Category: Matches affirmative welfare programs.
- Occupation: Finds schemes for your line of work (farming, construction, artisans).

You can update your profile anytime on the Profile page (/profile).`
      }
    ]
  },
  {
    id: 'reviewer-guide',
    title: 'Government Reviewer Guide',
    icon: 'ClipboardCheck',
    description: 'For authorized review officers: review queues, document checks, and decisions.',
    audience: 'reviewer',
    articles: [
      {
        id: 'rg-reviewer-dashboard',
        title: 'Reviewer Dashboard Overview',
        summary: 'View pending application queues and review statistics.',
        content: `The Reviewer Dashboard (/reviewer/dashboard) gives authorized officers:

- Pending Reviews Count: Total applications waiting for review.
- Verified Documents: Total certificates inspected and verified.
- Department Workload: Tracking response times and application progress.`
      },
      {
        id: 'rg-verifying-documents',
        title: 'Document Inspection & Verification Flow',
        summary: 'Compare uploaded scans with AI facts and verify.',
        content: `On the Review Applications page (/reviewer/applications):

1. Click any applicant to open their dossier.
2. Inspect original document scans side-by-side with AI-extracted facts.
3. Confirm details match the applicant's official records.
4. Click Verify Document to confirm legal validity. The citizen vault is updated immediately.`
      },
      {
        id: 'rg-application-decisions',
        title: 'Approving, Rejecting, or Requesting Corrections',
        summary: 'Record official determinations and feedback.',
        content: `Review officers have three choices:

- Approve: Mark the application approved when all conditions and documents are verified.
- Request Action: Ask the citizen to re-upload if a document is blurry or expired.
- Reject: If statutory conditions are not met, record the official reason in the remarks field for the record.`
      }
    ]
  },
  {
    id: 'faqs',
    title: 'Frequently Asked Questions',
    icon: 'HelpCircle',
    description: 'Quick answers to common questions about eligibility, documents, and tours.',
    audience: 'all',
    articles: [
      {
        id: 'faq-missing-evidence',
        title: 'Why does my eligibility status say "Missing Evidence"?',
        summary: 'Missing documentation is not a rejection.',
        content: `Missing Evidence is not a rejection. It simply means that while you might qualify, you have not uploaded the required proof (like an Income Certificate or Domicile Certificate) yet. As soon as you upload the certificate to your vault, the system will re-check your status.`
      },
      {
        id: 'faq-conflicting-income',
        title: 'Why does my profile income differ from my certificate income?',
        summary: 'How to fix conflicting evidence warnings.',
        content: `If you declared ₹1,00,000 in your profile, but your uploaded Income Certificate shows ₹1,80,000, the system flags a Conflicting Evidence condition. To fix this, simply update your profile income to match your certificate, or upload your latest valid certificate.`
      },
      {
        id: 'faq-scheme-not-found',
        title: 'Why can I not find a particular scheme in the catalog?',
        summary: 'How to check state and department filters.',
        content: `Make sure your State filter matches the scheme's region (for example, choose Himachal Pradesh for HP BOCW schemes, or Gujarat for local state schemes). Or select All India to view national Central Government programs.`
      },
      {
        id: 'faq-ocr-vs-verification',
        title: 'Does uploading a document mean it is officially verified?',
        summary: 'Understanding AI extraction vs. official verification.',
        content: `No. When you upload a document, our AI reads the text to help check readiness quickly. However, formal legal verification requires a government officer to inspect and approve it. Look for the green Verified badge in your vault.`
      },
      {
        id: 'faq-guaranteed-approval',
        title: 'Does a 100% Document Readiness score guarantee approval?',
        summary: 'Readiness prepares your application for smooth processing.',
        content: `Document Readiness means your vault has all certificates required to submit your application without delays. Final sanctioning is decided by the administering government department upon official review.`
      },
      {
        id: 'faq-restart-tour',
        title: 'How do I restart the Guided Onboarding Tour?',
        summary: 'Start the step-by-step walkthrough anytime.',
        content: `You can restart the tour anytime by clicking Start Guided Tour at the top of the Help Center (/help), or by opening the Help panel (?) from the top navigation bar.`
      }
    ]
  }
];

/**
 * Route-specific contextual help mappings for the permanent Help (?) button.
 */
export const ROUTE_HELP_MAP = {
  '/dashboard': {
    title: 'Dashboard Guidance',
    badge: 'Home Overview',
    summary: 'Your home base for checking potential benefits, document readiness, and top opportunities.',
    tips: [
      'Look at your Top Opportunities cards for schemes that match your verified profile.',
      'Keep your profile completion high to unlock accurate recommendations.',
      'Click on any metric card to jump directly to schemes, documents, or applications.'
    ],
    relatedArticles: ['gs-dashboard-navigation', 'gs-profile-setup', 'fs-suggested-schemes'],
    recommendedTourStep: 0
  },
  '/discover': {
    title: 'Scheme Discovery Guide',
    badge: 'Catalog Search',
    summary: 'Search and filter hundreds of Central and State government welfare schemes.',
    tips: [
      'Use the State dropdown to see schemes for your home state alongside Central policies.',
      'Filter by Department (Agriculture, Education, Women & Child Development) to narrow results.',
      'Click any scheme card to see detailed rules and test the What-If simulator.'
    ],
    relatedArticles: ['fs-catalog-search', 'fs-understanding-benefits', 'el-four-classifications'],
    recommendedTourStep: 2
  },
  '/suggested-schemes': {
    title: 'Suggested Schemes Guidance',
    badge: 'Personalized Matches',
    summary: 'Schemes matched to your age, state, category, and uploaded certificates.',
    tips: [
      'Schemes marked "Fully Document Ready" have all required documents verified in your vault.',
      'Look at match scores to see which programs provide the highest benefit impact.',
      'Click "Check Readiness" to evaluate your vault before applying.'
    ],
    relatedArticles: ['fs-suggested-schemes', 'sr-readiness-checker', 'dv-verification-statuses'],
    recommendedTourStep: 2
  },
  '/documents': {
    title: 'Document Vault & Readiness Checker',
    badge: 'Digital Vault',
    summary: 'Upload documents, inspect AI-extracted facts, and evaluate readiness before applying.',
    tips: [
      'Click the Eye icon on any document to inspect the extracted facts and vision results.',
      'Use the Scheme Readiness Checker on the right to test your vault against any government scheme.',
      'When readiness is complete, click "Apply for [Scheme]" to submit with pre-selected details.',
      'Click "Export Dossier" to download a clean PDF summary of your verified records.'
    ],
    relatedArticles: ['dv-supported-types', 'dv-ocr-extraction', 'dv-verification-statuses', 'sr-readiness-checker', 'sr-direct-application-flow'],
    recommendedTourStep: 4
  },
  '/applications': {
    title: 'Application Tracking Desk',
    badge: 'Status Pipeline',
    summary: 'Track the status of all your submitted government scheme applications in real time.',
    tips: [
      'Every application has a unique ID (like APP-XXXXXX) for official tracking.',
      'If status says "Action Required", open the card to see officer remarks and upload a fresh copy.',
      'Approved applications show the sanctioned benefit and Direct Benefit Transfer (DBT) details.'
    ],
    relatedArticles: ['ta-status-pipeline', 'ta-responding-to-remarks', 'bp-dbt-guidelines'],
    recommendedTourStep: 6
  },
  '/profile': {
    title: 'Citizen Profile Guidance',
    badge: 'Identity & Details',
    summary: 'Manage your demographic, category, and income details that power scheme matching.',
    tips: [
      'Keep your Annual Family Income updated to match your latest Income Certificate.',
      'State and district selections control which state-specific schemes appear in your catalog.',
      'Category (SC, ST, OBC, General) determines applicable welfare reservations.'
    ],
    relatedArticles: ['gs-profile-setup', 'pa-fields-and-impact', 'faq-conflicting-income'],
    recommendedTourStep: 1
  },
  '/reviewer/dashboard': {
    title: 'Reviewer Operations Help',
    badge: 'Officer Dashboard',
    summary: 'Overview of department application queues, pending reviews, and processing metrics.',
    tips: [
      'Review pending application totals to monitor incoming desk caseloads.',
      'Monitor verification throughput to maintain compliance with citizen service standards.'
    ],
    relatedArticles: ['rg-reviewer-dashboard', 'rg-verifying-documents'],
    recommendedTourStep: 0
  },
  '/reviewer/applications': {
    title: 'Application Review Terminal',
    badge: 'Verification Desk',
    summary: 'Inspect applicant dossiers, verify uploaded documents, and issue official decisions.',
    tips: [
      'Compare original document scans side-by-side with OCR-extracted facts.',
      'Click "Verify Document" to confirm individual certificate validity.',
      'When rejecting an application, record the clear statutory reason in the remarks field.'
    ],
    relatedArticles: ['rg-verifying-documents', 'rg-application-decisions'],
    recommendedTourStep: 6
  }
};

/**
 * Returns contextual help content for a given URL path.
 */
export function getContextualHelpForRoute(pathname = '') {
  if (pathname.includes('/schemes/') && pathname.includes('/benefits')) {
    return {
      title: 'Scheme Benefits Breakdown',
      badge: 'Policy Financials',
      summary: 'Explore monthly pensions, one-time grants, and bank account linking guidelines.',
      tips: [
        'Review installment schedules to understand recurring vs one-time cash grants.',
        'Ensure your bank account is linked with Aadhaar for Direct Benefit Transfer (DBT).',
        'Displayed figures represent statutory policy maximums set by government rules.'
      ],
      relatedArticles: ['fs-understanding-benefits', 'bp-dbt-guidelines', 'bp-benefit-disclaimers'],
      recommendedTourStep: 3
    };
  }

  if (pathname.startsWith('/schemes/') || pathname.startsWith('/scheme/')) {
    return {
      title: 'Scheme Eligibility & What-If Simulator',
      badge: 'Policy Details',
      summary: 'Inspect conditions, understand missing evidence, and test profile changes safely.',
      tips: [
        'Review the Eligibility Change card to see why conditions pass or fail.',
        'Use the What-If Simulator to test hypothetical income or age numbers safely.',
        'Click "Check Document Readiness in Vault" to evaluate your certificates against this policy.'
      ],
      relatedArticles: ['el-four-classifications', 'el-actionability-matrix', 'el-what-if-simulator', 'sr-readiness-checker'],
      recommendedTourStep: 3
    };
  }

  const cleanPath = pathname.split('?')[0].replace(/\/$/, '') || '/dashboard';
  return ROUTE_HELP_MAP[cleanPath] || ROUTE_HELP_MAP['/dashboard'];
}
