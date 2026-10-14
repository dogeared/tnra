package com.afitnerd.tnra.billing.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The billing service is publicly reachable, so every response — a permitted endpoint and a denied
 * one alike — must carry the full security-header set (securityheaders.com A).
 */
@SpringBootTest
class SecurityHeadersTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static void assertSecurityHeaders(MockHttpServletResponse res) {
        assertEquals("max-age=63072000 ; includeSubDomains", res.getHeader("Strict-Transport-Security"));
        assertEquals(SecurityConfig.CSP, res.getHeader("Content-Security-Policy"));
        assertEquals("no-referrer", res.getHeader("Referrer-Policy"));
        assertEquals(SecurityConfig.PERMISSIONS_POLICY, res.getHeader("Permissions-Policy"));
        assertEquals("nosniff", res.getHeader("X-Content-Type-Options"));
        assertEquals("DENY", res.getHeader("X-Frame-Options"));
    }

    @Test
    void permittedEndpoint_carriesAllSecurityHeaders() throws Exception {
        MockHttpServletResponse res = mvc.perform(get("/actuator/health")).andReturn().getResponse();

        assertEquals(200, res.getStatus());
        assertSecurityHeaders(res);
    }

    @Test
    void deniedRequest_carriesAllSecurityHeaders_too() throws Exception {
        MockHttpServletResponse res = mvc.perform(get("/")).andReturn().getResponse();

        assertNotEquals(200, res.getStatus());
        assertSecurityHeaders(res);
    }

    @Test
    void policyAllowsNothing_andHstsIsNotRequestScoped() {
        assertTrue(SecurityConfig.CSP.startsWith("default-src 'none'"));
        assertTrue(SecurityConfig.CSP.contains("frame-ancestors 'none'"));
        assertTrue(SecurityConfig.PERMISSIONS_POLICY.contains("payment=()"));
    }
}
