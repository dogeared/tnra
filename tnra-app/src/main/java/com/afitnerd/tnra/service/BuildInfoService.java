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
        if (gitProperties != null) {
            String tag = gitProperties.get("tags");
            if (tag == null || tag.isBlank()) {
                tag = gitProperties.get("tag");
            }
            if (tag == null || tag.isBlank()) {
                tag = gitProperties.get("closest.tag.name");
            }
            if (tag != null && !tag.isBlank()) {
                return tag;
            }
        }
        // A CI-only commit on top of a release has no tag at HEAD, and these
        // release tags are lightweight, so git.closest.tag.name stays empty.
        // VERSION and the Maven build version are kept in sync with the release
        // tag, so use that rather than showing N/A.
        if (buildProperties != null && buildProperties.getVersion() != null) {
            return buildProperties.getVersion();
        }
        return "N/A";
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
