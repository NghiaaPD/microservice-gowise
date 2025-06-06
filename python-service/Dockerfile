FROM python:3.10-slim
WORKDIR /app
COPY pyproject.toml uv.lock ./
RUN pip install uv
RUN uv pip install --system --no-cache .
COPY . .
EXPOSE 8001
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8001"]