#!/bin/bash

# Run service Java (Spring Boot) on dev mode
nohup mvn spring-boot:run -f discovery-server/pom.xml > .logs/discovery-server.log 2>&1 &
nohup mvn spring-boot:run -f admin-service/pom.xml > .logs/admin-service.log 2>&1 &
nohup mvn spring-boot:run -f api-gateway/pom.xml > .logs/api-gateway.log 2>&1 &
nohup mvn spring-boot:run -f demo/pom.xml > .logs/demo.log 2>&1 &
nohup mvn spring-boot:run -f auth-service/pom.xml > .logs/auth-service.log 2>&1 &
nohup mvn spring-boot:run -f user-service/pom.xml > .logs/user-service.log 2>&1 &

# RUn service Go ( Gin ) on dev mode
(cd go-service && nohup go run server.go > ../.logs/go-service.log 2>&1 &)

# Run service Python (FastAPI) on dev mode
(cd plan-service && source .venv/bin/activate && nohup uvicorn server:app --host 0.0.0.0 --port 8001 > ../.logs/plan-service.log 2>&1 &)

echo "Tất cả các service đã được khởi động ở chế độ dev."