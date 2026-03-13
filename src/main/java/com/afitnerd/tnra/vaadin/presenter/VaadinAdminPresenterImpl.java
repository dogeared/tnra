package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.User;
import com.afitnerd.tnra.service.BuildInfoService;
import com.afitnerd.tnra.service.OidcUserService;
import com.afitnerd.tnra.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class VaadinAdminPresenterImpl implements VaadinAdminPresenter {

    private final OidcUserService oidcUserService;
    private final UserService userService;
    private final BuildInfoService buildInfoService;

    public VaadinAdminPresenterImpl(
        OidcUserService oidcUserService,
        UserService userService,
        BuildInfoService buildInfoService
    ) {
        this.oidcUserService = oidcUserService;
        this.userService = userService;
        this.buildInfoService = buildInfoService;
    }

    @Override
    public String getGitTag() {
        return buildInfoService.getGitTag();
    }

    @Override
    public String getGitCommitId() {
        return buildInfoService.getGitCommitId();
    }

    @Override
    public String getGitBranch() {
        return buildInfoService.getGitBranch();
    }

    @Override
    public String getSpringBootVersion() {
        return buildInfoService.getSpringBootVersion();
    }

    @Override
    public String getVaadinVersion() {
        return buildInfoService.getVaadinVersion();
    }

    @Override
    public String getJavaVersion() {
        return buildInfoService.getJavaVersion();
    }

    @Override
    public String getBuildTime() {
        return buildInfoService.getBuildTime();
    }

    @Override
    public boolean isAuthenticated() {
        return oidcUserService.isAuthenticated();
    }

    @Override
    public User getCurrentUser() {
        return userService.getCurrentUser();
    }
}
