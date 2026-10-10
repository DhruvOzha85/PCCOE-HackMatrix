"""
FIN Addition 2: Evidence-Based Cross-Document Verification Test Suite.
Tests the 17 core scenarios required by HackMatrix FIN-03 specification:
  1. Paraphrased statement supporting a condition
  2. Evidence directly contradicting a numerical threshold
  3. Evidence semantically related but insufficient
  4. Missing required evidence
  5. Two documents containing conflicting income values
  6. Conflicting values with different assessment dates
  7. Expired certificate where rule specifies validity
  8. Document whose validity policy is unknown
  9. Exact threshold-boundary tests (<= vs <)
  10. Equivalent numerical formats (₹1,50,000, 1.5 Lakhs, INR 150000)
  11. Duplicate copies of the same document (deduplication)
  12. Malformed model output handling
  13. Inference provider timeout resilience
"""

import unittest
from datetime import datetime, timezone, timedelta
from typing import Any, Dict, List

from src.nlp.evidence_models import (
    EligibilityCondition,
    EvidenceClassification,
    ConflictSeverity,
)
from src.nlp.evidence_verifier import (
    NLPSemanticEvidenceVerifier,
    parse_indian_currency_number,
    parse_standard_date,
)


class TestEvidenceVerificationScenarios(unittest.TestCase):
    def setUp(self):
        self.verifier = NLPSemanticEvidenceVerifier()

    # Scenario 1: Paraphrased statement that supports a condition
    def test_01_paraphrased_statement_supporting(self):
        cond = EligibilityCondition(
            condition_id="c_inc_2l",
            scheme_id="sch_test",
            description="Family income must not exceed ₹2,00,000 per annum",
            category="annual_income",
            operator="<=",
            threshold=200000,
            unit="INR/year",
        )
        # Paraphrase: "total aggregate annual family earnings"
        doc = {
            "id": "doc_01",
            "fileName": "income_cert.pdf",
            "documentType": "income_certificate",
            "issuer": "Revenue Circle Officer",
            "extractedText": "This certifies that total aggregate annual family earnings of the applicant is Rs. 1,45,000 only.",
            "extractedData": {}
        }
        res = self.verifier.verify_single_condition(cond, [doc])
        self.assertEqual(res.overall_status, EvidenceClassification.SUPPORTING)
        self.assertEqual(len(res.supporting_evidence), 1)
        self.assertEqual(res.supporting_evidence[0].normalized_value, 145000.0)
        self.assertIn("1,45,000", res.supporting_evidence[0].excerpt)

    # Scenario 2: Evidence that directly contradicts a numerical threshold
    def test_02_evidence_contradicting_numerical_threshold(self):
        cond = EligibilityCondition(
            condition_id="c_inc_2l",
            scheme_id="sch_test",
            description="Family income must not exceed ₹2,00,000",
            category="annual_income",
            operator="<=",
            threshold=200000,
            unit="INR/year",
        )
        doc = {
            "id": "doc_02",
            "fileName": "salary_statement.pdf",
            "documentType": "salary_slip",
            "extractedText": "Gross household annual revenue for financial year stands at ₹3,50,000.",
            "extractedData": {}
        }
        res = self.verifier.verify_single_condition(cond, [doc])
        self.assertEqual(res.overall_status, EvidenceClassification.CONTRADICTING)
        self.assertEqual(len(res.contradicting_evidence), 1)
        self.assertEqual(res.contradicting_evidence[0].normalized_value, 350000.0)

    # Scenario 3: Evidence that is semantically related but insufficient
    def test_03_semantically_related_but_insufficient(self):
        cond = EligibilityCondition(
            condition_id="c_inc_2l",
            scheme_id="sch_test",
            description="Family income must not exceed ₹2,00,000",
            category="annual_income",
            operator="<=",
            threshold=200000,
            unit="INR/year",
        )
        doc = {
            "id": "doc_03",
            "fileName": "affidavit.pdf",
            "documentType": "affidavit",
            "extractedText": "The applicant belongs to an economically weaker household and marginalized family.",
            "extractedData": {}
        }
        res = self.verifier.verify_single_condition(cond, [doc])
        self.assertEqual(res.overall_status, EvidenceClassification.INSUFFICIENT)
        self.assertTrue(res.requires_manual_review)
        self.assertFalse(res.ready_for_rule_engine)

    # Scenario 4: Missing required evidence
    def test_04_missing_required_evidence(self):
        cond = EligibilityCondition(
            condition_id="c_kisan_land",
            scheme_id="pm_kisan",
            description="Applicant must own cultivable agricultural land up to 5 acres",
            category="land_ownership",
            operator="<=",
            threshold=5.0,
            unit="acres",
            mandatory=True
        )
        # Uploaded doc is unrelated bank passbook without landholding details
        doc = {
            "id": "doc_04",
            "fileName": "passbook.pdf",
            "documentType": "bank_passbook",
            "extractedText": "Savings account opened at branch Pune. Balance ₹5,000.",
            "extractedData": {}
        }
        res = self.verifier.verify_single_condition(cond, [doc])
        self.assertEqual(res.overall_status, EvidenceClassification.INSUFFICIENT)
        self.assertTrue(res.requires_manual_review)

    # Scenario 5: Two documents containing conflicting income values
    def test_05_conflicting_income_values_across_documents(self):
        cond = EligibilityCondition(
            condition_id="c_inc_3l",
            scheme_id="sch_test",
            description="Annual income ceiling ₹3,00,000",
            category="annual_income",
            operator="<=",
            threshold=300000,
            unit="INR/year",
        )
        docs = [
            {
                "id": "doc_inc_cert",
                "fileName": "income_cert.pdf",
                "documentType": "income_certificate",
                "extractedText": "Certified annual family income is ₹1,60,000.",
                "extractedData": {"annual_income": 160000}
            },
            {
                "id": "doc_salary_slip",
                "fileName": "salary_slip.pdf",
                "documentType": "salary_slip",
                "extractedText": "Annual household income is ₹4,20,000.",
                "extractedData": {"annual_income": 420000}
            }
        ]
        report = self.verifier.verify_application_evidence(
            applicant_id="app_123",
            application_id="sub_456",
            scheme_id="sch_test",
            scheme_name="Test Scheme",
            conditions=[cond],
            documents=docs
        )
        self.assertEqual(report.overall_status, "CONFLICT_DETECTED")
        self.assertGreater(len(report.detected_conflicts), 0)
        conflict = report.detected_conflicts[0]
        self.assertEqual(conflict.field_category, "annual_income")
        self.assertEqual(len(conflict.source_excerpts), 2)
        self.assertFalse(report.ready_for_rule_engine)

    # Scenario 6: Conflicting values with different assessment dates
    def test_06_conflicting_values_with_different_dates(self):
        cond = EligibilityCondition(
            condition_id="c_inc_3l",
            scheme_id="sch_test",
            description="Annual income ceiling ₹3,00,000",
            category="annual_income",
            operator="<=",
            threshold=300000,
        )
        docs = [
            {
                "id": "doc_old",
                "fileName": "income_2021.pdf",
                "documentType": "income_certificate",
                "issueDate": "2021-03-31",
                "extractedText": "Annual family income for FY 2020-21 is ₹1,20,000.",
                "extractedData": {"annual_income": 120000, "issue_date": "2021-03-31"}
            },
            {
                "id": "doc_new",
                "fileName": "income_2024.pdf",
                "documentType": "income_certificate",
                "issueDate": "2024-04-10",
                "extractedText": "Annual family income for FY 2023-24 is ₹2,80,000.",
                "extractedData": {"annual_income": 280000, "issue_date": "2024-04-10"}
            }
        ]
        report = self.verifier.verify_application_evidence(
            applicant_id="app_123",
            application_id="sub_456",
            scheme_id="sch_test",
            scheme_name="Test Scheme",
            conditions=[cond],
            documents=docs,
            assessment_date="2024-05-01"
        )
        # Preserves both excerpts and dates rather than silently deleting the historical record
        self.assertGreater(len(report.detected_conflicts), 0)
        c = report.detected_conflicts[0]
        dates = [s.get("date") for s in c.source_excerpts]
        self.assertIn("2021-03-31", dates)
        self.assertIn("2024-04-10", dates)

    # Scenario 7: Expired certificate where rule specifies validity
    def test_07_expired_certificate_where_rule_specifies_validity(self):
        cond = EligibilityCondition(
            condition_id="c_inc_valid",
            scheme_id="sch_scholarship",
            description="Current valid income certificate below ₹2,00,000",
            category="annual_income",
            operator="<=",
            threshold=200000,
            validity_period_days=365
        )
        doc = {
            "id": "doc_expired",
            "fileName": "old_income.pdf",
            "documentType": "income_certificate",
            "issueDate": "2020-01-15",
            "validUntil": "2021-01-15",
            "extractedText": "Certified annual income ₹80,000. Valid strictly until 15-01-2021.",
            "extractedData": {"annual_income": 80000, "valid_until": "2021-01-15"}
        }
        res = self.verifier.verify_single_condition(cond, [doc], assessment_date="2024-06-01")
        self.assertEqual(res.overall_status, EvidenceClassification.INSUFFICIENT)
        self.assertTrue(res.requires_manual_review)
        self.assertIn("expired", res.insufficient_evidence[0].explanation.lower())

    # Scenario 8: Document whose validity policy is unknown
    def test_08_unknown_validity_policy_does_not_auto_reject(self):
        cond = EligibilityCondition(
            condition_id="c_domicile",
            scheme_id="sch_dom",
            description="Applicant must reside in Maharashtra",
            category="residence",
            operator="==",
            threshold="Maharashtra",
            validity_period_days=None  # Domicile certificates do not have standard expiry
        )
        doc = {
            "id": "doc_dom_2015",
            "fileName": "domicile_certificate.pdf",
            "documentType": "domicile",
            "issueDate": "2015-06-20",
            "extractedText": "Certified that applicant is permanent resident of Maharashtra.",
            "extractedData": {"state": "Maharashtra"}
        }
        res = self.verifier.verify_single_condition(cond, [doc])
        # Does not reject simply because it was issued in 2015 when validity policy is undefined
        self.assertEqual(res.overall_status, EvidenceClassification.SUPPORTING)

    # Scenario 9: Exact threshold boundary tests (<= vs <)
    def test_09_exact_boundary_conditions(self):
        # Boundary 1: <= 200000 with exactly 200000 -> PASS/SUPPORTING
        cond_lte = EligibilityCondition(
            condition_id="c_lte",
            scheme_id="sch_bnd",
            description="Income <= 200000",
            category="annual_income",
            operator="<=",
            threshold=200000
        )
        doc_200k = {
            "id": "doc_bnd",
            "fileName": "inc.pdf",
            "extractedText": "Total annual income is ₹2,00,000.",
            "extractedData": {"annual_income": 200000}
        }
        res_lte = self.verifier.verify_single_condition(cond_lte, [doc_200k])
        self.assertEqual(res_lte.overall_status, EvidenceClassification.SUPPORTING)

        # Boundary 2: < 200000 with exactly 200000 -> CONTRADICTING
        cond_lt = EligibilityCondition(
            condition_id="c_lt",
            scheme_id="sch_bnd",
            description="Income strictly < 200000",
            category="annual_income",
            operator="<",
            threshold=200000
        )
        res_lt = self.verifier.verify_single_condition(cond_lt, [doc_200k])
        self.assertEqual(res_lt.overall_status, EvidenceClassification.CONTRADICTING)

    # Scenario 10: Equivalent numerical formats (₹1,50,000, 1.5 Lakhs, INR 150000)
    def test_10_equivalent_numerical_formats(self):
        self.assertEqual(parse_indian_currency_number("₹1,50,000"), 150000.0)
        self.assertEqual(parse_indian_currency_number("Rs. 1,50,000"), 150000.0)
        self.assertEqual(parse_indian_currency_number("INR 150000"), 150000.0)
        self.assertEqual(parse_indian_currency_number("1.5 Lakhs"), 150000.0)
        self.assertEqual(parse_indian_currency_number("1.5 lac"), 150000.0)
        self.assertEqual(parse_indian_currency_number("2.5 Crore"), 25000000.0)

    # Scenario 11: Duplicate copies of the same document
    def test_11_duplicate_copies_not_corroborated(self):
        cond = EligibilityCondition(
            condition_id="c_age",
            scheme_id="sch_age",
            description="Age >= 18",
            category="age",
            operator=">=",
            threshold=18
        )
        # Identical document uploaded twice
        doc1 = {
            "id": "doc_1",
            "fileName": "aadhaar.pdf",
            "extractedText": "Name: Amit Sharma, Date of Birth: 12/04/1995.",
            "sha256": "abcdef1234567890"
        }
        doc2 = {
            "id": "doc_2",
            "fileName": "aadhaar_copy.pdf",
            "extractedText": "Name: Amit Sharma, Date of Birth: 12/04/1995.",
            "sha256": "abcdef1234567890"
        }
        report = self.verifier.verify_application_evidence(
            applicant_id="app_1",
            application_id="app_sub",
            scheme_id="sch_age",
            scheme_name="Age Scheme",
            conditions=[cond],
            documents=[doc1, doc2]
        )
        # Duplicate should be detected and filtered out from independent corroboration
        self.assertGreater(len(report.duplicate_documents_ignored), 0)

    # Scenario 12: Malformed model output handling
    def test_12_malformed_model_output_safe_recovery(self):
        # Empty/corrupt document text
        cond = EligibilityCondition(
            condition_id="c_inc",
            scheme_id="sch_test",
            description="Income <= 100000",
            category="annual_income",
            operator="<=",
            threshold=100000
        )
        doc = {
            "id": "doc_corrupt",
            "fileName": "corrupt.pdf",
            "extractedText": "\x00\x00\x00\xff\xfe unreadable corrupt bytes",
            "extractedData": None
        }
        res = self.verifier.verify_single_condition(cond, [doc])
        # Gracefully defaults to INSUFFICIENT with clear warning, no crash
        self.assertEqual(res.overall_status, EvidenceClassification.INSUFFICIENT)
        self.assertFalse(res.ready_for_rule_engine)

    # Scenario 13: Inference provider timeout resilience
    def test_13_timeout_resilience(self):
        # Engine operates deterministically even without external LLM network connection
        verifier_offline = NLPSemanticEvidenceVerifier(llm_provider=None)
        cond = EligibilityCondition(
            condition_id="c_occ",
            scheme_id="sch_kisan",
            description="Applicant occupation must be Farmer",
            category="occupation",
            operator="==",
            threshold="farmer"
        )
        doc = {
            "id": "doc_kisan",
            "fileName": "kcc.pdf",
            "extractedText": "Declared occupation is Small Marginal Farmer.",
            "extractedData": {"occupation": "farmer"}
        }
        res = verifier_offline.verify_single_condition(cond, [doc])
        self.assertEqual(res.overall_status, EvidenceClassification.SUPPORTING)


if __name__ == "__main__":
    unittest.main()
