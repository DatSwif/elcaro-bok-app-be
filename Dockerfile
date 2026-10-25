FROM eclipse-temurin:25-jre
COPY application/target/elcaro-bok-app-application-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
