FROM openjdk:17-slim AS build
WORKDIR /app
COPY . .
RUN apt-get update && apt-get install -y curl unzip
RUN curl -L https://services.gradle.org/distributions/gradle-8.5-bin.zip -o gradle.zip
RUN unzip gradle.zip && rm gradle.zip
ENV PATH="/app/gradle-8.5/bin:${PATH}"
RUN gradle build --no-daemon

FROM openjdk:17-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
