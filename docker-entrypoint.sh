#!/bin/sh
# Fix ownership of the bind-mounted uploads directory so appuser can write to it.
# Runs as root (no USER in Dockerfile) then drops privileges before starting the app.
chown appuser:root /uploads
exec runuser -u appuser -- java -jar /app.jar
