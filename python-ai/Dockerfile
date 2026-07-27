FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1
WORKDIR /app/python-ai
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 libglib2.0-0 libxcb1 libxext6 libsm6 \
    && rm -rf /var/lib/apt/lists/*
COPY python-ai/pyproject.toml ./
COPY python-ai/app ./app
RUN pip install . \
    && pip install torch torchvision --index-url https://download.pytorch.org/whl/cpu \
    && pip install ".[memory,vision]"
COPY python-ai/models ./models
COPY python-ai/scripts ./scripts
RUN useradd --system --home-dir /app travelmind \
    && mkdir -p /app/uploads/private /app/uploads/public \
    && chown -R travelmind:travelmind /app
USER travelmind
EXPOSE 19080
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "19080"]
