#FROM gradle:8.0-jdk19
#WORKDIR /
#COPY / .
#RUN ./gradlew installDist
#CMD ./build/install/app/bin/app

# Use an official Java runtime as a parent image
#FROM gradle:8.0-jdk19

# Set the working directory in the container
#WORKDIR /

# Copy the local jar file into the container
#COPY / app.jar

# Specify the command to run the application
#CMD ["java", "-jar", "app.jar"]

FROM gradle:8.11-jdk21-corretto
WORKDIR /
COPY / .
RUN ./gradlew build
CMD ./build/install/libs


#=====READY========
#FROM openjdk:19
#COPY ./build/libs/DomidzeBot-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java","-jar","/app.jar"]
