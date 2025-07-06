FROM gradle:8.8.0-jdk21-alpine AS builder
WORKDIR /app
COPY . .

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jdk-alpine
WORKDIR /APP
COPY --from=builder /app/build/libs/smartdoc-finder-backend-*.jar app.jar
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
EXPOSE 5005
ENTRYPOINT ["java", "-jar", "app.jar"]
