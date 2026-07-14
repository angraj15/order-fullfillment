# ============================
# Multi-stage build: Backend
# ============================

# Stage 1: Build with Gradle
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
RUN ./gradlew :app:bootJar --no-daemon -x test

# Stage 2: Runtime with minimal JRE
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=build /app/app/build/libs/order-fulfilment-app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
