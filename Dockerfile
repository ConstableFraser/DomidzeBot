FROM openjdk:19
WORKDIR /
COPY ./distr/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]

#WORKDIR /backend
#
#COPY gradle gradle
#COPY build.gradle.kts .
#COPY settings.gradle.kts .
#COPY gradlew .
#
#RUN ./gradlew --no-daemon dependencies
#
#COPY lombok.config .
#COPY src src
#
#RUN ./gradlew --no-daemon build
#
#ENV JAVA_OPTS "-Xmx512M -Xms512M"
#EXPOSE 8080
#
#CMD java -jar build/libs/HexletSpringBlog-1.0-SNAPSHOT.jar