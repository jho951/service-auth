FROM gradle:8.5-jdk17 AS build

WORKDIR /app

ARG GITHUB_ACTOR
ARG GITHUB_TOKEN
ENV GITHUB_ACTOR=${GITHUB_ACTOR}
ENV GITHUB_TOKEN=${GITHUB_TOKEN}

COPY gradle gradle
COPY gradlew gradlew
COPY build.gradle settings.gradle gradle.properties ./
COPY common/build.gradle common/build.gradle
COPY app/build.gradle app/build.gradle

RUN chmod +x gradlew
RUN ./gradlew :app:dependencies :common:dependencies --no-daemon

COPY . .

RUN ./gradlew :app:build --no-daemon -x test

FROM amazoncorretto:17-alpine-jdk
WORKDIR /app

COPY --from=build /app/app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=dev"]
