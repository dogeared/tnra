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

## Third prompt - stats

```
Add a nav menu that has the login and logout items as well as a Stats item that goes to a new view called Stats. The Stats menu item should only be available if the user is authenticated. The Stats view should show sliders for each of the stats in the Post model. If there isn't an in-porgress post, a new one should be created. Once there is an in-porgress post, changes to the stats sliders should automatically persist to the database.
```

## Fourth prompt - menu/nav

Simplified previous request. Interestingly, it created a Stats view on its own...

```
create a collapsible menu. Relocate the login button to the menu. When the user is authenticated, this should change to the logout button.
```

It took a few refinements to get it right:

```
The nav is working properly. However, it is not collapsible
```

```
There's no icon to click to expand or collapse the menu
```

You can see the changes by diffing from commit 68a26cd to aa2dfd2

## Fifth prompt - Stats

```
update the Stats view to show the stats section of the Post model. Each stat should be represented by a control that has an entry field with an up and down arrow on each side. The entry field should be set to 0 initially. If there is no Post record that with PostState IN_PROGRESS, a new Post should be created when the user switches to this view. When the up arrow is pressed for a stat, it should increment the value. If the down arrow is pressed it should decrement the value. The value should not drop below 0 and should not exceed 99. The user can enter a value directly, but only numbers should be entered.
```

This came up with a view that had an error - it was creating a new Uer object rather than looking it up.

I manually created a UserService that uses the UserRepository to look up the authenticated user. That fixed the error. 

But, the up and down arrows were appearing separately from the input fields. It took a few more prompts to get it to render correctly.

## Post prompt

```
Create a PostView. At the top there should be a pick list of available posts. The items in the list should be the available end dates of each post for the authenticated user.

The rest of the view will contain all the elements from the Post model.

The view should be divided into sections:

The Intro section has thre inputs: a textarea for what i don't want you to know (widwytk), an input for kryptonite and a text area for what and when

The next three sections are Personal, Family and Work. Each has two textareas: one for Best and one for Worst

The final section is the stats setion and should be an embedded version of the StatsView.

There should be a Start New Post button at the top to start a new post IF there isn't one already in progress.
```


## Dev Notes

Speak to the importance of an ErrorView and logging the issue