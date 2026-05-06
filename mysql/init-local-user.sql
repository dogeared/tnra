-- Creates the 'tnra' database user matching application.yml defaults.
-- Runs automatically on first MySQL container start (docker-entrypoint-initdb.d).
-- If you already have a MySQL volume with data, run: docker compose down -v
CREATE USER IF NOT EXISTS 'tnra'@'%' IDENTIFIED BY '123456aA$';
GRANT ALL PRIVILEGES ON `tnra`.* TO 'tnra'@'%';
FLUSH PRIVILEGES;
