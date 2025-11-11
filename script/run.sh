#!/bin/bash

# Run service Java (Spring Boot) on dev mode
# Ensure logs directory exists
mkdir -p .logs

nohup mvn spring-boot:run -f discovery-server/pom.xml > .logs/discovery-server.log 2>&1 &
nohup mvn spring-boot:run -f admin-service/pom.xml > .logs/admin-service.log 2>&1 &
nohup mvn spring-boot:run -f api-gateway/pom.xml > .logs/api-gateway.log 2>&1 &
nohup mvn spring-boot:run -f demo/pom.xml > .logs/demo.log 2>&1 &
nohup mvn spring-boot:run -f auth-service/pom.xml > .logs/auth-service.log 2>&1 &
nohup mvn spring-boot:run -f user-service/pom.xml > .logs/user-service.log 2>&1 &

# Start blog-service with its .env loaded
(
	set -a
	if [ -f "blog-service/.env" ]; then
		# shellcheck disable=SC1091
		source blog-service/.env
	fi
	set +a
	nohup mvn spring-boot:run -f blog-service/pom.xml > .logs/blog-service.log 2>&1 &
)

# Start gallery-service with its .env loaded into environment so DB_/MINIO_ variables are available
(
	set -a
	if [ -f "gallery-service/.env" ]; then
		# shellcheck disable=SC1091
		source gallery-service/.env
	fi
	set +a
	nohup mvn spring-boot:run -f gallery-service/pom.xml > .logs/gallery-service.log 2>&1 &
)

# RUn service Go ( Gin ) on dev mode
(cd go-service && nohup go run server.go > ../.logs/go-service.log 2>&1 &)

# Run service Python (FastAPI) on dev mode
(cd plan-service && source .venv/bin/activate && nohup uvicorn server:app --host 0.0.0.0 --port 8001 > ../.logs/plan-service.log 2>&1 &)
(cd chatbot-service && source .venv/bin/activate && nohup python main.py > ../.logs/chatbot-service.log 2>&1 &)

echo "Tất cả các service đã được khởi động ở chế độ dev."