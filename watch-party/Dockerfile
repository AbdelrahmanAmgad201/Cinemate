# ===============================
# Build stage
# ===============================
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /build

# Copy only pom.xml first to cache dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the jar with optimizations
RUN mvn clean package -DskipTests -B

# ===============================
# Runtime stage (Alpine for minimal size)
# ===============================
FROM eclipse-temurin:17-jre-alpine

# Add curl for healthchecks
RUN apk add --no-cache curl

WORKDIR /app

# Create non-root user for security
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Copy only the built jar from the builder stage
# Note: Adjust the jar name to match your actual artifact name
COPY --from=builder /build/target/*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose port 8081 (matching your application.properties)
EXPOSE 8081

# Use exec form for proper signal handling
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]