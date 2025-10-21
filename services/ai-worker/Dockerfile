# Multi-stage build for AI Worker
# Stage 1: Build
FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app

# Copy entire project (needed for shared module dependency)
COPY --chown=gradle:gradle . .

# Build the AI Worker (this will also build the shared module)
RUN gradle :services:ai-worker:bootJar --no-daemon --info

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Smart Packing Assistant Team"
LABEL description="AI Worker for Smart Packing Assistant - OpenAI Integration"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/services/ai-worker/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=docker"]
