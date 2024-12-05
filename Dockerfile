FROM gradle:8.0-jdk19

ARG JAR_FILE=./build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","app.jar"]