#!/bin/bash

# Kill Spring Boot (Java)
pkill -f 'spring-boot:run'
pkill -f 'admin-service'
pkill -f 'api-gateway'
pkill -f 'discovery-server'
pkill -f 'demo'

# Kill another service (tìm theo tên file hoặc thư mục)
lsof -ti :8001-8003 | xargs -r kill -9


rm -f .logs/*.log

echo "Đã dừng các service và xóa file log trong .logs/"