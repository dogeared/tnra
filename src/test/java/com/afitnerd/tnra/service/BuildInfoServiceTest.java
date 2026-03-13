package com.afitnerd.tnra.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuildInfoServiceTest {

    @Test
    void returnsExpectedValuesFromGitAndBuildProperties() {
        Properties git = new Properties();
        git.setProperty("tags", "v1.2.3");
        git.setProperty("branch", "main");
        git.setProperty("commit.id.abbrev", "abc1234");

        Properties build = new Properties();
        build.setProperty("time", "2026-03-13T17:00:00Z");

        BuildInfoService service = new BuildInfoService(new GitProperties(git), new BuildProperties(build));

        assertEquals("v1.2.3", service.getGitTag());
        assertEquals("abc1234", service.getGitCommitId());
        assertEquals("main", service.getGitBranch());
        assertEquals("2026-03-13T17:00:00Z", service.getBuildTime());
        assertNotNull(service.getSpringBootVersion());
        assertNotNull(service.getVaadinVersion());
        assertNotNull(service.getJavaVersion());
    }

    @Test
    void fallsBackToClosestTagAndNaValuesWhenMissing() {
        Properties git = new Properties();
        git.setProperty("tags", " ");
        git.setProperty("closest.tag.name", "release-42");

        BuildInfoService serviceWithFallbackTag = new BuildInfoService(new GitProperties(git), null);
        assertEquals("release-42", serviceWithFallbackTag.getGitTag());
        assertEquals("N/A", serviceWithFallbackTag.getBuildTime());

        BuildInfoService serviceNoProps = new BuildInfoService(null, null);
        assertEquals("N/A", serviceNoProps.getGitTag());
        assertEquals("N/A", serviceNoProps.getGitCommitId());
        assertEquals("N/A", serviceNoProps.getGitBranch());
        assertEquals("N/A", serviceNoProps.getBuildTime());
    }
}
