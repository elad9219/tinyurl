# Stage 1: Build the application
FROM openjdk:11-jdk-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests && apt-get clean && rm -rf /var/lib/apt/lists/*

# Stage 2: Run the application
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/target/tinyurl-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java", "-jar", "tinyurl-0.0.1-SNAPSHOT.jar"]