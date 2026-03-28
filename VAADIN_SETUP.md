# Vaadin Flow Frontend Setup

This document describes the Vaadin Flow frontend that has been added to the TNRA Spring Boot application with OIDC authentication.

## Features

- **Vaadin Flow Frontend**: Modern web UI built with Vaadin Flow
- **OIDC Authentication**: Integration with Keycloak for authentication using authorization code flow
- **Responsive Design**: Clean, modern interface that works on all devices
- **Anonymous Access**: Unauthenticated users can view the login page
- **Secure Routes**: Protected routes require authentication

## Configuration

### OIDC Settings

The application is configured to use Keycloak for OIDC authentication. Set these environment variables for non-local deployments:

```bash
export KEYCLOAK_CLIENT_ID=tnra-app
export KEYCLOAK_CLIENT_SECRET=your-client-secret
export KEYCLOAK_ISSUER_URI=https://your-keycloak-server/realms/tnra
```

For local development, defaults point to `http://localhost:8180/realms/tnra` (Docker Compose Keycloak).

### Local Keycloak Setup

1. Start Keycloak: `docker compose up keycloak -d`
2. The `tnra` realm is auto-imported with pre-configured client and test users
3. Admin console: `http://localhost:8180/admin` (admin/admin)
4. Test users: `admin@tnra.local` / `admin` (admin role), `member@tnra.local` / `member`

## Running the Application

### Local Development

1. Start Keycloak: `docker compose up keycloak -d`
2. Run the Spring Boot application:
   ```bash
   ./mvnw spring-boot:run
   ```
3. Access the application at: `http://localhost:8080`

### Production with NGINX

1. The application is configured to work with the existing NGINX setup
2. Access the application at: `https://tnra.afitnerd.local`
3. NGINX will proxy requests to the Spring Boot application

## Application Structure

### Main Components

- **MainView**: Entry point with login/logout functionality
- **ErrorView**: Handles authentication errors and other issues
- **SpringSecurityConfig**: Security configuration for OIDC and Vaadin
- **VaadinConfig**: Vaadin-specific configuration

### Routes

- `/` - Main application view (anonymous access allowed)
- `/main` - Alias for main view
- `/error` - Error handling page
- `/oauth2/authorization/keycloak` - OIDC authorization endpoint
- `/login/oauth2/code/keycloak` - OIDC callback endpoint
- `/logout` - Logout endpoint

## Testing

### Unauthenticated Access

1. Visit `http://localhost:8080`
2. You should see the welcome page with a "Login" button
3. The page should be accessible without authentication

### Authentication Flow

1. Click the "Login" button
2. You will be redirected to Keycloak for authentication
3. After successful authentication, you'll be redirected back to the application
4. The page should show a welcome message with your name and a logout button

### Logout

1. Click the "Logout" button when authenticated
2. You should be logged out and redirected to the main page
3. The page should show the login button again

## Troubleshooting

### Common Issues

1. **OIDC Configuration**: Ensure Keycloak is running (`docker compose up keycloak -d`)
2. **Redirect URI**: The realm export configures `http://localhost:8080/*` as the redirect URI
3. **CORS Issues**: The application is configured to work with the existing NGINX setup
4. **Vaadin Resources**: Ensure Vaadin resources are properly served (handled automatically)

### Debug Logging

The application includes debug logging for Spring Security and OIDC. Check the logs for:
- OIDC authentication flow
- Vaadin routing
- Security configuration

## Security Considerations

- Authorization code flow with client secret (confidential client)
- Session management is handled by Spring Security
- CSRF protection is configured for Vaadin
- All sensitive endpoints require authentication
- Logout properly invalidates sessions and clears cookies
