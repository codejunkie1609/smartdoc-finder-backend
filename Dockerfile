# Use a Gradle image to build the application JAR
FROM gradle:8.8.0-jdk21-alpine AS builder
WORKDIR /app
COPY . .
# Build the application, skipping tests as they should be run in a separate CI step
RUN gradle build --no-daemon -x test

# --- Final Stage ---
# Use a minimal Java runtime image
FROM eclipse-temurin:21-jdk-alpine
WORKDIR /app

# Install netcat, which is needed for the wait script
RUN apk add --no-cache netcat-openbsd

# ✅ CORRECTED: Copy the wait script directly from the build context, not from the builder stage.
# This assumes your script is in './scripts/wait-for-services.sh' inside the backend folder.
COPY scripts/wait-for-services.sh /wait-for-services.sh
RUN chmod +x /wait-for-services.sh

# Copy the built application JAR from the builder stage
COPY --from=builder /app/build/libs/smartdoc-finder-backend-*.jar app.jar

# Expose the application and debug ports
EXPOSE 8080
EXPOSE 5005
ENV JAVA_TOOL_OPTIONS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"

# Set the wait script as the entrypoint
ENTRYPOINT ["/wait-for-services.sh"]

# The default command to run after the wait script succeeds
CMD ["java", "-jar", "app.jar"]
