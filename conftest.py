# Add project root to Python path and alias multipart to python_multipart
import os
import sys

sys.path.insert(0, os.path.abspath(os.path.dirname(__file__)))
# Alias multipart import to python_multipart for Starlette compatibility
import python_multipart as _python_multipart

sys.modules["multipart"] = _python_multipart
