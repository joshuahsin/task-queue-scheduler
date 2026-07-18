# Build context is the repo root (not this directory) — a Maven multi-module reactor needs the
# parent pom and every declared module's pom.xml present, even though task-worker's own source
# isn't needed to build scheduler-api. Build with: docker build -f scheduler-api/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY scheduler-api/pom.xml scheduler-api/pom.xml
COPY task-worker/pom.xml task-worker/pom.xml
COPY scheduler-api/src scheduler-api/src
RUN mvn -q -pl scheduler-api -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/scheduler-api/target/*.jar app.jar
EXPOSE 8080
# Exec form is required, not shell form — this makes the JVM PID 1 so it receives SIGTERM directly
# on `kubectl delete pod`/rolling updates, letting Spring Boot shut down gracefully (finish in-flight
# requests, close the DB/Redis connection pools) instead of an intermediate shell process eating the
# signal and forcing a SIGKILL after the grace period.
ENTRYPOINT ["java", "-jar", "app.jar"]
