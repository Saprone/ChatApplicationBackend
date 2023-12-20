FROM openjdk:17-jdk
ARG JAR_FILE=target/*.jar
COPY ./target/ChatApplication-0.0.1.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java","-jar","/app.jar"]