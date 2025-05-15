from setuptools import find_packages, setup

setup(
    name="hygie-ai",
    version="0.2.0",
    description="Hygie-AI Shared Medication Review Service",
    package_dir={"": "backend"},
    packages=find_packages(where="backend"),
    install_requires=[
        "fastapi==0.110.2",
        "uvicorn[standard]==0.29.0",
        "pydantic==2.7.1",
        "torch>=2.2",
        "transformers>=4.40",
        "accelerate>=0.27",
        "httpx>=0.27",
        "pyyaml>=6.0",
        "prometheus_client>=0.20",
        "pandas>=2.2",
        "pdfminer.six>=20221105",
        "python-jose[cryptography]>=3.3",
        "fhir.resources>=7.2",
    ],
)
