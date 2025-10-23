"""FastAPI application entrypoint.

The module exposes a :func:`create_app` factory that wires together the
infrastructure, domain services and API routers. This promotes explicit
configuration and keeps the global state surface small.
"""

from __future__ import annotations

from fastapi import FastAPI

from .api import get_api_router
from .core.config import get_settings
from .infrastructure.logging import configure_logging


def create_app() -> FastAPI:
    """Create and configure a :class:`FastAPI` instance."""
    settings = get_settings()
    configure_logging(settings)

    app = FastAPI(
        title=settings.app_name,
        version=settings.version,
        docs_url="/docs" if settings.enable_docs else None,
        redoc_url="/redoc" if settings.enable_docs else None,
    )

    app.include_router(get_api_router())

    return app


app = create_app()
