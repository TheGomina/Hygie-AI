FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
# Copy code inside package dir to preserve import path
COPY . /app/bmp_service
# Ensure package directory is discoverable
ENV PYTHONPATH="/app"
CMD ["uvicorn", "bmp_service.main:app", "--host", "0.0.0.0", "--port", "8000"]
