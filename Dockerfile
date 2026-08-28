# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Build stage — compile and package the app with a full JDK + Maven.
# No Maven wrapper is bundled, so use the maven image.
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so this layer is cached until pom.xml changes.
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline

# Build the jar (CI runs the tests against MySQL; skip them here).
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Runtime stage — slim JRE, non-root user, just the jar.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

COPY --from=build /build/target/meditrack-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
