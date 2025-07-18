FROM eclipse-temurin:17-jammy
VOLUME /tmp
ADD target/*.jar app.jar
ADD uploads uploads
ENTRYPOINT ["java","-jar","/app.jar"]
