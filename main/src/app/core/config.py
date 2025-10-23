"""Application configuration management."""

from __future__ import annotations

from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """Runtime configuration values loaded from the environment."""

    app_name: str = Field(default="Domu Backend", description="Service name")
    environment: str = Field(default="development", description="Active environment name")
    version: str = Field(default="0.1.0", description="Application version")
    enable_docs: bool = Field(default=True, description="Expose interactive API docs")

    model_config = {
        "env_file": ".env",
        "env_file_encoding": "utf-8",
        "extra": "ignore",
    }


@lru_cache
def get_settings() -> Settings:
    """Return cached application settings instance."""
    return Settings()


__all__ = ["Settings", "get_settings"]
