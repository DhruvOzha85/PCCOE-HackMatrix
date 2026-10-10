"""
FIN Cross-Document Evidence Verification Models & Data Types.
Defines schemas for eligibility conditions, evidence items, conflict records,
and verification results across applicant documents.
"""

from dataclasses import dataclass, field
from datetime import datetime, timezone
from enum import Enum
from typing import Any, Dict, List, Optional, Union


class EvidenceClassification(str, Enum):
    """
    Standard three-state evidence classification for eligibility condition verification.
    """
    SUPPORTING = "SUPPORTING"
    CONTRADICTING = "CONTRADICTING"
    INSUFFICIENT = "INSUFFICIENT"


class ConflictSeverity(str, Enum):
    """Priority level for caseworker review."""
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    CRITICAL = "CRITICAL"


@dataclass
class EligibilityCondition:
    """
    Structured representation of a statutory scheme eligibility condition.
    """
    condition_id: str
    scheme_id: str
    description: str
    category: str  # annual_income, age, residence, marital_status, occupation, land_ownership, social_category, document_requirement
    operator: str  # <=, <, >=, >, ==, !=, in, contains, between
    threshold: Optional[Any] = None
    unit: Optional[str] = None  # INR/year, years, acres, hectares, etc.
    required_evidence_types: List[str] = field(default_factory=list)
    mandatory: bool = True
    rule_combination: str = "AND"  # AND / OR
    is_verified: bool = True  # False if rule is unverified/inferred
    validity_period_days: Optional[int] = None  # e.g., 365 days for income certificates

    def to_dict(self) -> Dict[str, Any]:
        return {
            "condition_id": self.condition_id,
            "scheme_id": self.scheme_id,
            "description": self.description,
            "category": self.category,
            "operator": self.operator,
            "threshold": self.threshold,
            "unit": self.unit,
            "required_evidence_types": self.required_evidence_types,
            "mandatory": self.mandatory,
            "rule_combination": self.rule_combination,
            "is_verified": self.is_verified,
            "validity_period_days": self.validity_period_days,
        }

    @classmethod
    def from_dict(cls, data: Dict[str, Any]) -> "EligibilityCondition":
        return cls(
            condition_id=str(data["condition_id"]),
            scheme_id=str(data.get("scheme_id", "")),
            description=str(data.get("description", "")),
            category=str(data.get("category", "general")),
            operator=str(data.get("operator", "==")),
            threshold=data.get("threshold"),
            unit=data.get("unit"),
            required_evidence_types=list(data.get("required_evidence_types", [])),
            mandatory=bool(data.get("mandatory", True)),
            rule_combination=str(data.get("rule_combination", "AND")),
            is_verified=bool(data.get("is_verified", True)),
            validity_period_days=data.get("validity_period_days"),
        )


@dataclass
class ConditionEvidenceItem:
    """
    Represents an extracted piece of evidence linked to a condition and document.
    Preserves exact provenance, dates, original excerpts, and normalized facts.
    """
    evidence_id: str
    condition_id: str
    document_id: str
    document_type: str
    filename: str
    excerpt: str
    classification: EvidenceClassification
    explanation: str
    issuer: Optional[str] = None
    document_date: Optional[str] = None
    valid_from: Optional[str] = None
    valid_until: Optional[str] = None
    page: Optional[int] = 1
    normalized_value: Optional[Any] = None
    normalized_unit: Optional[str] = None
    raw_value: Optional[Any] = None
    confidence: float = 1.0
    method: str = "NLP_SEMANTIC_VERIFIER"
    warnings: List[str] = field(default_factory=list)
    sha256: Optional[str] = None

    def to_dict(self) -> Dict[str, Any]:
        return {
            "evidence_id": self.evidence_id,
            "condition_id": self.condition_id,
            "document_id": self.document_id,
            "document_type": self.document_type,
            "filename": self.filename,
            "issuer": self.issuer,
            "document_date": self.document_date,
            "valid_from": self.valid_from,
            "valid_until": self.valid_until,
            "page": self.page,
            "excerpt": self.excerpt,
            "classification": self.classification.value if hasattr(self.classification, "value") else str(self.classification),
            "explanation": self.explanation,
            "normalized_value": self.normalized_value,
            "normalized_unit": self.normalized_unit,
            "raw_value": self.raw_value,
            "confidence": round(self.confidence, 4),
            "method": self.method,
            "warnings": self.warnings,
            "sha256": self.sha256,
        }


@dataclass
class CrossDocumentConflict:
    """
    Record of a factual conflict detected across multiple documents or between
    a document and an applicant profile declaration.
    """
    conflict_id: str
    condition_id: str
    field_category: str
    conflicting_evidence_ids: List[str]
    source_excerpts: List[Dict[str, Any]]
    explanation: str
    severity: ConflictSeverity = ConflictSeverity.HIGH
    status: str = "OPEN"  # OPEN, RESOLVED, UNDER_REVIEW, REQUIRES_DOCUMENTS
    resolution_reason: Optional[str] = None
    resolved_by: Optional[str] = None
    resolved_at: Optional[str] = None
    authoritative_source: Optional[str] = None
    detected_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())

    def to_dict(self) -> Dict[str, Any]:
        return {
            "conflict_id": self.conflict_id,
            "condition_id": self.condition_id,
            "field_category": self.field_category,
            "conflicting_evidence_ids": self.conflicting_evidence_ids,
            "source_excerpts": self.source_excerpts,
            "explanation": self.explanation,
            "severity": self.severity.value if hasattr(self.severity, "value") else str(self.severity),
            "status": self.status,
            "resolution_reason": self.resolution_reason,
            "resolved_by": self.resolved_by,
            "resolved_at": self.resolved_at,
            "authoritative_source": self.authoritative_source,
            "detected_at": self.detected_at,
        }


@dataclass
class ConditionVerificationResult:
    """
    Aggregated evidence result for a single eligibility condition.
    """
    condition_id: str
    description: str
    category: str
    overall_status: EvidenceClassification
    supporting_evidence: List[ConditionEvidenceItem] = field(default_factory=list)
    contradicting_evidence: List[ConditionEvidenceItem] = field(default_factory=list)
    insufficient_evidence: List[ConditionEvidenceItem] = field(default_factory=list)
    detected_conflicts: List[CrossDocumentConflict] = field(default_factory=list)
    threshold: Optional[Any] = None
    actual_value: Optional[Any] = None
    unit: Optional[str] = None
    explanation: str = ""
    requires_manual_review: bool = False
    ready_for_rule_engine: bool = False

    def to_dict(self) -> Dict[str, Any]:
        return {
            "condition_id": self.condition_id,
            "description": self.description,
            "category": self.category,
            "overall_status": self.overall_status.value if hasattr(self.overall_status, "value") else str(self.overall_status),
            "supporting_evidence": [e.to_dict() for e in self.supporting_evidence],
            "contradicting_evidence": [e.to_dict() for e in self.contradicting_evidence],
            "insufficient_evidence": [e.to_dict() for e in self.insufficient_evidence],
            "detected_conflicts": [c.to_dict() for c in self.detected_conflicts],
            "threshold": self.threshold,
            "actual_value": self.actual_value,
            "unit": self.unit,
            "explanation": self.explanation,
            "requires_manual_review": self.requires_manual_review,
            "ready_for_rule_engine": self.ready_for_rule_engine,
        }


@dataclass
class CrossDocumentVerificationReport:
    """
    Comprehensive verification report covering all conditions of a scheme for an applicant.
    """
    applicant_id: str
    application_id: Optional[str]
    scheme_id: str
    scheme_name: str
    conditions: List[ConditionVerificationResult]
    conflicts: List[CrossDocumentConflict]
    overall_verification_status: str  # VERIFIED, CONFLICT_DETECTED, MISSING_EVIDENCE, CONTRADICTED, UNVERIFIED_RULE
    source_documents: List[Dict[str, Any]]
    total_conditions: int
    supporting_count: int
    contradicting_count: int
    insufficient_count: int
    requires_caseworker_review: bool
    duplicate_documents_ignored: List[str] = field(default_factory=list)
    verified_at: str = field(default_factory=lambda: datetime.now(timezone.utc).isoformat())
    verification_method: str = "NLP_SEMANTIC_EVIDENCE_ENGINE"

    @property
    def overall_status(self) -> str:
        return self.overall_verification_status

    @property
    def detected_conflicts(self) -> List[CrossDocumentConflict]:
        return self.conflicts

    @property
    def ready_for_rule_engine(self) -> bool:
        return not self.requires_caseworker_review and self.overall_verification_status != "CONFLICT_DETECTED"

    def to_dict(self) -> Dict[str, Any]:
        cond_list = [c.to_dict() for c in self.conditions]
        cnf_list = [cnf.to_dict() for cnf in self.conflicts]
        return {
            "applicant_id": self.applicant_id,
            "application_id": self.application_id,
            "scheme_id": self.scheme_id,
            "scheme_name": self.scheme_name,
            "conditions": cond_list,
            "condition_results": cond_list,
            "conflicts": cnf_list,
            "overall_verification_status": self.overall_verification_status,
            "overall_status": self.overall_verification_status,
            "detected_conflicts": cnf_list,
            "source_documents": self.source_documents,
            "duplicate_documents_ignored": self.duplicate_documents_ignored,
            "total_conditions": self.total_conditions,
            "supporting_count": self.supporting_count,
            "contradicting_count": self.contradicting_count,
            "insufficient_count": self.insufficient_count,
            "requires_caseworker_review": self.requires_caseworker_review,
            "verified_at": self.verified_at,
            "verification_method": self.verification_method,
        }
