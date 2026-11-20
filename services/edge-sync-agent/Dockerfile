FROM python:3.12-slim
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt && mkdir -p /state && chown 10001:10001 /state
COPY agent.py .
USER 10001:10001
CMD ["python", "agent.py"]
