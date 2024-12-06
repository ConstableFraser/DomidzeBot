FROM openjdk:19
WORKDIR /
COPY ./distr/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
