package com.afitnerd.tnra.landing.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.header.HeaderWriter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The site-wide headers earn an A on securityheaders.com; /pay must loosen exactly what Paddle needs and
 * nothing else. These tests pin both policies and the path switch between them.
 */
class SecurityConfigTest {

    private final HeaderWriter writer = SecurityConfig.pathAwareHeaders();

    private MockHttpServletResponse headersFor(String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", servletPath);
        request.setServletPath(servletPath);
        MockHttpServletResponse response = new MockHttpServletResponse();
        writer.writeHeaders(request, response);
        return response;
    }

    @Test
    void ordinaryPages_getTightSiteWidePolicy() {
        MockHttpServletResponse res = headersFor("/pricing");

        assertEquals(SecurityConfig.CSP, res.getHeader("Content-Security-Policy"));
        assertEquals(SecurityConfig.PERMISSIONS_POLICY, res.getHeader("Permissions-Policy"));
        assertFalse(res.getHeader("Content-Security-Policy").contains("paddle.com"));
        assertTrue(res.getHeader("Permissions-Policy").contains("payment=()"));
    }

    @Test
    void payPage_allowsPaddleScriptFrameAndApi() {
        String csp = headersFor("/pay").getHeader("Content-Security-Policy");

        assertEquals(SecurityConfig.PAY_CSP, csp);
        assertTrue(csp.contains("script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.paddle.com"));
        assertTrue(csp.contains("frame-src https://buy.paddle.com https://sandbox-buy.paddle.com"));
        assertTrue(csp.contains("connect-src 'self' https://*.paddle.com"));
        // Nothing else loosened.
        assertTrue(csp.contains("object-src 'none'"));
        assertTrue(csp.contains("frame-ancestors 'self'"));
        assertTrue(csp.contains("form-action 'self'"));
    }

    @Test
    void payPage_allowsPaymentRequestApiForPaddleFrames() {
        String pp = headersFor("/pay").getHeader("Permissions-Policy");

        assertEquals(SecurityConfig.PAY_PERMISSIONS_POLICY, pp);
        assertTrue(pp.contains("payment=(self \"https://buy.paddle.com\" \"https://sandbox-buy.paddle.com\")"));
        assertTrue(pp.contains("camera=()"));
    }

    @Test
    void payPath_matchesWithAndWithoutTrailingSlash_butNotPrefixes() {
        assertTrue(SecurityConfig.isPayPath(requestFor("/pay", "")));
        assertTrue(SecurityConfig.isPayPath(requestFor("/pay/", "")));
        assertFalse(SecurityConfig.isPayPath(requestFor("/payments", "")));
        assertFalse(SecurityConfig.isPayPath(requestFor("/", "")));
    }

    @Test
    void payPath_stripsContextPath_andFallsBackToRequestUri() {
        MockHttpServletRequest withContext = new MockHttpServletRequest("GET", "/app/pay");
        withContext.setContextPath("/app");
        withContext.setServletPath(""); // some containers report an empty servlet path for the default servlet
        assertTrue(SecurityConfig.isPayPath(withContext));

        MockHttpServletRequest noPath = new MockHttpServletRequest("GET", "/pricing");
        noPath.setServletPath("");
        assertFalse(SecurityConfig.isPayPath(noPath));
    }

    private MockHttpServletRequest requestFor(String servletPath, String contextPath) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", contextPath + servletPath);
        request.setContextPath(contextPath);
        request.setServletPath(servletPath);
        return request;
    }
}
