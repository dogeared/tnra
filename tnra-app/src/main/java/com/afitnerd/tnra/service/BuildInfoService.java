package com.afitnerd.tnra.service;

import com.vaadin.flow.server.Version;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

@Service
public class BuildInfoService {

    private final GitProperties gitProperties;
    private final BuildProperties buildProperties;

    public BuildInfoService(
        @Nullable GitProperties gitProperties,
        @Nullable BuildProperties buildProperties
    ) {
        this.gitProperties = gitProperties;
        this.buildProperties = buildProperties;
    }

    public String getGitTag() {
        if (gitProperties == null) return "N/A";
        String tag = gitProperties.get("tags");
        if (tag == null || tag.isBlank()) {
            tag = gitProperties.get("closest.tag.name");
        }
        return tag != null && !tag.isBlank() ? tag : "N/A";
    }

    public String getGitCommitId() {
        return gitProperties != null ? gitProperties.getShortCommitId() : "N/A";
    }

    public String getGitBranch() {
        return gitProperties != null ? gitProperties.getBranch() : "N/A";
    }

    public String getSpringBootVersion() {
        return SpringBootVersion.getVersion();
    }

    public String getVaadinVersion() {
        return Version.getFullVersion();
    }

    public String getJavaVersion() {
        return System.getProperty("java.version");
    }

    public String getBuildTime() {
        if (buildProperties != null && buildProperties.getTime() != null) {
            return buildProperties.getTime().toString();
        }
        return "N/A";
    }
}
