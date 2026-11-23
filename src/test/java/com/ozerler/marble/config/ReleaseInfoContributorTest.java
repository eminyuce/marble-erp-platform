package com.ozerler.marble.config;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;

@ExtendWith(MockitoExtension.class)
class ReleaseInfoContributorTest {

    @Mock
    private Environment environment;

    @Test
    @DisplayName("actuator info exposes the pom version and omits secrets")
    void contributePublishesReleaseFactsFromBuildInfo() {
        Mockito.when(environment.getActiveProfiles()).thenReturn(new String[]{"prod"});
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
        Assertions.assertThat(release.get("appVersion")).isEqualTo("1.0.1");
        Assertions.assertThat(release.get("appName")).isEqualTo("Özerler Mermer ERP");
        Assertions.assertThat(release.get("gitBranch")).isEqualTo("main");
        Assertions.assertThat(release.get("sessionStore")).isEqualTo("JDBC");
        Assertions.assertThat(release.get("storageType")).isEqualTo("rustfs");
        Assertions.assertThat(release.get("bucket")).isEqualTo("erp-files");
        Assertions.assertThat(release.get("activeProfiles")).isEqualTo("prod");
        Assertions.assertThat(release.get("springBootVersion").toString()).isNotBlank();
        Assertions.assertThat(release.get("springFrameworkVersion").toString()).isNotBlank();
        Assertions.assertThat(release.get("image")).isEqualTo("guvenulu/marble-erp-platform:latest");
        Assertions.assertThat(release.toString()).doesNotContain("SECRET");
        Assertions.assertThat(release.toString()).doesNotContain("password");
    }

    @Test
    @DisplayName("git commit is shortened and a missing sha stays unknown")
    void shortShaTrimsAndHidesUnknown() {
        Assertions.assertThat(ReleaseInfoContributor.shortSha("abcdef1234567890ffff")).isEqualTo("abcdef123456");
        Assertions.assertThat(ReleaseInfoContributor.shortSha("unknown")).isEqualTo("unknown");
        Assertions.assertThat(ReleaseInfoContributor.shortSha(null)).isEqualTo("unknown");
    }

    @Test
    @DisplayName("a baked git.properties value is used when the image env sha is missing")
    void gitCommitUsesPropertiesWhenEnvIsUnknown() {
        Properties file = new Properties();
        file.setProperty("git.commit.id", "abcdef1234567890ffff");
        Assertions.assertThat(ReleaseInfoContributor.gitCommit("unknown", file)).isEqualTo("abcdef123456");
        Assertions.assertThat(ReleaseInfoContributor.gitCommit("1234567890abcdef", file)).isEqualTo("1234567890ab");
        Assertions.assertThat(ReleaseInfoContributor.gitCommit(null, null)).isEqualTo("unknown");
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
