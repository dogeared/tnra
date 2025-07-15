# Nginx Subdomain Configuration

This directory contains nginx configuration templates for routing traffic based on subdomains.

## Configuration Files

### Production Configuration
- **`nginx-subdomain.conf.template`**: Routes traffic to Docker containers
  - `tnra.afitnerd.local` → `server:8080`
  - `tnra-new.afitnerd.local` → `server:8081`

### Local Development Configuration
- **`nginx-subdomain-local.conf.template`**: Routes traffic to localhost
  - `tnra.afitnerd.local` → `host.docker.internal:8080`
  - `tnra-new.afitnerd.local` → `host.docker.internal:8081`

## Features

- **SSL/HTTPS Support**: Both configurations include SSL certificate handling
- **HTTP to HTTPS Redirect**: Automatic redirect from HTTP to HTTPS
- **Proxy Headers**: Proper forwarding of headers for web applications
- **WebSocket Support**: Includes WebSocket upgrade handling

## Usage

### For Production

1. Copy the template to your nginx configuration:
   ```bash
   cp nginx/templates/nginx-subdomain.conf.template /etc/nginx/sites-available/tnra-subdomains
   ```

2. Enable the site:
   ```bash
   ln -s /etc/nginx/sites-available/tnra-subdomains /etc/nginx/sites-enabled/
   ```

3. Test and reload nginx:
   ```bash
   nginx -t
   systemctl reload nginx
   ```

### For Local Development

1. Use the local template with Docker:
   ```bash
   # Update your docker-compose.yml to use the local template
   environment:
     - NGINX_TEMPLATE=nginx-subdomain-local.conf.template
   ```

2. Restart the nginx container:
   ```bash
   docker-compose restart nginx
   ```

## DNS Configuration

Make sure your DNS records point both subdomains to your server:

```
tnra.afitnerd.local     A     YOUR_SERVER_IP
tnra-new.afitnerd.local A     YOUR_SERVER_IP
```

## SSL Certificates

The configuration expects SSL certificates at:
- `/etc/nginx/certs/cert.pem`
- `/etc/nginx/certs/key.pem`

For local development, you can generate self-signed certificates or use tools like mkcert.

## Testing

Test the configuration by accessing:
- https://tnra.afitnerd.local (should route to port 8080)
- https://tnra-new.afitnerd.local (should route to port 8081)

Both should automatically redirect from HTTP to HTTPS. 