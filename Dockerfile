# One image with the API and the React client, served from the same origin (no CORS needed).
#   docker build -t leaveflow .
# Run it with a database: see compose.demo.yaml.

# 1. React client
FROM node:22-alpine AS frontend
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# 2. Spring Boot jar, with the client's build as static resources
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q dependency:go-offline
COPY src/ src/
COPY --from=frontend /frontend/dist/ src/main/resources/static/
# tests run in CI against Testcontainers; the image build only packages
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -q package -DskipTests -Djacoco.skip=true \
    && java -Djarmode=tools -jar target/leaveflow-*.jar extract --layers --launcher --destination extracted

# 3. Runtime: JRE only, unprivileged user, dependencies in their own layers so code changes stay small
FROM eclipse-temurin:21-jre
RUN groupadd --system leaveflow && useradd --system --gid leaveflow leaveflow
WORKDIR /app
COPY --from=backend /app/extracted/dependencies/ ./
COPY --from=backend /app/extracted/spring-boot-loader/ ./
COPY --from=backend /app/extracted/snapshot-dependencies/ ./
COPY --from=backend /app/extracted/application/ ./
USER leaveflow
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -fs http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
