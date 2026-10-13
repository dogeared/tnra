package com.afitnerd.tnra.landing.vaadin;

import com.afitnerd.tnra.landing.config.PaddleSettings;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Paddle checkout page — the site's <b>default payment link</b>.
 *
 * <p>Paddle's "hosted" checkout is not a Paddle-hosted page: the {@code checkout.url} the billing
 * service gets back from {@code POST /transactions} is simply this page's URL with
 * {@code ?_ptxn=txn_…} appended. Paddle.js on this page reads that parameter and opens the checkout
 * overlay for the transaction. So this URL ({@code https://tnra.app/pay}) must be configured in the
 * Paddle dashboard under <i>Checkout → Checkout settings → Default payment link</i>, and the page must
 * live on the domain Paddle approved for the seller (the landing site's). The group apps never embed
 * Paddle.js themselves — they redirect the member here and Paddle sends them back.
 *
 * <p>Return trip: the billing service stores the group app's return URL in the transaction's
 * {@code custom_data.redirect_url}. Paddle.js echoes {@code custom_data} on every checkout event, so on
 * {@code checkout.completed} the page forwards the member there (normally the group's
 * {@code /billing/activating} page, which waits for the webhook). Only {@code https://} targets are
 * followed — this is a public page, so the redirect must not become an open redirect.
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
public class PayView extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(PayView.class);

    static final String PADDLE_JS_URL = "https://cdn.paddle.com/paddle/v2/paddle.js";

    static final String OPENING = "Opening Paddle's secure checkout…";
    static final String NOT_CONFIGURED =
        "Checkout isn't set up on this site yet. Please contact support@dogeared.dev.";
    static final String RETURN_LINK_TEXT = "Return to your group";

    /**
     * Runs in the browser with {@code $0}=client token, {@code $1}=environment, {@code $2}=Paddle.js URL,
     * {@code $3}=status paragraph element, {@code $4}=return link element. Kept as one script so the whole
     * flow (load Paddle.js → initialize → react to checkout events) is reviewable in one place.
     */
    static final String SCRIPT = String.join("\n",
        "const token = $0, env = $1, src = $2, status = $3, link = $4;",
        "const txn = new URLSearchParams(window.location.search).get('_ptxn');",
        "if (!txn) {",
        "  status.textContent = 'There is nothing to pay for here. Start checkout from the membership page in your group app.';",
        "  return;",
        "}",
        "let redirect = null, completed = false;",
        "function remember(data) {",
        "  const url = data && data.custom_data && data.custom_data.redirect_url;",
        "  if (typeof url === 'string' && url.startsWith('https://')) {",
        "    redirect = url; link.href = url; link.removeAttribute('hidden');",
        "  }",
        "}",
        "function onEvent(ev) {",
        "  if (!ev || !ev.name) { return; }",
        "  if (ev.name === 'checkout.loaded') {",
        "    remember(ev.data);",
        "    status.textContent = 'Complete your payment in the secure checkout window.';",
        "  } else if (ev.name === 'checkout.completed') {",
        "    completed = true; remember(ev.data);",
        "    if (redirect) {",
        "      status.textContent = 'Payment received — taking you back to your group…';",
        "      setTimeout(function () { window.location.replace(redirect); }, 1500);",
        "    } else {",
        "      status.textContent = 'Payment received. You can close this window and return to your group app.';",
        "    }",
        "  } else if (ev.name === 'checkout.closed' && !completed) {",
        "    status.textContent = 'Checkout closed — nothing has been charged. Use the link below to return to your group, or reload this page to try again.';",
        "  } else if (ev.name === 'checkout.error') {",
        "    status.textContent = 'Something went wrong starting the checkout. Please return to your group app and try again.';",
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

    public PayView(PaddleSettings paddle) {
        this.paddle = paddle;
        addClassName("landing-view");
        setPadding(false);
        setSpacing(false);

        add(LandingChrome.nav());

        Div section = new Div();
        section.addClassName("content-section");
        Div prose = new Div();
        prose.addClassName("content-prose");
        // The link only becomes useful once Paddle tells us where the member came from; the script
        // un-hides it then. Attribute (not setVisible) so the server never fights the browser over it.
        returnLink.getElement().setAttribute("hidden", true);
        prose.add(new H1("Secure checkout"), status, returnLink);
        section.add(prose);
        add(section);

        add(LandingChrome.footer());
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
            status.getElement(), returnLink.getElement());
    }
}
