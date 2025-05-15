from pathlib import Path

from pydantic import BaseSettings


def _env_file_path() -> str:
    # Load from project root
    return Path(__file__).resolve().parents[2] / ".env"


class Settings(BaseSettings):  # Hygie-AI configuration
    # Application version
    version: str = "0.1.0"
    hf_api_key: str = ""
    redis_url: str = "redis://redis:6379/0"
    llm_base_dir: Path = Path.home() / "LLM"

    class Config:
        env_file = _env_file_path()
        env_file_encoding = "utf-8"


settings = Settings()
