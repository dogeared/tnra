# Vaadin Flow Frontend Setup

This document describes the Vaadin Flow frontend that has been added to the TNRA Spring Boot application with OIDC authentication.

## Features

- **Vaadin Flow Frontend**: Modern web UI built with Vaadin Flow
- **OIDC Authentication**: Integration with Okta for authentication using PKCE flow
- **Responsive Design**: Clean, modern interface that works on all devices
- **Anonymous Access**: Unauthenticated users can view the login page
- **Secure Routes**: Protected routes require authentication

## Configuration

### OIDC Settings

The application is configured to use Okta for OIDC authentication. Update the following environment variables:

```bash
export OKTA_CLIENT_ID=your-okta-client-id
export OKTA_CLIENT_SECRET=your-okta-client-secret
export OKTA_ISSUER_URI=https://your-domain.okta.com/oauth2/default
```

### Okta Application Setup

1. Create a new OIDC application in your Okta developer console
2. Set the redirect URI to: `https://tnra.afitnerd.local/login/oauth2/code/okta`
3. Enable PKCE (Proof Key for Code Exchange) for enhanced security
4. Configure the scopes: `openid`, `profile`, `email`

## Running the Application

### Local Development

1. Set the environment variables for Okta configuration
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
- `/oauth2/authorization/okta` - OIDC authorization endpoint
- `/login/oauth2/code/okta` - OIDC callback endpoint
- `/logout` - Logout endpoint

## Testing

### Unauthenticated Access

1. Visit `https://tnra.afitnerd.local`
2. You should see the welcome page with a "Login with OIDC" button
3. The page should be accessible without authentication

### Authentication Flow

1. Click the "Login with OIDC" button
2. You will be redirected to Okta for authentication
3. After successful authentication, you'll be redirected back to the application
4. The page should show a welcome message with your name and a logout button

### Logout

1. Click the "Logout" button when authenticated
2. You should be logged out and redirected to the main page
3. The page should show the login button again

## Troubleshooting

### Common Issues

1. **OIDC Configuration**: Ensure all environment variables are set correctly
2. **Redirect URI**: Verify the redirect URI matches your Okta application configuration
3. **CORS Issues**: The application is configured to work with the existing NGINX setup
4. **Vaadin Resources**: Ensure Vaadin resources are properly served (handled automatically)

### Debug Logging

The application includes debug logging for Spring Security and OIDC. Check the logs for:
- OIDC authentication flow
- Vaadin routing
- Security configuration

## Security Considerations

- PKCE flow is used for enhanced security
- Session management is handled by Spring Security
- CSRF protection is configured for Vaadin
- All sensitive endpoints require authentication
- Logout properly invalidates sessions and clears cookies 