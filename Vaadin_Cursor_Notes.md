## Step 0

Take the current project and remove the src/frontend folder and all frontend build references in the pom.xml

## First prompt

```
This is a backend api project that uses OAuth2 for authorization. Add a Vaadin Flow frontend that has default view with a login button. The vaadin flow app should be a pwa. The login should use the redirect PKCE flow from OIDC. The spring boot application is already configured for this. NGINX is already set up to receive requests for the app on: https://tnra.afitnerd.local and this is the url that should be used to test the vaadin flow app.
```

## Additional prompts

The page was coming up blank. It took a number of tries to get it to render properly. The thing that fixed it was:

```
I see a blank page when I navigate to the app. There are no errors in the console.
```

### Still having trouble - going to reset and try again

## Second prompt

(Leaving out the PWA part)

```
This is a backend api project that uses OIDC for authn and authz. Add a Vaadin Flow frontend that has default unauthenticated view with a login button. The login should use the PKCE flow from OIDC. The spring boot application is already configured for this. NGINX is already set up to receive requests for the app on: https://tnra.afitnerd.local and this is the url that should be used to test the vaadin flow app.
```

Lots of wrestling with cursor and the application.yml
