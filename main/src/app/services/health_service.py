"""Service layer utilities for operational health."""

from __future__ import annotations

from fastapi import Depends

from ..core.config import Settings, get_settings
from ..domain.models.health import HealthStatus


class HealthService:
    """Provide health indicators for the API."""

    def __init__(self, settings: Settings) -> None:
        self._settings = settings

    def get_status(self) -> HealthStatus:
        """Return the current health status for the application."""
        return HealthStatus(
            service=self._settings.app_name,
            status="healthy",
            environment=self._settings.environment,
        )


def get_health_service(settings: Settings = Depends(get_settings)) -> HealthService:
    """FastAPI dependency that instantiates :class:`HealthService`."""
    return HealthService(settings=settings)


__all__ = ["HealthService", "get_health_service"]
