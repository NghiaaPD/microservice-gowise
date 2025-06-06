FROM python:3.11-slim
WORKDIR /app
COPY . .
RUN pip install fastapi uvicorn py_eureka_client
EXPOSE 8000
CMD ["uvicorn", "server:app", "--host", "0.0.0.0", "--port", "8000"]