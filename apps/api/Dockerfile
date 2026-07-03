FROM gradle:8.14.2-jdk21 AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY apps/api ./apps/api
RUN gradle --no-daemon :apps:api:bootJar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/apps/api/build/libs/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
