FROM eclipse-temurin:17-jammy as build
RUN ./mvnw install -DskipTests

FROM eclipse-temurin:17-jammy
VOLUME /tmp
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]