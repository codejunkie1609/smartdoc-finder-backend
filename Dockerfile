FROM eclipse-temurin:21-jdk-alpine

WORKDIR /APP

COPY build/libs/smartdoc-finder-backend-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
