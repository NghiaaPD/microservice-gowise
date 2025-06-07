FROM golang:1.22-alpine AS builder
WORKDIR /app
COPY . .
RUN go build -o server server.go

FROM alpine:latest
WORKDIR /app
COPY --from=builder /app/server .
COPY .env .
EXPOSE 8002
CMD ["./server"]