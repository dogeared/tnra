## Hosting Env setup

1. Update
    * `apt-get update`

1. Create an unprivileged user
    * `adduser <username>`

1. Install Docker
    * `apt install docker`

1. Install docker-compose
    * `apt install docker-compose`

1. Give permission to unprivileged user for docker
    * `usermod -aG docker <username>`

1. Install git
    * `apt install git`

1. Install maven
    * `apt install maven`

1. Clone the repo
    * `git clone https://github.com/dogeared/tnra`

1. setup the .env file
    * `~/tnra/.env`

1. test build the project
    * `mvn clean install -DskipTests=true`

1. set up certs
    * `scp fullchain.pem privkey.pem <user>@<ip>:~/`

1. put certs in right place
    ```
    mv privkey.pem tnra/nginx/.cert/key.pem
    mv fullchain.pem tnra/nginx/.cert/cert.pem
    ```

1. imoprt database
    * `mysql -u root -p -h 127.0.0.1 -P 3307 tnra < <dump.sql>`