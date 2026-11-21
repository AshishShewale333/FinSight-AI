# Build the Spring Boot application with Java 17.
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -Dmaven.test.skip=true package

# Run the packaged application as an unprivileged user.
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
RUN groupadd --system finsight && useradd --system --gid finsight --create-home finsight \
    && mkdir -p /app/data/documents /app/data/nse \
    && chown -R finsight:finsight /app
COPY --from=build --chown=finsight:finsight /workspace/target/finsight-ai-0.1.0-SNAPSHOT.jar /app/app.jar

USER finsight
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
