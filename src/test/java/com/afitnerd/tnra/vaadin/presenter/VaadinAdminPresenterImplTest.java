package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.BuildInfoService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VaadinAdminPresenterImplTest {

    @Test
    void delegatesBuildInfoAndAuthCalls() {
        OidcUserService oidc = mock(OidcUserService.class);
        UserService userService = mock(UserService.class);
        BuildInfoService buildInfo = mock(BuildInfoService.class);
        User user = new User();

        when(buildInfo.getGitTag()).thenReturn("v1");
        when(buildInfo.getGitCommitId()).thenReturn("abc");
        when(buildInfo.getGitBranch()).thenReturn("main");
        when(buildInfo.getSpringBootVersion()).thenReturn("3.5");
        when(buildInfo.getVaadinVersion()).thenReturn("24.9");
        when(buildInfo.getJavaVersion()).thenReturn("21");
        when(buildInfo.getBuildTime()).thenReturn("now");
        when(oidc.isAuthenticated()).thenReturn(true);
        when(userService.getCurrentUser()).thenReturn(user);

        VaadinAdminPresenterImpl presenter = new VaadinAdminPresenterImpl(oidc, userService, buildInfo);

        assertEquals("v1", presenter.getGitTag());
        assertEquals("abc", presenter.getGitCommitId());
        assertEquals("main", presenter.getGitBranch());
        assertEquals("3.5", presenter.getSpringBootVersion());
        assertEquals("24.9", presenter.getVaadinVersion());
        assertEquals("21", presenter.getJavaVersion());
        assertEquals("now", presenter.getBuildTime());
        assertTrue(presenter.isAuthenticated());
        assertSame(user, presenter.getCurrentUser());
    }
}
