This repo is set up to run all components of the app in a docker container. It uses `docker compose` to run multiple
containers from a single configuration file.

## Configuration

There are three components to the app running in Docker:

1. nginx proxy
2. mysql
3. app server

### NGINX configuration

In order for NGINX to proxy HTTPS requests to the app server, a certificate and private key file must be configured for
it.

**NOTE**: These files should NOT be checked into the git repo. 

[certbot](https://certbot.eff.org/) is a handy way to generate and renew the cert files. 

A comprehensive guide to using certbot is outside the scope of this doc, but here's some notes on how to configure it
on macos and in conjunction with cloudflare.

Execute the following to get certbot setup:

```
brew install certbot
pip3 install certbot-dns-cloudflare
```

Here's an example of running the command on mac:

```
certbot certonly \
  --config-dir ~/letsencrypt/config \
  --work-dir ~/letsencrypt/work \
  --logs-dir ~/letsencrypt/logs \
  --dns-cloudflare \
  --dns-cloudflare-credentials ~/cloudflare-creds.ini \
-d tnra.afitnerd.com
```

On the host where you'll be running the application, put the cert and key files in: `nginx/.cert/cert.pem` and
`nginx/.cert/key.pem`.

### MYSQL

No additional configuration is necessary for the MySQL server to start up in Docker. By default it's configured with
username and password set to `root`.

### App Server

You'll need to create a file named `.env` in the root of the app on the host machine. It will need to have each of the
environment variables found in the `.env.template` file set.

## Running on Linux as a Service

Make sure the Docker service is installed and running. 

Create an unpriveleged user and group. Make sure this user belongs to the docker group so it can run docker commands:

```
usermod -aG docker <user>
```

Create a symbolic link to the `systemd` service files. For example:

```
ln -s /home/tnra/tnra/tnra.service  /etc/systemd/system/tnra.service
```

**NOTE**: The `tnra.service` file is in the git repo

Install the system service:

```
systemctl enable tnra.service
```

Start the system service:

```
systemctl start tnra.service
```

This will execute the `tnra.start.sh` script which in turns references the `.env` you set up earlier.

**NOTE**: You can stop the three docker containers using the `tnra.stop.sh` script.

Finally, you'll need to configure your hosting service firewall to allow HTTPS connections (on port `443`) and you'll 
need to configure DNS to resolve the IP address of your host. 