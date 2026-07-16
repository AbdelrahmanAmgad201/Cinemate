# ===============================
# Build stage
# ===============================
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache dependencies on the pom first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Build the jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# ===============================
# Runtime stage
# ===============================
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /build/target/gateway-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
