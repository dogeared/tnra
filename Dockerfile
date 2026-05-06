FROM eclipse-temurin:21-jammy
RUN useradd -r -u 1001 -g root appuser
VOLUME /tmp
ADD target/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh && mkdir -p /uploads
VOLUME /uploads
ENTRYPOINT ["/docker-entrypoint.sh"]
