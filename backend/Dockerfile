FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace
COPY gradle gradle
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S atlas && adduser -S atlas -G atlas
COPY --from=build /workspace/build/libs/*.jar /app/app.jar
USER atlas
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
