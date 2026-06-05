package com.afitnerd.tnra.vaadin.presenter;

import com.afitnerd.tnra.model.User;

public interface VaadinAdminPresenter {
    String getGitTag();
    String getGitCommitId();
    String getGitBranch();
    String getSpringBootVersion();
    String getVaadinVersion();
    String getJavaVersion();
    String getBuildTime();
    boolean isAuthenticated();
    User getCurrentUser();
}
