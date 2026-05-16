FROM eclipse-temurin:21-jammy
RUN useradd -r -u 1001 -g root appuser
VOLUME /tmp
ADD target/*.jar app.jar
USER appuser
ENTRYPOINT ["java", "-jar", "/app.jar"]
