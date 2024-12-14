FROM gradle:7.6-jdk19 AS builder
WORKDIR /
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradlew .
COPY ./src src
COPY ./config config
RUN ./gradlew --no-daemon dependencies
RUN ./gradlew --no-daemon build
ENV JAVA_OPTS="-Xmx512M -Xms512M"
COPY build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]

#FROM openjdk:19
#WORKDIR /
#COPY ./distr/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","/app.jar"]
