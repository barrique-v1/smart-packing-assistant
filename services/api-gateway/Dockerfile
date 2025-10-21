# Multi-stage build for API Gateway
# Stage 1: Build
FROM gradle:8.14-jdk21-alpine AS builder

WORKDIR /app

# Copy entire project (needed for shared module dependency)
COPY --chown=gradle:gradle . .

# Build the API Gateway (this will also build the shared module)
RUN gradle :services:api-gateway:bootJar --no-daemon --info

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Smart Packing Assistant Team"
LABEL description="API Gateway for Smart Packing Assistant"

# Create non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy JAR from builder stage
COPY --from=builder /app/services/api-gateway/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=docker"]
