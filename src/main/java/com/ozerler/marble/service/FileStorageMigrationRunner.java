package com.ozerler.marble.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.migrate.enabled", havingValue = "true")
public class FileStorageMigrationRunner implements ApplicationRunner {

    private final FileStorageMigrationService fileStorageMigrationService;
    private final com.ozerler.marble.config.ObjectStorageProperties storageProperties;

    @Override
    public void run(ApplicationArguments args) {
        boolean dryRun = storageProperties.getMigrate().isDryRun();
        log.info("Starting leftover filesystem file migration dryRun={}", dryRun);
        FileStorageMigrationService.MigrationResult result = fileStorageMigrationService.migrate(dryRun);
        log.info("Filesystem file migration finished examined={} migrated={} skipped={} failed={} dryRun={}",
                result.examined(), result.migrated(), result.skipped(), result.failed(), result.dryRun());
    }
}
