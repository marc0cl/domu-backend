"""Domain models for operational health."""

from __future__ import annotations

from datetime import datetime, timezone

from pydantic import BaseModel, Field


def _default_timestamp() -> datetime:
    """Return the current UTC timestamp."""
    return datetime.now(tz=timezone.utc)


class HealthStatus(BaseModel):
    """Represents the liveness and readiness of the API."""

    service: str = Field(..., description="Name of the running service")
    status: str = Field(..., description="Semantic health indicator")
    timestamp: datetime = Field(
        default_factory=_default_timestamp,
        description="Timestamp of the health evaluation in UTC.",
    )
    environment: str = Field(..., description="Application runtime environment")


__all__ = ["HealthStatus"]
