"""API routing layer.

Routers are grouped by bounded context inside ``app.api.routes``. The
:func:`get_api_router` function composes a top-level router that the
application entrypoint can mount. This keeps the FastAPI application
construction isolated from the route definitions.
"""

from __future__ import annotations

from fastapi import APIRouter

from .routes.health import router as health_router


def get_api_router() -> APIRouter:
    """Assemble the public API router."""
    api_router = APIRouter(prefix="/api")
    api_router.include_router(health_router)
    return api_router


__all__ = ["get_api_router"]
