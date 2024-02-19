FROM eclipse-temurin:11-jdk
#FROM adoptopenjdk/openjdk11:alpine-jre
VOLUME /tmp
EXPOSE 5026
EXPOSE 8026
ARG JAR_FILE=target/mapper-1.0.0-SNAPSHOT.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-Dspring.profiles.active=docr", "-jar","/app.jar"]
