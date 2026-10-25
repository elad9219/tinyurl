# Stage 1: Build the Spring Boot application
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
COPY src ./src

# Prevent accidental packaging of local credentials
RUN ! find src -type f \
        \( -name 'application.properties' \
        -o -name 'secure-connect*.zip' \) \
        | grep -q .

# The React frontend is deployed separately on Vercel
RUN rm -rf src/main/resources/static

# Build the executable JAR
RUN mvn --batch-mode --no-transfer-progress \
    clean package -DskipTests

# Stage 2: Java 21 runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

# Run the application as a non-root user
RUN groupadd --system appgroup \
    && useradd --system \
       --gid appgroup \
       appuser

COPY --from=build \
    --chown=appuser:appgroup \
    /app/target/tinyurl-0.0.1-SNAPSHOT.jar \
    /app/tinyurl.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/tinyurl.jar"]
