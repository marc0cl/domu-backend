"""Health-check endpoint."""

from __future__ import annotations

from fastapi import APIRouter, Depends

from ...services.health_service import HealthService, get_health_service
from ...domain.models.health import HealthStatus

router = APIRouter(prefix="/health", tags=["Health"])


@router.get("/", response_model=HealthStatus, summary="Service health-check")
def read_health(service: HealthService = Depends(get_health_service)) -> HealthStatus:
    """Return the current health information for the service."""
    return service.get_status()


__all__ = ["router"]
