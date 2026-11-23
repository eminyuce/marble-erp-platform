package com.ozerler.marble.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseInfoContributorTest {

    @Mock
    private Environment environment;

    @Test
    @DisplayName("actuator info exposes the pom version and omits secrets")
    void contribute_PublishesReleaseFactsFromBuildInfo() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
        ObjectStorageProperties storage = new ObjectStorageProperties();
        storage.setType("rustfs");
        storage.getRustfs().setBucket("erp-files");
        DeploymentProperties deployment = new DeploymentProperties();
        deployment.setGitBranch("main");
        deployment.setImageRepository("guvenulu/marble-erp-platform");
        deployment.setImageTag("latest");

        ReleaseInfoContributor contributor = new ReleaseInfoContributor(
                Optional.of(buildProperties()),
                environment,
                storage,
                deployment);
        Info.Builder builder = new Info.Builder();
        contributor.contribute(builder);

        @SuppressWarnings("unchecked")
        Map<String, Object> release = (Map<String, Object>) builder.build().getDetails().get("release");
        assertThat(release.get("appVersion")).isEqualTo("1.0.1");
        assertThat(release.get("appName")).isEqualTo("Özerler Mermer ERP");
        assertThat(release.get("gitBranch")).isEqualTo("main");
        assertThat(release.get("sessionStore")).isEqualTo("JDBC");
        assertThat(release.get("storageType")).isEqualTo("rustfs");
        assertThat(release.get("bucket")).isEqualTo("erp-files");
        assertThat(release.get("activeProfiles")).isEqualTo("prod");
        assertThat(release.get("springBootVersion").toString()).isNotBlank();
        assertThat(release.get("springFrameworkVersion").toString()).isNotBlank();
        assertThat(release.get("image")).isEqualTo("guvenulu/marble-erp-platform:latest");
        assertThat(release.toString()).doesNotContain("SECRET");
        assertThat(release.toString()).doesNotContain("password");
    }

    @Test
    @DisplayName("git commit is shortened and a missing sha stays unknown")
    void shortSha_TrimsAndHidesUnknown() {
        assertThat(ReleaseInfoContributor.shortSha("abcdef1234567890ffff")).isEqualTo("abcdef123456");
        assertThat(ReleaseInfoContributor.shortSha("unknown")).isEqualTo("unknown");
        assertThat(ReleaseInfoContributor.shortSha(null)).isEqualTo("unknown");
    }

    private static BuildProperties buildProperties() {
        Properties entries = new Properties();
        entries.setProperty("name", "Özerler Mermer ERP");
        entries.setProperty("version", "1.0.1");
        entries.setProperty("time", "2026-11-23T10:07:04.460Z");
        entries.setProperty("group", "com.ozerler");
        entries.setProperty("artifact", "marble-erp-platform");
        return new BuildProperties(entries);
    }
}
