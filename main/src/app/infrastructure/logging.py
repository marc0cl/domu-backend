"""Centralised logging configuration."""

from __future__ import annotations

import logging
from logging.config import dictConfig

from ..core.config import Settings


def configure_logging(settings: Settings) -> None:
    """Configure structured logging for the application."""
    level = logging.INFO if settings.environment == "production" else logging.DEBUG

    dictConfig(
        {
            "version": 1,
            "disable_existing_loggers": False,
            "formatters": {
                "standard": {
                    "format": "%(asctime)s | %(levelname)s | %(name)s | %(message)s",
                }
            },
            "handlers": {
                "default": {
                    "level": level,
                    "formatter": "standard",
                    "class": "logging.StreamHandler",
                }
            },
            "loggers": {
                "": {"handlers": ["default"], "level": level, "propagate": False}
            },
        }
    )


__all__ = ["configure_logging"]
