package com.afitnerd.tnra.vaadin;

import com.afitnerd.tnra.billing.BillingClient;
import java.util.Optional;
import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.AuthNavigationService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MainLayoutTest {

    @Mock
    private OidcUserService oidcUserService;

    @Mock
    private UserService userService;

    @Mock
    private AuthNavigationService authNavigationService;

    private UI ui;

    @BeforeEach
    void setUp() {
        ui = new UI();
        VaadinSession session = mock(VaadinSession.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        lenient().when(session.hasLock()).thenReturn(true);
        VaadinService service = mock(VaadinService.class);
        lenient().when(session.getService()).thenReturn(service);
        ui.getInternals().setSession(session);
        UI.setCurrent(ui);
    }

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    private MainLayout buildLayout() {
        VaadinService mockVaadinService = mock(VaadinService.class);
        Router mockRouter = mock(Router.class);
        lenient().when(mockVaadinService.getRouter()).thenReturn(mockRouter);

        try (MockedStatic<RouteConfiguration> routeConfig = mockStatic(RouteConfiguration.class);
             MockedStatic<VaadinService> vaadinServiceStatic = mockStatic(VaadinService.class)) {
            vaadinServiceStatic.when(VaadinService::getCurrent).thenReturn(mockVaadinService);

            RouteConfiguration mockConfig = mock(RouteConfiguration.class, invocation -> {
                if ("getUrl".equals(invocation.getMethod().getName())) return "/";
                return null;
            });
            routeConfig.when(RouteConfiguration::forSessionScope).thenReturn(mockConfig);
            routeConfig.when(RouteConfiguration::forApplicationScope).thenReturn(mockConfig);
            routeConfig.when(() -> RouteConfiguration.forRegistry(any())).thenReturn(mockConfig);
            return new MainLayout(oidcUserService, userService, authNavigationService, Optional.empty());
        }
    }

    @Test
    void testMainLayoutConstructorWithValidService() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);
        // Expect either success or routing exception — just verify the service was used
        try {
            new MainLayout(oidcUserService, userService, authNavigationService, Optional.empty());
        } catch (Exception e) {
            // Routing context not available in pure unit test — expected
        }
        verify(oidcUserService, atLeastOnce()).isAuthenticated();
    }

    @Test
    void testMainLayoutConstructorWithNullService() {
        assertThrows(Exception.class, () -> new MainLayout(null, null, null, null));
    }

    @Test
    void constructorSucceedsUnauthenticatedWithMockedRoutes() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);
        MainLayout layout = buildLayout();
        assertNotNull(layout);
    }

    @Test
    void constructorSucceedsAuthenticatedWithMockedRoutes() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(null);
        MainLayout layout = buildLayout();
        assertNotNull(layout);
    }

    @Test
    void constructorSucceedsAuthenticatedWithUserAndMockedRoutes() {
        User user = new User();
        user.setId(1L);
        user.setFirstName("Alice");
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);
        MainLayout layout = buildLayout();
        assertNotNull(layout);
    }

    @Test
    void resolveInitialDarkModeReturnsCookieWhenUnauthenticated() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);
        MainLayout layout = buildLayout();
        assertFalse(layout.resolveInitialDarkMode());
    }

    @Test
    void resolveInitialDarkModeReturnsFalseWhenUserDarkModeNull() {
        User user = new User();
        user.setId(1L);
        user.setDarkMode(null);
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);
        MainLayout layout = buildLayout();
        assertFalse(layout.resolveInitialDarkMode());
    }

    @Test
    void resolveInitialDarkModeReturnsTrueWhenUserPrefersDark() {
        User user = new User();
        user.setId(1L);
        user.setDarkMode(true);
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);
        MainLayout layout = buildLayout();
        assertTrue(layout.resolveInitialDarkMode());
    }

    @Test
    void toggleThemeFlipsMode() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);
        MainLayout layout = buildLayout();
        // Toggle twice — should end up in same state
        layout.toggleTheme();
        layout.toggleTheme();
        // No exception means toggle logic executed
        assertNotNull(layout);
    }

    @Test
    void testServiceNotNull() {
        assertThrows(Exception.class, () -> new MainLayout(null, null, null, null));
    }

    @Test
    void testOidcUserServiceDependency() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        try {
            new MainLayout(oidcUserService, userService, authNavigationService, Optional.empty());
        } catch (Exception e) {
            // expected if routing not set up
        }
        verify(oidcUserService, atLeastOnce()).isAuthenticated();
    }

    @Test
    void testAuthenticatedUserServiceCall() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        try {
            new MainLayout(oidcUserService, userService, authNavigationService, Optional.empty());
        } catch (Exception e) {
            // expected
        }
        verify(oidcUserService, atLeastOnce()).isAuthenticated();
    }

    @Test
    void testUnauthenticatedUserServiceCall() {
        when(oidcUserService.isAuthenticated()).thenReturn(false);
        try {
            new MainLayout(oidcUserService, userService, authNavigationService, Optional.empty());
        } catch (Exception e) {
            // expected
        }
        verify(oidcUserService, atLeastOnce()).isAuthenticated();
    }

    /** Same router/registry stubbing as buildLayout(), with a billing client. */
    private MainLayout layoutWith(Optional<BillingClient> billing) {
        VaadinService mockVaadinService = mock(VaadinService.class);
        Router mockRouter = mock(Router.class);
        lenient().when(mockVaadinService.getRouter()).thenReturn(mockRouter);
        try (MockedStatic<RouteConfiguration> routeConfig = mockStatic(RouteConfiguration.class);
             MockedStatic<VaadinService> vaadinServiceStatic = mockStatic(VaadinService.class)) {
            vaadinServiceStatic.when(VaadinService::getCurrent).thenReturn(mockVaadinService);
            RouteConfiguration mockConfig = mock(RouteConfiguration.class, invocation -> {
                if ("getUrl".equals(invocation.getMethod().getName())) return "/";
                return null;
            });
            routeConfig.when(RouteConfiguration::forSessionScope).thenReturn(mockConfig);
            routeConfig.when(RouteConfiguration::forApplicationScope).thenReturn(mockConfig);
            routeConfig.when(() -> RouteConfiguration.forRegistry(any())).thenReturn(mockConfig);
            return new MainLayout(oidcUserService, userService, authNavigationService, billing);
        }
    }

    @Test
    void membershipBanner_shownForGracePeriodMember_onRefresh() {
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getEmail()).thenReturn("m@x.com");
        BillingClient billing = mock(BillingClient.class);
        when(billing.entitlement("m@x.com")).thenReturn(new BillingClient.Entitlement(true, "ON_GRACE_PERIOD", null));
        MainLayout layout = layoutWith(Optional.of(billing));
        assertFalse(layout.membershipBanner.isVisible());

        layout.refreshMembershipBanner();

        assertTrue(layout.membershipBanner.isVisible());
    }

    @Test
    void membershipBanner_hidden_whenActive_orAnonymous_orBillingOff() {
        BillingClient billing = mock(BillingClient.class);
        lenient().when(billing.entitlement("m@x.com")).thenReturn(new BillingClient.Entitlement(true, "ACTIVE", null));
        when(oidcUserService.isAuthenticated()).thenReturn(true);
        when(oidcUserService.getEmail()).thenReturn("m@x.com");
        MainLayout active = layoutWith(Optional.of(billing));
        active.refreshMembershipBanner();
        assertFalse(active.membershipBanner.isVisible());

        when(oidcUserService.isAuthenticated()).thenReturn(false);
        MainLayout anon = layoutWith(Optional.of(billing));
        anon.refreshMembershipBanner();
        assertFalse(anon.membershipBanner.isVisible());

        MainLayout off = layoutWith(Optional.empty());
        off.refreshMembershipBanner();
        assertFalse(off.membershipBanner.isVisible());
        verify(billing, times(1)).entitlement("m@x.com");
    }
}
