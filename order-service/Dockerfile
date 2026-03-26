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
COPY --from=build /app/src/main/resources/openapi /app/openapi
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]