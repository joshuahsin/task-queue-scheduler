# Build context is the repo root (not this directory) — a Maven multi-module reactor needs the
# parent pom and every declared module's pom.xml present, even though scheduler-api's own source
# isn't needed to build task-worker. Build with: docker build -f task-worker/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY task-worker/pom.xml task-worker/pom.xml
COPY scheduler-api/pom.xml scheduler-api/pom.xml
COPY task-worker/src task-worker/src
RUN mvn -q -pl task-worker -am package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/task-worker/target/*.jar app.jar
# Exec form is required, not shell form — this makes the JVM PID 1 so it receives SIGTERM directly
# on `kubectl delete pod`/rolling updates. WorkerShutdownHandler's @PreDestroy deregister call only
# fires on a real Spring context shutdown; with shell-form (`CMD java -jar app.jar`), the shell
# becomes PID 1, silently swallows SIGTERM, and the JVM gets SIGKILLed after the grace period
# without ever running that hook — this app would just look like a crash instead of a clean exit.
ENTRYPOINT ["java", "-jar", "app.jar"]
