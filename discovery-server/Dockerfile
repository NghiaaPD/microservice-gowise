FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
COPY .env .env
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]