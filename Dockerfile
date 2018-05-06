FROM java:8-jdk-alpine
ADD target/scala-2.12/sportadvisor-api.jar sportadvisor-api.jar
ENTRYPOINT ["java", "-jar", "sportadvisor-api.jar"]