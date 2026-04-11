FROM eclipse-temurin:19-jdk AS builder
WORKDIR /app
COPY gradle/wrapper gradle/wrapper
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY ./config config/
RUN ./gradlew --no-daemon dependencies
COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:19-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]



#FROM eclipse-temurin:19-jdk AS builder
#WORKDIR /app
#
#COPY gradle gradle
#COPY build.gradle.kts settings.gradle.kts gradlew ./
#COPY ./config config/
#
#RUN ./gradlew --no-daemon dependencies || return 0
#
#COPY ./src src/
#
#RUN ./gradlew --no-daemon clean build -x test
#
#FROM eclipse-temurin:19-jre-alpine
#WORKDIR /app
#
#COPY --from=builder /app/build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
#
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]




#FROM eclipse-temurin:19-jdk AS builder
#WORKDIR /
#
#COPY gradle gradle
#COPY build.gradle.kts .
#COPY settings.gradle.kts .
#COPY gradlew .
#COPY ./config config
#
#RUN ./gradlew --no-daemon dependencies || return 0
#COPY ./src src
#
#RUN ./gradlew --no-daemon clean build -x test
#
#FROM eclipse-temurin:19-jre-alpine
#WORKDIR /
#COPY --from=builder /build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
#EXPOSE 8080
#
#ENTRYPOINT ["java", "-jar", "app.jar"]
