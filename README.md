## Running locally

The app is set to use H2 when run locally, so you'll need to re-insert records when restarting.

Here's a list of the env variables you'll need:

```
MAILGUN_URL
MAILGUN_KEY_PRIVATE
MAILGUN_KEY_PUBLIC
TNRA_SLACK_ACCESS_TOKEN
TNRA_SLACK_TOKEN
```

run from IntelliJ or command line and the app will listen on port 8080.

## Testing from slack

run ngrok to setup public url:

```
ngrok http --domain=micah-silverman.ngrok.io 8080
```

navigate to the slack app settings: `https://api.slack.com/apps/A5GLLLLG6/slash-commands`. Update the `post_test` slash
command to point to your local instance: `https://micah-silverman.ngrok.io/api/v1/post`

You can now issue commands in slack and they will be sent to your locally running instance:

```
/post_test rep int wid test text for widwytk
```

## Testing from the command line

You can make a POST request with HTTPie like:

```
http POST :8080/api/v1/post \
token=<slack token env var> \
team_id=T1DLRUNN8 \
team_domain=tnra \
channel_id=D1DN187CH \
channel_name=directmessage \
user_id=U1DMD2Z8A \
user_name=afitnerd \
response_url=https://hooks.slack.com/commands/T1DLRUNN8/1522759899702/WNXLruMwCJKUT9VhBODbIZwP \
trigger_id=1553333092576.47705974756.7e452cdd0aab5a165b1b7dfdd3f9dba5 \
command=/post_test \
text="start"
```

You can also use the `local_post_test.sh` script as:

```
./local_post_test.sh <slack token env var> http://localhost:8080
```

The first param is the slack token and the second param is the url

to do a complete test post locally

## Vultr Configuration

```
systemctl status tnra.service
```

/etc/systemd/system/tnra.service

docker-compose logs tnra
docker-compose logs -f server

## Refresh the SSL cert

### On local machine

```
certbot certonly \
  --config-dir ~/letsencrypt/config \
  --work-dir ~/letsencrypt/work \
  --logs-dir ~/letsencrypt/logs \
  --dns-cloudflare --dns-cloudflare-credentials ~/cloudflare-creds.ini \
  -d tnra.afitnerd.com
scp \
  /Users/micahsilverman/letsencrypt/config/live/tnra.afitnerd.com/fullchain.pem \
  /Users/micahsilverman/letsencrypt/config/live/tnra.afitnerd.com/privkey.pem \
  tnra@108.61.192.65:~/
```

## On Vultr

```
mv privkey.pem tnra/nginx/.cert/key.pem
mv fullchain.pem tnra/nginx/.cert/cert.pem
```

## Helpful info

To dump the database:

```
mysqldump -h <host> -u<username> -p --skip-column-statistics  --no-tablespaces <database name> > ~/tnra.sql
```