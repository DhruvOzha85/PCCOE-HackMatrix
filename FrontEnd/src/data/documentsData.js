/**
 * Documents Data Model
 * Enhanced for PCCOE HackMatrix Citizen Portal
 * Clean state: zero dummy/mock documents or tickets.
 */

export const INITIAL_DOCUMENTS = [];

export const SUPPORTED_DOC_TYPES = [
  { id: 'aadhaar', name: 'Aadhaar Card', category: 'Identity Verification', purpose: 'Identity Proof', iconType: 'id-card', iconColor: 'blue' },
  { id: 'pan', name: 'PAN Card', category: 'Tax Identification', purpose: 'Financial Transactions', iconType: 'credit-card', iconColor: 'purple' },
  { id: 'income_cert', name: 'Income Certificate', category: 'Issued by State Government', purpose: 'Income Proof', iconType: 'certificate', iconColor: 'green' },
  { id: 'caste_cert', name: 'Caste Certificate', category: 'Issued by Competent Authority', purpose: 'Category Benefit', iconType: 'scroll', iconColor: 'amber' },
  { id: 'domicile', name: 'Domicile Certificate', category: 'State of Residence Proof', purpose: 'State Eligibility', iconType: 'home', iconColor: 'rose' },
  { id: 'bank_passbook', name: 'Bank Passbook / Details', category: 'Cancelled Cheque / Passbook', purpose: 'Direct Benefit Transfer', iconType: 'bank', iconColor: 'blue' },
  { id: 'address_proof', name: 'Address Proof', category: 'Residency Verification', purpose: 'Address Proof', iconType: 'map-pin', iconColor: 'cyan' },
  { id: 'udyam', name: 'Udyam Registration', category: 'MSME Verification', purpose: 'Business Eligibility', iconType: 'award', iconColor: 'purple' },
  { id: 'itr', name: 'Income Tax Return (ITR)', category: 'Financial Verification', purpose: 'Income Verification', iconType: 'file-text', iconColor: 'green' },
  { id: 'land', name: 'Land Records (7/12 Extract / 8-A)', category: 'Revenue Department', purpose: 'Agriculture & Farm Proof', iconType: 'home', iconColor: 'amber' },
  { id: 'photo', name: 'Passport Size Photo', category: 'Visual Verification', purpose: 'Identity Verification', iconType: 'image', iconColor: 'rose' },
  { id: 'disability_cert', name: 'Disability Certificate', category: 'Medical Board', purpose: 'Affirmative Action', iconType: 'scroll', iconColor: 'blue' },
  { id: 'labour_card', name: 'Labour Card / BOCW Registration', category: 'Labour Welfare Board', purpose: 'Worker Eligibility Proof', iconType: 'award', iconColor: 'amber' },
  { id: 'widow_cert', name: 'Widow Certificate', category: 'Revenue Department', purpose: 'Marital Status Benefit', iconType: 'scroll', iconColor: 'rose' },
  { id: 'death_cert', name: 'Death Certificate of Worker', category: 'Department of Births & Deaths', purpose: 'Demise & Dependent Benefit Proof', iconType: 'file-text', iconColor: 'purple' },
  { id: 'age_proof', name: 'Age Proof / Birth Certificate', category: 'Competent Authority', purpose: 'Age Eligibility Proof', iconType: 'id-card', iconColor: 'blue' },
  { id: 'consent_letter', name: 'Consent Letter / NOC', category: 'Land / Joint Holder Declaration', purpose: 'Joint Account / Land Consent', iconType: 'scroll', iconColor: 'cyan' },
  { id: 'forest_rights', name: 'Van Adhikar Patra / Forest Sanad', category: 'Forest Revenue Department', purpose: 'Forest Area Farmer Eligibility', iconType: 'scroll', iconColor: 'green' },
  { id: 'other_doc', name: 'Any Other Supporting Document', category: 'Optional Verification Proof', purpose: 'Conditional / Supplementary Evidence', iconType: 'file-text', iconColor: 'blue' }
];

export const DOC_TYPE_LOOKUP = Object.fromEntries(SUPPORTED_DOC_TYPES.map(d => [d.id, d]));

export const SCHEMES_CHECKLIST = [
  {
    id: 'pmegp',
    name: 'PMEGP (Prime Minister Employment Generation)',
    benefit: 'Credit-Linked Margin Money Subsidy',
    requiredDocIds: ['aadhaar', 'pan', 'caste_cert', 'address_proof'],
  },
  {
    id: 'pm-kisan',
    name: 'PM Kisan Samman Nidhi',
    benefit: '₹ 6,000 / year (Direct Income Support)',
    requiredDocIds: ['aadhaar', 'bank_passbook', 'land', 'domicile'],
  },
  {
    id: 'msme',
    name: 'MSME Credit & Financial Support',
    benefit: 'Credit Guarantee & Term Support',
    requiredDocIds: ['aadhaar', 'pan', 'bank_passbook', 'address_proof'],
  },
  {
    id: 'scholarship',
    name: 'National Higher Education Scholarship',
    benefit: 'Direct Financial Assistance',
    requiredDocIds: ['aadhaar', 'income_cert', 'domicile', 'photo'],
  },
];

export const STORAGE_DOCUMENTS_KEY = 'fin_documents_data';
export const STORAGE_TICKETS_KEY = 'fin_support_tickets_data';
export const STORAGE_APPLICATIONS_KEY = 'fin_associated_applications_data';

export const INITIAL_TICKETS = [];

/**
 * Normalizes statutory requirement identifiers to standard vault document types
 */
export function normalizeDocRequirementId(rawId) {
  if (!rawId) return 'address_proof';
  const rawStr = String(rawId).toLowerCase().trim();
  const clean = rawStr.replace(/[^a-z0-9_]/g, '_');

  if (clean.includes('caste') || clean.includes('jati')) return 'caste_cert';
  if (clean.includes('labour') || clean.includes('labor') || clean.includes('bocw') || clean.includes('hpbocwwb') || clean.includes('worker_card')) return 'labour_card';
  if (clean.includes('widow')) return 'widow_cert';
  if (clean.includes('death')) return 'death_cert';
  if (clean.includes('age') || clean.includes('birth') || clean.includes('dob')) return 'age_proof';
  if (clean.includes('consent') || clean.includes('noc') || clean.includes('undertaking')) return 'consent_letter';
  if (clean.includes('van_adhikar') || clean.includes('sanad') || clean.includes('forest')) return 'forest_rights';
  if (clean.includes('land') || clean.includes('712') || clean.includes('8a') || clean.includes('8_a') || clean.includes('ror') || clean.includes('khatauni') || clean.includes('fencing')) return 'land';
  if (clean.includes('address') || clean.includes('residence') || clean.includes('domicile')) return clean.includes('domicile') ? 'domicile' : 'address_proof';
  if (clean.includes('bank') || clean.includes('passbook') || clean.includes('account')) return 'bank_passbook';
  if (clean.includes('income') || clean.includes('salary') || clean.includes('aavak') || clean.includes('aay')) return 'income_cert';
  if (clean.includes('photo') || clean.includes('picture')) return 'photo';
  if (clean.includes('aadhaar') || clean.includes('aadhar') || clean.includes('uidai')) return 'aadhaar';
  if (clean.includes('pan')) return 'pan';
  if (clean.includes('udyam') || clean.includes('msme')) return 'udyam';
  if (clean.includes('itr') || clean.includes('tax')) return 'itr';
  if (clean.includes('disability') || clean.includes('handicap')) return 'disability_cert';
  if (clean.includes('other') || clean.includes('optional') || clean.includes('if_required') || clean.includes('if_applicable')) return 'other_doc';
  return clean;
}

/**
 * Determines whether a statutory document requirement is conditional or optional.
 * Conditional/optional documents do NOT block applicant scheme readiness.
 */
export function isOptionalRequirement(rawIdOrLabel) {
  if (!rawIdOrLabel) return false;
  const str = String(rawIdOrLabel).toLowerCase().trim();
  return str.includes('(if applicable)') ||
         str.includes('if applicable') ||
         str.includes('(if required)') ||
         str.includes('if required') ||
         str.includes('(optional)') ||
         str.includes('optional') ||
         str.includes('forest areas') ||
         str.includes('any other') ||
         str.includes('other document');
}

/**
 * Dynamically builds scheme checklist from canonical scheme records
 */
export function buildDynamicSchemeChecklist(schemesList = []) {
  if (!Array.isArray(schemesList) || schemesList.length === 0) {
    return SCHEMES_CHECKLIST;
  }
  return schemesList.map(s => {
    const rawDocs = s.documents_required || s.required_documents || s.documents || [];
    let parsedDocs = [];
    if (Array.isArray(rawDocs)) {
      parsedDocs = rawDocs;
    } else if (typeof rawDocs === 'string') {
      parsedDocs = rawDocs.split(/[;\n|]+/).map(t => t.trim()).filter(Boolean);
    }

    let requiredDocIds = [];
    if (parsedDocs.length > 0) {
      requiredDocIds = parsedDocs.map(d => ({
        id: normalizeDocRequirementId(d),
        label: d
      }));
    } else {
      requiredDocIds = [];
    }

    return {
      id: s.id || s.scheme_id,
      slug: s.slug || s.scheme_slug || null,
      name: s.name || s.scheme_name || s.title || 'Government Scheme',
      state: s.state || null,
      benefit: s.benefit_display || s.benefit_summary || (s.max_benefit ? `Up to ₹${Number(s.max_benefit).toLocaleString('en-IN')}` : 'Statutory Guidelines'),
      requiredDocIds
    };
  });
}

function getStorageKey(baseKey, userId) {
  return userId ? `${baseKey}_${userId}` : baseKey;
}

export function loadTicketsFromStorage(userId = null) {
  if (typeof window === 'undefined' || !window.localStorage) return [];
  try {
    const key = getStorageKey(STORAGE_APPLICATIONS_KEY, userId);
    let raw = localStorage.getItem(key);
    if (!raw && !userId) raw = localStorage.getItem(STORAGE_TICKETS_KEY);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch (e) {
    console.error('Failed to load applications from storage', e);
  }
  return [];
}

export function saveTicketsToStorage(tickets, userId = null) {
  if (typeof window === 'undefined' || !window.localStorage) return;
  try {
    const key = getStorageKey(STORAGE_APPLICATIONS_KEY, userId);
    localStorage.setItem(key, JSON.stringify(tickets));
  } catch (e) {
    console.error('Failed to save applications to storage', e);
  }
}

export function loadDocumentsFromStorage(userId = null) {
  if (typeof window === 'undefined' || !window.localStorage) return [];
  try {
    const key = getStorageKey(STORAGE_DOCUMENTS_KEY, userId);
    const raw = localStorage.getItem(key);
    if (raw) {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) return parsed;
    }
  } catch (e) {
    console.error('Failed to load documents from storage', e);
  }
  return [];
}

export function saveDocumentsToStorage(docs, userId = null) {
  if (typeof window === 'undefined' || !window.localStorage) return;
  try {
    const key = getStorageKey(STORAGE_DOCUMENTS_KEY, userId);
    localStorage.setItem(key, JSON.stringify(docs));
  } catch (e) {
    console.error('Failed to save documents to storage', e);
  }
}

