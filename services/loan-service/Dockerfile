ARG SERVICE_NAME=loan-service
ARG SERVICE_PORT=8082

FROM gradle:8.12-jdk21 AS builder

ARG SERVICE_NAME
WORKDIR /workspace

COPY settings.gradle build.gradle gradle.properties ./
COPY services ./services

RUN gradle ":${SERVICE_NAME}:bootJar" --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime

ARG SERVICE_NAME
ARG SERVICE_PORT

ENV TZ=Asia/Jakarta \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Jakarta"

RUN apk add --no-cache curl tzdata \
    && addgroup -S app \
    && adduser -S app -G app

WORKDIR /app

COPY --from=builder /workspace/services/${SERVICE_NAME}/build/libs/*.jar app.jar
RUN chown -R app:app /app

USER app

EXPOSE ${SERVICE_PORT}

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl -fsS "http://localhost:${SERVICE_PORT}/actuator/health/liveness" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
