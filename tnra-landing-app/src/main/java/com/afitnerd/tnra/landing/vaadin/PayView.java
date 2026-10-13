package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.config.PaddleSettings;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;

/**
 * Paddle checkout page — the site's <b>default payment link</b>.
 *
 * <p>Paddle's "hosted" checkout is not a Paddle-hosted page: the {@code checkout.url} the billing
 * service gets back from {@code POST /transactions} is simply this page's URL with
 * {@code ?_ptxn=txn_…} appended. Paddle.js on this page reads that parameter and opens the checkout
 * overlay for the transaction. So this URL ({@code https://tnra.app/pay}) must be configured in the
 * Paddle dashboard under <i>Checkout → Checkout settings → Default payment link</i>, and the page must
 * live on the domain Paddle approved for the seller (the landing site's). The group apps never embed
 * Paddle.js themselves — they redirect the member here and this page sends them back.
 *
 * <p>Way back: the group app appends two URLs to the checkout link before sending the browser here —
 * {@code return} (where to go if the member closes the overlay without paying: their membership page)
 * and {@code done} (where to go after a completed checkout: their {@code /billing/activating} page,
 * which waits for the webhook). Each group has its own subdomain, so both are dynamic; the page's
 * "TNRA" header link points at the origin of {@code return}, never at this landing site. Paddle also
 * echoes {@code custom_data.redirect_url} on checkout events as a fallback for {@code done}. Only
 * {@code https://} targets are honoured — this is a public page, so it must not become an open redirect.
 *
 * <p>Vaadin only renders the chrome; the checkout itself is a Paddle iframe. The matching CSP that lets
 * Paddle.js and the overlay load is scoped to this path in
 * {@link com.afitnerd.tnra.landing.config.SecurityConfig}.
 */
@Route("pay")
@PageTitle("Secure checkout — TNRA")
@AnonymousAllowed
@CssImport("./styles/theme.css")
@CssImport("./styles/landing-view.css")
public class PayView extends VerticalLayout implements BeforeEnterObserver {

    private static final Logger log = LoggerFactory.getLogger(PayView.class);

    static final String PADDLE_JS_URL = "https://cdn.paddle.com/paddle/v2/paddle.js";
    static final String RETURN_PARAM = "return";
    static final String DONE_PARAM = "done";

    static final String OPENING = "Opening Paddle's secure checkout…";
    static final String NOT_CONFIGURED =
        "Checkout isn't set up on this site yet. Please contact support@dogeared.dev.";
    static final String RETURN_LINK_TEXT = "Back to your group";

    /**
     * Runs in the browser with {@code $0}=client token, {@code $1}=environment, {@code $2}=Paddle.js URL,
     * {@code $3}=status paragraph element, {@code $4}=return link element, {@code $5}=done URL (or ''),
     * {@code $6}=return URL (or ''). One script so the whole flow (load Paddle.js → initialize → react to
     * checkout events) is reviewable in one place.
     */
    static final String SCRIPT = String.join("\n",
        "const token = $0, env = $1, src = $2, status = $3, link = $4;",
        "let done = $5 || null, back = $6 || null, completed = false;",
        "function https(url) { return typeof url === 'string' && url.startsWith('https://') ? url : null; }",
        "const txn = new URLSearchParams(window.location.search).get('_ptxn');",
        "if (!txn) {",
        "  status.textContent = 'There is nothing to pay for here. Start checkout from the membership page in your group app.';",
        "  return;",
        "}",
        "function onEvent(ev) {",
        "  if (!ev || !ev.name) { return; }",
        "  const echoed = https(ev.data && ev.data.custom_data && ev.data.custom_data.redirect_url);",
        "  if (ev.name === 'checkout.loaded') {",
        "    status.textContent = 'Complete your payment in the secure checkout window.';",
        "  } else if (ev.name === 'checkout.completed') {",
        "    completed = true;",
        "    const target = done || echoed || back;",
        "    if (target) {",
        "      status.textContent = 'Payment received — taking you back to your group…';",
        "      link.href = target; link.textContent = 'Continue to your group';",
        "      setTimeout(function () { window.location.replace(target); }, 1500);",
        "    } else {",
        "      status.textContent = 'Payment received. You can close this window and return to your group app.';",
        "    }",
        "  } else if (ev.name === 'checkout.closed' && !completed) {",
        "    status.textContent = 'Checkout closed — nothing has been charged. Head back to your group to try again whenever you like.';",
        "  } else if (ev.name === 'checkout.error') {",
        "    status.textContent = 'Something went wrong starting the checkout. Please go back to your group and try again.';",
        "  }",
        "}",
        "const s = document.createElement('script');",
        "s.src = src; s.async = true;",
        "s.onload = function () {",
        "  if (env === 'sandbox') { Paddle.Environment.set('sandbox'); }",
        "  Paddle.Initialize({ token: token, eventCallback: onEvent });",
        "};",
        "s.onerror = function () {",
        "  status.textContent = 'We couldn\\'t load the secure checkout. Check your connection and reload this page.';",
        "};",
        "document.head.appendChild(s);");

    private final transient PaddleSettings paddle;
    final Paragraph status = new Paragraph(OPENING);
    final Anchor returnLink = new Anchor("", RETURN_LINK_TEXT);
    /** Header "TNRA" link: the member's own group app (origin of {@code return}), not this landing site. */
    final Anchor logoLink = new Anchor("/", "TNRA");

    String returnUrl; // package-private for testing
    String doneUrl;

    public PayView(PaddleSettings paddle) {
        this.paddle = paddle;
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        // A minimal header: no About / Pricing / Request Access — nothing that pulls a member mid-checkout
        // away from their group. Same classes as the landing nav so it looks like the rest of the site.
        HorizontalLayout header = new HorizontalLayout();
        header.addClassName("landing-nav");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        logoLink.addClassName("nav-logo");
        header.add(logoLink);
        add(header);

        Div section = new Div();
        section.addClassName("content-section");
        Div prose = new Div();
        prose.addClassName("content-prose");
        // Hidden until we know where "back" is (beforeEnter); attribute, not setVisible, so the server
        // never fights the browser over it once the script takes over.
        returnLink.getElement().setAttribute("hidden", true);
        prose.add(new H1("Secure checkout"), status, returnLink);
        section.add(prose);
        add(section);

        add(LandingChrome.footer());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        var params = event.getLocation().getQueryParameters().getParameters();
        returnUrl = httpsOrNull(first(params.get(RETURN_PARAM)));
        doneUrl = httpsOrNull(first(params.get(DONE_PARAM)));
        if (returnUrl != null) {
            returnLink.setHref(returnUrl);
            returnLink.getElement().removeAttribute("hidden");
            String origin = originOf(returnUrl);
            logoLink.setHref(origin != null ? origin : returnUrl);
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!paddle.isConfigured()) {
            log.warn("/pay opened but tnra.paddle.client-token is blank — checkout cannot start");
            status.setText(NOT_CONFIGURED);
            return;
        }
        getElement().executeJs(SCRIPT, paddle.getClientToken(), paddle.getEnvironment(), PADDLE_JS_URL,
            status.getElement(), returnLink.getElement(), doneUrl == null ? "" : doneUrl,
            returnUrl == null ? "" : returnUrl);
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    /** Only https targets are ever linked or redirected to — this is a public, unauthenticated page. */
    static String httpsOrNull(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(url.trim());
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null ? uri.toString() : null;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** {@code https://group.tnra.app/billing} → {@code https://group.tnra.app}. */
    static String originOf(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
