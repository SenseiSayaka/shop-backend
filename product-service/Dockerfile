FROM gradle:8.5-jdk17 AS build
WORKDIR /app
COPY gradle gradle
COPY gradlew .
COPY build.gradle.kts .
COPY src ./src
RUN chmod +x gradlew && ./gradlew shadowJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]