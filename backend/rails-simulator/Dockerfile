# syntax=docker/dockerfile:1.7
# Build from the repository root:  docker build -f rails-simulator/Dockerfile -t rails-simulator:local .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY . .
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -pl rails-simulator -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --home /app app
WORKDIR /app
COPY --from=build /src/rails-simulator/target/app.jar /app/app.jar
USER app
EXPOSE 8089
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
