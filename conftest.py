# Add project root to Python path and alias multipart to python_multipart
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))
# Alias multipart import to python_multipart for Starlette compatibility
try:
    import python_multipart as _python_multipart
    sys.modules["python_multipart"] = _python_multipart
    sys.modules["multipart"] = _python_multipart
except ImportError:
    # multipart support missing, continue without alias
    pass

import pytest
from starlette.testclient import TestClient

@pytest.fixture(scope="session")
def auth_client():
    from backend.auth_service.app import app as auth_app
    return TestClient(auth_app)

@pytest.fixture(scope="session")
def bmp_client():
    from backend.bmp_service.main import app as bmp_app
    return TestClient(bmp_app)

@pytest.fixture
def pro_token():
    from backend.auth_service.security import create_access_token
    return create_access_token(subject="test-pro", role="pro")

@pytest.fixture
def patient_token():
    from backend.auth_service.security import create_access_token
    return create_access_token(subject="test-patient", role="patient")
