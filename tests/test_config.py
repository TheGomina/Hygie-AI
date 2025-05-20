import os
from pathlib import Path
import pytest
from backend.bmp_service.config import _env_file_path, settings


def test_env_file_path():
    path = _env_file_path()
    # Should point to a .env file in project root
    assert isinstance(path, Path) or isinstance(path, str)
    p = Path(path)
    assert p.name == '.env'


def test_settings_defaults(tmp_path, monkeypatch):
    # Test default settings values
    # Temporarily override HOME to isolate llm_base_dir
    monkeypatch.setenv('HOME', str(tmp_path))
    import importlib
    # Reload settings to apply new HOME
    import backend.bmp_service.config as cfg_module
    importlib.reload(cfg_module)
    s = cfg_module.settings
    assert s.version == '0.1.0'
    assert isinstance(s.hf_api_key, str)
    assert s.hf_api_key == ''
    assert isinstance(s.redis_url, str)
    assert 'redis://' in s.redis_url
    # llm_base_dir defaults to HOME/LLM
    assert s.llm_base_dir == Path(str(tmp_path)) / 'LLM'
