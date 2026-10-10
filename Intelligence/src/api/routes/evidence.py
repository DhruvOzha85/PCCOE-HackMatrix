"""
FIN Evidence Verification API Endpoints (Addition 2: Evidence-Based Cross-Document Verification).
Provides:
  POST /v1/evidence/verify                   - Runs full cross-document evidence verification pipeline
  GET  /v1/evidence/results/{application_id} - Retrieves persisted or cached verification report
  POST /v1/evidence/evaluate-baselines       - Runs reproducible benchmark comparing Baselines A, B, and Proposed NLP
  POST /v1/evidence/conflicts/{conflict_id}/resolve - Resolves an open conflict with reviewer reason & audit
Protected by X-AI-Service-Key with zero LLM hallucination and deterministic numerical arithmetic.
"""

import logging
from typing import Any, Dict, List, Optional
from fastapi import APIRouter, Body, Depends, HTTPException, Path, Query, status
from pydantic import BaseModel, Field

from src.nlp.evidence_models import (
    EligibilityCondition,
    EvidenceClassification,
    CrossDocumentVerificationReport,
    CrossDocumentConflict,
)
from src.nlp.evidence_verifier import NLPSemanticEvidenceVerifier
from src.evaluation.cross_document_eval import CrossDocumentBenchmarkRunner

from ..auth import verify_api_key
from ..config import ServiceConfig
from ..dependencies import get_service_config

logger = logging.getLogger("fin.api.routes.evidence")

router = APIRouter(prefix="/v1/evidence", tags=["Evidence-Based Verification"])

# In-memory store for verification reports (application_id -> dict)
_REPORT_CACHE: Dict[str, Dict[str, Any]] = {}
# In-memory store for conflicts (conflict_id -> CrossDocumentConflict)
_CONFLICT_CACHE: Dict[str, CrossDocumentConflict] = {}

_VERIFIER = NLPSemanticEvidenceVerifier()


class VerifyApplicationEvidenceRequest(BaseModel):
    applicant_id: str = Field(..., description="Unique applicant identifier")
    application_id: Optional[str] = Field(None, description="Optional application record ID")
    scheme_id: str = Field(..., description="Target government scheme identifier")
    scheme_name: str = Field(..., description="Target government scheme display name")
    conditions: List[Dict[str, Any]] = Field(..., description="List of structured eligibility conditions")
    documents: List[Dict[str, Any]] = Field(..., description="List of uploaded documents with extracted text and metadata")
    applicant_profile: Optional[Dict[str, Any]] = Field(None, description="Citizen profile declarations")
    assessment_date: Optional[str] = Field(None, description="Optional assessment date (YYYY-MM-DD)")


class ResolveEvidenceConflictRequest(BaseModel):
    resolver_id: str = Field(..., description="Authenticated caseworker/officer identifier")
    selected_source: str = Field(..., description="Selected authoritative source (DOCUMENT or USER_INPUT)")
    reason: str = Field(..., description="Mandatory justification for resolving the conflict")
    authoritative_value: Optional[Any] = Field(None, description="Optional explicit override value verified by caseworker")


class EvaluateBaselinesRequest(BaseModel):
    split: Optional[str] = Field("held_out_test", description="Dataset split to evaluate: dev, held_out_test, or all")


@router.post(
    "/verify",
    summary="Verify applicant documents against scheme eligibility conditions",
    description="Extracts traceable evidence from uploaded documents, classifies each condition (SUPPORTING, CONTRADICTING, INSUFFICIENT), detects cross-document conflicts, and performs deterministic numerical verification.",
)
async def verify_evidence(
    req: VerifyApplicationEvidenceRequest,
    api_key: str = Depends(verify_api_key),
    service_config: ServiceConfig = Depends(get_service_config),
) -> Dict[str, Any]:
    try:
        parsed_conditions = [
            EligibilityCondition.from_dict(c) for c in req.conditions
        ]
    except Exception as e:
        logger.error("Invalid eligibility conditions schema: %s", e)
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Invalid eligibility condition format: {str(e)}"
        )

    try:
        report = _VERIFIER.verify_application_evidence(
            applicant_id=req.applicant_id,
            application_id=req.application_id,
            scheme_id=req.scheme_id,
            scheme_name=req.scheme_name,
            conditions=parsed_conditions,
            documents=req.documents,
            applicant_profile=req.applicant_profile,
            assessment_date=req.assessment_date,
        )

        res_dict = report.to_dict()

        # Cache report and conflicts for subsequent retrieval & caseworker review
        cache_key = req.application_id or f"{req.applicant_id}_{req.scheme_id}"
        _REPORT_CACHE[cache_key] = res_dict
        for conf in report.detected_conflicts:
            _CONFLICT_CACHE[conf.conflict_id] = conf

        return {
            "success": True,
            "report": res_dict,
        }
    except Exception as e:
        logger.exception("Evidence verification failed for applicant %s: %s", req.applicant_id, e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Evidence verification failed: {str(e)}"
        )


@router.get(
    "/results/{application_id}",
    summary="Get evidence verification results for an application",
    description="Retrieves the cached or persisted condition-level verification results and conflict report.",
)
async def get_verification_results(
    application_id: str = Path(..., description="Application ID"),
    api_key: str = Depends(verify_api_key),
) -> Dict[str, Any]:
    if application_id in _REPORT_CACHE:
        return {
            "success": True,
            "report": _REPORT_CACHE[application_id],
        }

    # Search by prefix or return 404
    for k, v in _REPORT_CACHE.items():
        if application_id in k:
            return {"success": True, "report": v}

    raise HTTPException(
        status_code=status.HTTP_404_NOT_FOUND,
        detail=f"No verification report found for application {application_id}"
    )


@router.post(
    "/conflicts/{conflict_id}/resolve",
    summary="Resolve a cross-document conflict (Caseworker Review)",
    description="Resolves an open evidence conflict with recorded rationale, authoritative source, and audit trail.",
)
async def resolve_evidence_conflict(
    conflict_id: str = Path(..., description="Conflict ID to resolve"),
    body: ResolveEvidenceConflictRequest = Body(...),
    api_key: str = Depends(verify_api_key),
) -> Dict[str, Any]:
    if conflict_id not in _CONFLICT_CACHE:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Conflict record '{conflict_id}' not found"
        )

    conflict = _CONFLICT_CACHE[conflict_id]
    conflict.status = "RESOLVED"
    conflict.resolution_reason = body.reason
    conflict.resolved_by = body.resolver_id
    from datetime import datetime, timezone
    conflict.resolved_at = datetime.now(timezone.utc).isoformat()

    # Update in cached reports as well
    for r in _REPORT_CACHE.values():
        for c_dict in r.get("detected_conflicts", []):
            if c_dict.get("conflict_id") == conflict_id:
                c_dict["status"] = "RESOLVED"
                c_dict["resolution_reason"] = body.reason
                c_dict["resolved_by"] = body.resolver_id
                c_dict["resolved_at"] = conflict.resolved_at

    return {
        "success": True,
        "message": f"Conflict {conflict_id} resolved successfully by caseworker {body.resolver_id}",
        "conflict": conflict.to_dict()
    }


@router.post(
    "/evaluate-baselines",
    summary="Run benchmark comparison across Baseline A, Baseline B, and Proposed NLP Verifier",
    description="Evaluates all 3 approaches on the annotated cross-document dataset and returns per-class precision, recall, F1, Macro-F1, confusion matrix, and Markdown table.",
)
async def evaluate_baselines(
    req: Optional[EvaluateBaselinesRequest] = Body(None),
    api_key: str = Depends(verify_api_key),
) -> Dict[str, Any]:
    try:
        split_requested = req.split if req and req.split else "held_out_test"
        runner = CrossDocumentBenchmarkRunner()
        split_val = None if split_requested == "all" else split_requested
        res = runner.run_full_benchmark(split=split_val)
        md_table = runner.generate_markdown_comparison_table(res)

        return {
            "success": True,
            "split": split_requested,
            "benchmark_results": res,
            "markdown_table": md_table,
        }
    except Exception as e:
        logger.exception("Baseline evaluation failed: %s", e)
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Baseline evaluation failed: {str(e)}"
        )
