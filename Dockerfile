FROM gradle:7.6-jdk19 AS builder
WORKDIR /
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradlew .
COPY ./src src
COPY ./config config
ENV JAVA_OPTS="-Xmx4g -Xms1024M"
RUN ./gradlew --no-daemon --stacktrace dependencies
RUN ./gradlew --no-daemon --stacktrace build
COPY ./build build
COPY build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
