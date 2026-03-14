package com.afitnerd.tnra.vaadin;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class MainLayoutUtilityTest {

    @Test
    void readDarkModeCookieReflectsCookieValue() {
        VaadinRequest request = mock(VaadinRequest.class, withSettings().extraInterfaces(HttpServletRequest.class));
        HttpServletRequest servletRequest = (HttpServletRequest) request;

        try (var mocked = mockStatic(VaadinService.class)) {
            mocked.when(VaadinService::getCurrentRequest).thenReturn(request);
            when(servletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(MainLayout.DARK_MODE_COOKIE, "true")});
            assertTrue(MainLayout.readDarkModeCookie());

            when(servletRequest.getCookies()).thenReturn(new Cookie[]{new Cookie(MainLayout.DARK_MODE_COOKIE, "false")});
            assertFalse(MainLayout.readDarkModeCookie());
        }
    }

    @Test
    void updateToggleIconAndRoleCheckPathsWork() throws Exception {
        MainLayout layout = allocateWithoutConstructor();
        setField(layout, "themeToggleButton", new Button());

        setField(layout, "darkMode", true);
        invoke(layout, "updateToggleIcon");
        assertEquals("Switch to light mode", ((Button) getField(layout, "themeToggleButton")).getElement().getAttribute("title"));

        setField(layout, "darkMode", false);
        invoke(layout, "updateToggleIcon");
        assertEquals("Switch to dark mode", ((Button) getField(layout, "themeToggleButton")).getElement().getAttribute("title"));

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("u", "p", List.of(() -> "ROLE_ADMIN"))
        );
        assertTrue((Boolean) invoke(layout, "hasAdminRole"));

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("u", "p", List.of(() -> "ROLE_USER"))
        );
        assertFalse((Boolean) invoke(layout, "hasAdminRole"));
    }

    @Test
    void writeDarkModeCookieAddsCookieToResponse() throws Exception {
        MainLayout layout = allocateWithoutConstructor();
        VaadinResponse response = mock(VaadinResponse.class, withSettings().extraInterfaces(HttpServletResponse.class));
        HttpServletResponse servletResponse = (HttpServletResponse) response;

        try (var mocked = mockStatic(VaadinService.class)) {
            mocked.when(VaadinService::getCurrentResponse).thenReturn(response);
            invoke(layout, "writeDarkModeCookie", true);
        }

        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(servletResponse).addCookie(cookieCaptor.capture());
        assertEquals(MainLayout.DARK_MODE_COOKIE, cookieCaptor.getValue().getName());
        assertEquals("true", cookieCaptor.getValue().getValue());
    }

    private static MainLayout allocateWithoutConstructor() throws Exception {
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
        theUnsafe.setAccessible(true);
        Object unsafe = theUnsafe.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
        return (MainLayout) allocateInstance.invoke(unsafe, MainLayout.class);
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass() == Boolean.class ? boolean.class : args[i].getClass();
        }
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
