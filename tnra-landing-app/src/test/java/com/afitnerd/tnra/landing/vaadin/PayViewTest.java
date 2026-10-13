package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.config.PaddleSettings;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

class PayViewTest {

    private static AttachEvent attach(PayView view) {
        return new AttachEvent(view, true);
    }

    /** The footer RouterLinks need a router + route registry; stub them like the main app's tests do. */
    private static PayView view(PaddleSettings settings) {
        VaadinService service = mock(VaadinService.class);
        lenient().when(service.getRouter()).thenReturn(mock(Router.class));
        try (MockedStatic<RouteConfiguration> routeConfig = mockStatic(RouteConfiguration.class);
             MockedStatic<VaadinService> vaadinService = mockStatic(VaadinService.class)) {
            vaadinService.when(VaadinService::getCurrent).thenReturn(service);
            RouteConfiguration config = mock(RouteConfiguration.class, invocation ->
                "getUrl".equals(invocation.getMethod().getName()) ? "/" : null);
            routeConfig.when(RouteConfiguration::forSessionScope).thenReturn(config);
            routeConfig.when(RouteConfiguration::forApplicationScope).thenReturn(config);
            routeConfig.when(() -> RouteConfiguration.forRegistry(any())).thenReturn(config);
            return new PayView(settings);
        }
    }

    private static BeforeEnterEvent enterWith(Map<String, String> params) {
        BeforeEnterEvent event = mock(BeforeEnterEvent.class);
        when(event.getLocation()).thenReturn(new Location("pay", QueryParameters.simple(params)));
        return event;
    }

    @Test
    void rendersOwnHeaderStatusAndHiddenReturnLink() {
        PayView view = view(new PaddleSettings("test_abc", "sandbox"));

        assertEquals(PayView.OPENING, view.status.getText());
        assertEquals(PayView.RETURN_LINK_TEXT, view.returnLink.getText());
        assertTrue(view.returnLink.getElement().hasAttribute("hidden"),
            "return link stays hidden until the group app tells the page where back is");
        assertEquals("TNRA", view.logoLink.getText());
        assertTrue(view.getClassNames().contains("landing-view"));
        assertEquals(3, view.getComponentCount(), "header + content section + footer");
    }

    @Test
    void beforeEnter_withReturnAndDone_linksBackToTheMembersOwnGroup() {
        PayView view = view(new PaddleSettings("test_abc", "sandbox"));

        view.beforeEnter(enterWith(Map.of(
            "return", "https://rome.tnra.app/billing",
            "done", "https://rome.tnra.app/billing/activating",
            "_ptxn", "txn_1")));

        assertEquals("https://rome.tnra.app/billing", view.returnUrl);
        assertEquals("https://rome.tnra.app/billing/activating", view.doneUrl);
        assertEquals("https://rome.tnra.app/billing", view.returnLink.getHref());
        assertFalse(view.returnLink.getElement().hasAttribute("hidden"), "back link shown from first paint");
        assertEquals("https://rome.tnra.app", view.logoLink.getHref(), "header logo goes to the group, not the landing site");
    }

    @Test
    void beforeEnter_withoutParams_keepsLandingDefaults() {
        PayView view = view(new PaddleSettings("test_abc", "sandbox"));

        view.beforeEnter(enterWith(Map.of("_ptxn", "txn_1")));

        assertNull(view.returnUrl);
        assertNull(view.doneUrl);
        assertTrue(view.returnLink.getElement().hasAttribute("hidden"));
        assertEquals("/", view.logoLink.getHref());
    }

    @Test
    void beforeEnter_rejectsNonHttpsTargets_noOpenRedirect() {
        PayView view = view(new PaddleSettings("test_abc", "sandbox"));

        view.beforeEnter(enterWith(Map.of("return", "http://evil.example/x", "done", "javascript:alert(1)")));

        assertNull(view.returnUrl);
        assertNull(view.doneUrl);
        assertTrue(view.returnLink.getElement().hasAttribute("hidden"));
        assertEquals("/", view.logoLink.getHref());
    }

    @Test
    void httpsOrNull_andOriginOf() {
        assertEquals("https://g.tnra.app/billing", PayView.httpsOrNull(" https://g.tnra.app/billing "));
        assertNull(PayView.httpsOrNull("http://g.tnra.app/billing"));
        assertNull(PayView.httpsOrNull("https:///nohost"));
        assertNull(PayView.httpsOrNull("not a url at all"));
        assertNull(PayView.httpsOrNull(""));
        assertNull(PayView.httpsOrNull(null));
        assertEquals("https://g.tnra.app", PayView.originOf("https://g.tnra.app/billing?x=1"));
        assertEquals("https://g.tnra.app:8443", PayView.originOf("https://g.tnra.app:8443/billing"));
        assertNull(PayView.originOf("/relative"));
        assertNull(PayView.originOf("::not-a-uri"));
    }

    @Test
    void onAttach_withoutClientToken_explainsInsteadOfLoadingPaddle() {
        PayView view = view(new PaddleSettings("", "sandbox"));

        view.onAttach(attach(view));

        assertEquals(PayView.NOT_CONFIGURED, view.status.getText());
    }

    @Test
    void onAttach_withClientToken_keepsOpeningStatusForTheScriptToDrive() {
        PayView view = view(new PaddleSettings("test_abc", "sandbox"));
        view.beforeEnter(enterWith(Map.of("return", "https://rome.tnra.app/billing")));

        view.onAttach(attach(view)); // queues the Paddle.js bootstrap; the browser takes it from here

        assertEquals(PayView.OPENING, view.status.getText());
    }

    @Test
    void script_coversTheWholeCheckoutFlow() {
        String js = PayView.SCRIPT;

        assertTrue(js.contains("get('_ptxn')"));
        assertTrue(js.contains("s.src = src"));
        assertTrue(PayView.PADDLE_JS_URL.startsWith("https://cdn.paddle.com/"));
        assertTrue(js.contains("if (env === 'sandbox') { Paddle.Environment.set('sandbox'); }"));
        assertTrue(js.contains("Paddle.Initialize({ token: token, eventCallback: onEvent })"));
        // After payment: explicit done URL first, Paddle's custom_data echo second, back link last.
        assertTrue(js.contains("const target = done || echoed || back;"));
        assertTrue(js.contains("ev.data.custom_data.redirect_url"));
        assertTrue(js.contains("url.startsWith('https://')"));
        assertTrue(js.contains("window.location.replace(target)"));
        // Closing the overlay after paying must not report "nothing charged".
        assertTrue(js.contains("ev.name === 'checkout.closed' && !completed"));
        assertFalse(js.contains("http://"), "never downgrade to http");
    }
}
