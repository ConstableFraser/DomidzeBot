FROM eclipse-temurin:21-jdk
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
EXPOSE 8080

CMD java -jar build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar

#FROM gradle:7.6-jdk19 AS builder
#WORKDIR /
#COPY gradle gradle
#COPY build.gradle.kts .
#COPY settings.gradle.kts .
#COPY gradlew .
#COPY ./src src
#COPY ./config config
#ENV JAVA_OPTS="-Xmx4g -Xms1024M"
#RUN ./gradlew --no-daemon --stacktrace dependencies
#RUN ./gradlew --no-daemon --stacktrace build
#COPY ./build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8080
#ENTRYPOINT ["java","-jar","/app.jar"]
