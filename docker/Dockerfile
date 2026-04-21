# syntax=docker/dockerfile:1.7

FROM gradle:8.5-jdk17 AS build

WORKDIR /app

COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle settings.gradle gradle.properties ./
COPY common/build.gradle common/build.gradle
COPY app/build.gradle app/build.gradle

RUN chmod +x gradlew
RUN --mount=type=secret,id=gh_token,required=false \
    --mount=type=secret,id=github_actor,required=false \
    if [ -f /run/secrets/gh_token ]; then export GH_TOKEN="$(cat /run/secrets/gh_token)"; fi; \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi; \
    ./gradlew :app:dependencies :common:dependencies --no-daemon

COPY . .

RUN --mount=type=secret,id=gh_token,required=false \
    --mount=type=secret,id=github_actor,required=false \
    if [ -f /run/secrets/gh_token ]; then export GH_TOKEN="$(cat /run/secrets/gh_token)"; fi; \
    if [ -f /run/secrets/github_actor ]; then export GITHUB_ACTOR="$(cat /run/secrets/github_actor)"; fi; \
    ./gradlew :app:build --no-daemon -x test

FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

COPY --from=build /app/app/build/libs/*.jar app.jar

ENV SPRING_PROFILES_ACTIVE=dev

ENTRYPOINT ["java", "-jar", "app.jar"]
