FROM eclipse-temurin:21-jammy
RUN useradd -r -u 1001 -g root appuser
VOLUME /tmp
ADD target/*.jar app.jar
RUN mkdir -p /uploads && chown appuser:root /uploads
VOLUME /uploads
USER appuser
ENTRYPOINT ["java","-jar","/app.jar"]
