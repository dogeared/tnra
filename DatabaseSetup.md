# Database Setup

These instructions assume you have an existing MySQL server with root access.

## 1. Connect to MySQL as root

```bash
mysql -u root -p
```

## 2. Create the database

```sql
CREATE DATABASE tnra;
```

## 3. Create the user and grant access

```sql
CREATE USER 'tnra' IDENTIFIED BY '<your_password>';
GRANT ALL PRIVILEGES ON tnra.* TO 'tnra'@'%';
FLUSH PRIVILEGES;
```

Replace `<your_password>` with a secure password of your choice.

## 4. Verify

```bash
mysql -u tnra -p tnra
```

You should be connected to the `tnra` database as the `tnra` user.

## 5. Start over

To drop the database and remove the user, connect as root and run:

```sql
DROP DATABASE tnra;
DROP USER 'tnra'@'%';
```
