FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY .env .env
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]