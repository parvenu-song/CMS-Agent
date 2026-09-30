FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
RUN mvn -B -ntp verify
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S cms && adduser -S cms -G cms
WORKDIR /app
COPY --from=build /build/target/cms-agent-api-0.1.0.jar app.jar
USER cms
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
