package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.config.PaddleSettings;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.router.Router;
import com.vaadin.flow.server.VaadinService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class PayViewTest {

    private static AttachEvent attach(PayView view) {
        return new AttachEvent(view, true);
    }

    /** The nav/footer RouterLinks need a router + route registry; stub them like the main app's tests do. */
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

    @Test
    void rendersChromeStatusAndHiddenReturnLink() {
        PayView view = view(new PaddleSettings("test_abc", "sandbox"));

        assertEquals(PayView.OPENING, view.status.getText());
        assertEquals(PayView.RETURN_LINK_TEXT, view.returnLink.getText());
        assertTrue(view.returnLink.getElement().hasAttribute("hidden"),
            "return link stays hidden until Paddle tells the page where the member came from");
        assertTrue(view.getClassNames().contains("landing-view"));
        assertEquals(3, view.getComponentCount(), "nav + content section + footer");
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

        view.onAttach(attach(view)); // queues the Paddle.js bootstrap; the browser takes it from here

        assertEquals(PayView.OPENING, view.status.getText());
    }

    @Test
    void script_coversTheWholeCheckoutFlow() {
        String js = PayView.SCRIPT;

        // Reads the transaction Paddle appended to the default payment link.
        assertTrue(js.contains("get('_ptxn')"));
        // Loads Paddle.js from the URL passed in (pinned to cdn.paddle.com in the view).
        assertTrue(js.contains("s.src = src"));
        assertTrue(PayView.PADDLE_JS_URL.startsWith("https://cdn.paddle.com/"));
        // Sandbox must be selected before Initialize, and only for sandbox.
        assertTrue(js.contains("if (env === 'sandbox') { Paddle.Environment.set('sandbox'); }"));
        assertTrue(js.contains("Paddle.Initialize({ token: token, eventCallback: onEvent })"));
        // Return trip comes from the transaction's custom_data and is https-only (no open redirect).
        assertTrue(js.contains("data.custom_data.redirect_url"));
        assertTrue(js.contains("url.startsWith('https://')"));
        assertTrue(js.contains("checkout.completed"));
        assertTrue(js.contains("window.location.replace(redirect)"));
        // Closing the overlay after paying must not report "nothing charged".
        assertTrue(js.contains("ev.name === 'checkout.closed' && !completed"));
        assertFalse(js.contains("http://"), "never downgrade to http");
    }
}
