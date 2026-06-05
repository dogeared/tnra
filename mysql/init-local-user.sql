-- Creates the 'tnra' database user matching application.yml defaults.
-- Runs automatically on first MySQL container start (docker-entrypoint-initdb.d).
-- If you already have a MySQL volume with data, run: docker compose down -v
CREATE USER IF NOT EXISTS 'tnra'@'%' IDENTIFIED BY '123456aA$';
GRANT ALL PRIVILEGES ON `tnra`.* TO 'tnra'@'%';

-- The landing app (tnra-landing-app) uses its own database. Pre-create it and grant the
-- same `tnra` user so the landing app also runs zero-config from the IDE in dev.
CREATE DATABASE IF NOT EXISTS `tnra_landing`;
GRANT ALL PRIVILEGES ON `tnra_landing`.* TO 'tnra'@'%';

FLUSH PRIVILEGES;
