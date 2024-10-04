# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-alpine

# Set the working directory inside the container
WORKDIR /usr/src/app

ENV HTTP_SERVER_PORT=4000

# Copy the current directory contents into the container at /usr/src/app
COPY http-request-logger.jar .

# Specify the command to run the application
CMD java -jar http-request-logger.jar
