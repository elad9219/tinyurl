# Stage 1: Build the application
FROM eclipse-temurin:11-jdk AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Remove old frontend static files so they are not packaged into the JAR
RUN rm -rf src/main/resources/static

RUN apt-get update && apt-get install -y maven && mvn clean package -DskipTests && apt-get clean && rm -rf /var/lib/apt/lists/*

# Stage 2: Run the application
FROM eclipse-temurin:11-jre
WORKDIR /app
COPY --from=build /app/target/tinyurl-0.0.1-SNAPSHOT.jar .
EXPOSE 8080
CMD ["java", "-jar", "tinyurl-0.0.1-SNAPSHOT.jar"]