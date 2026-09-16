package com.ozerler.marble.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * In-app production deploy settings. Enabled only on the Linux host (prod profile).
 */
@Configuration
@ConfigurationProperties(prefix = "app.deployment")
@Getter
@Setter
public class DeploymentProperties {

    /**
     * Master switch. Keep false on local/dev/Windows.
     */
    private boolean enabled = false;

    /**
     * Root-owned wrapper installed by {@code scripts/deploy_production.sh}.
     */
    private String command = "/usr/local/sbin/marble-erp-inapp-deploy";

    /**
     * Git clone that {@code git pull origin main} runs against.
     */
    private String repoDir = "/home/eyuce/marble-erp-platform";

    private String logFile = "/opt/marble-erp/logs/inapp-deploy.log";

    private String statusFile = "/opt/marble-erp/logs/inapp-deploy.status";

    private String lockFile = "/opt/marble-erp/logs/inapp-deploy.lock";

    private String gitRemote = "origin";

    private String gitBranch = "main";

    /**
     * The systemd service user is not root, so the wrapper is invoked with {@code sudo -n}.
     */
    private boolean useSudo = true;

    /**
     * Treat a RUNNING status older than this as stale (crash / reboot mid-deploy).
     */
    private int staleRunningMinutes = 45;

    /**
     * Maximum log bytes returned per poll.
     */
    private int logChunkBytes = 32768;
}
