package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.AuthNavigationService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@CssImport("./styles/theme.css")
public class MainLayout extends AppLayout {

    static final String DARK_MODE_COOKIE = "tnra-dark-mode";
    private static final int COOKIE_MAX_AGE = 365 * 24 * 60 * 60; // 1 year

    private final OidcUserService oidcUserService;
    private final UserService userService;
    private final AuthNavigationService authNavigationService;
    private Button themeToggleButton;
    private boolean darkMode;

    public MainLayout(
        OidcUserService oidcUserService,
        UserService userService,
        AuthNavigationService authNavigationService
    ) {
        this.oidcUserService = oidcUserService;
        this.userService = userService;
        this.authNavigationService = authNavigationService;
        createHeader();
        createDrawer();

        setDrawerOpened(false);
        setPrimarySection(Section.DRAWER);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // Determine initial dark mode state
        darkMode = resolveInitialDarkMode();

        // Apply to the UI's theme list (sets theme="dark" on <html>)
        applyTheme(darkMode);

        // Update the toggle icon to match
        updateToggleIcon();
    }

    /**
     * Resolve initial dark mode: prefer authenticated user's DB preference,
     * fall back to the cookie value.
     */
    private boolean resolveInitialDarkMode() {
        if (oidcUserService.isAuthenticated()) {
            try {
                User user = userService.getCurrentUser();
                if (user != null && user.getDarkMode() != null) {
                    return user.getDarkMode();
                }
            } catch (Exception ignored) {
                // fall through to cookie
            }
        }
        return readDarkModeCookie();
    }

    private void applyTheme(boolean dark) {
        getUI().ifPresent(ui -> {
            // Set on <html> so global CSS custom-property overrides
            // (html[theme~="dark"]) take effect for our --tnra-* tokens
            // and comprehensive Lumo variable overrides.
            ui.getPage().executeJs(
                "document.documentElement.setAttribute('theme', $0)",
                dark ? "dark" : ""
            );
        });
    }

    private void createHeader() {
        DrawerToggle drawerToggle = new DrawerToggle();

        H1 logo = new H1("TNRA");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE,
            LumoUtility.Margin.MEDIUM);

        // Theme toggle button — icon swaps between sun and moon
        themeToggleButton = new Button();
        themeToggleButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeToggleButton.addClassName("theme-toggle-button");
        themeToggleButton.getElement().setAttribute("title", "Toggle dark mode");
        themeToggleButton.setIcon(VaadinIcon.MOON_O.create()); // default icon
        themeToggleButton.addClickListener(e -> toggleTheme());

        // Auth button
        Button authButton;
        if (oidcUserService.isAuthenticated()) {
            authButton = new Button("Logout", VaadinIcon.SIGN_OUT.create(), e -> {
                openLogoutDialog();
            });
        } else {
            authButton = new Button("Login", VaadinIcon.SIGN_IN.create(), e -> {
                getUI().ifPresent(ui -> ui.getPage().setLocation(authNavigationService.getLoginPath()));
            });
        }
        authButton.addClassNames(LumoUtility.Margin.MEDIUM);

        // Right side: toggle + auth button
        HorizontalLayout rightSection = new HorizontalLayout(themeToggleButton, authButton);
        rightSection.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSection.setSpacing(true);

        // Left side: hamburger + logo grouped together
        HorizontalLayout leftSection = new HorizontalLayout(drawerToggle, logo);
        leftSection.setAlignItems(FlexComponent.Alignment.CENTER);
        leftSection.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(leftSection, rightSection);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
    }

    private void openLogoutDialog() {
        Dialog dialog = new Dialog();
        dialog.addClassName("logout-dialog");
        dialog.setHeaderTitle("Log out?");
        dialog.add(new Paragraph("You are about to end your current session."));

        Button cancelButton = new Button("Cancel", e -> dialog.close());
        Button confirmButton = new Button("Logout", e -> {
            dialog.close();
            getUI().ifPresent(ui -> ui.getPage().setLocation("/logout"));
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        dialog.getFooter().add(cancelButton, confirmButton);
        dialog.open();
    }

    private void toggleTheme() {
        darkMode = !darkMode;

        // Update the UI theme attribute
        applyTheme(darkMode);
        updateToggleIcon();

        // Persist to cookie (works for all users)
        writeDarkModeCookie(darkMode);

        // If authenticated, also persist to the user's profile
        if (oidcUserService.isAuthenticated()) {
            try {
                User user = userService.getCurrentUser();
                if (user != null) {
                    user.setDarkMode(darkMode);
                    userService.saveUser(user);
                }
            } catch (Exception ignored) {
                // cookie is the fallback
            }
        }
    }

    private void updateToggleIcon() {
        if (darkMode) {
            themeToggleButton.setIcon(VaadinIcon.SUN_O.create());
            themeToggleButton.getElement().setAttribute("title", "Switch to light mode");
        } else {
            themeToggleButton.setIcon(VaadinIcon.MOON_O.create());
            themeToggleButton.getElement().setAttribute("title", "Switch to dark mode");
        }
    }

    // ---- Cookie helpers ----

    static boolean readDarkModeCookie() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request instanceof HttpServletRequest httpRequest) {
            Cookie[] cookies = httpRequest.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (DARK_MODE_COOKIE.equals(cookie.getName())) {
                        return "true".equals(cookie.getValue());
                    }
                }
            }
        }
        return false;
    }

    private void writeDarkModeCookie(boolean dark) {
        VaadinResponse response = VaadinService.getCurrentResponse();
        if (response instanceof HttpServletResponse httpResponse) {
            Cookie cookie = new Cookie(DARK_MODE_COOKIE, String.valueOf(dark));
            cookie.setPath("/");
            cookie.setMaxAge(COOKIE_MAX_AGE);
            cookie.setHttpOnly(false); // not sensitive
            httpResponse.addCookie(cookie);
        }
    }

    // ---- Drawer / navigation ----

    private void createDrawer() {
        Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addClassNames(
            LumoUtility.Gap.SMALL,
            LumoUtility.Display.FLEX,
            LumoUtility.FlexDirection.COLUMN,
            LumoUtility.Height.FULL);

        Tab homeTab = createTab("Home", VaadinIcon.HOME, MainView.class);
        tabs.add(homeTab);

        if (oidcUserService.isAuthenticated()) {
            tabs.add(createTab("Stats", VaadinIcon.CHART_LINE, StatsView.class));
            tabs.add(createTab("Posts", VaadinIcon.FILE_TEXT, PostView.class));
            tabs.add(createTab("Go To Guy", VaadinIcon.PHONE, GTGView.class));
            tabs.add(createTab("Profile", VaadinIcon.USER, ProfileView.class));

            if (hasAdminRole()) {
                tabs.add(createTab("Admin", VaadinIcon.COG, AdminView.class));
            }
        }

        addToDrawer(tabs);
    }

    private Tab createTab(String text, VaadinIcon viewIcon, Class<?> navigationTarget) {
        Icon icon = viewIcon.create();
        icon.getStyle().set("box-sizing", "border-box")
            .set("margin-inline-end", "var(--lumo-space-m)")
            .set("margin-inline-start", "var(--lumo-space-xs)")
            .set("padding", "var(--lumo-space-xs)");

        RouterLink link = new RouterLink();
        link.add(icon, new Span(text));
        link.setRoute((Class) navigationTarget);
        link.setTabIndex(-1);

        return new Tab(link);
    }

    private boolean hasAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()) ||
                                 "ADMIN".equals(authority.getAuthority()));
    }
}
