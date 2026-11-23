package com.ozerler.marble.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.core.io.support.SpringFactoriesLoader;

class EnvironmentPostProcessorPackagingIT {

    private static final String SPRING_FACTORIES = "META-INF/spring.factories";
    private static final String FACTORIES_KEY =
            "org.springframework.boot.EnvironmentPostProcessor";

    @Test
    @DisplayName("the executable jar keeps spring.factories where java -jar can load it")
    void executableJarRegistersPostProcessorsInSpringFactories() throws Exception {
        Path jar = findExecutableJar();
        Assertions.assertThat(jar).isRegularFile();

        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Assertions.assertThat(jarFile.getJarEntry(SPRING_FACTORIES))
                    .as("Boot 4.2 leaves application META-INF at the jar root")
                    .isNotNull();
            Assertions.assertThat(
                            jarFile.getJarEntry("BOOT-INF/classes/" + SPRING_FACTORIES))
                    .as("the packager does not move META-INF/spring.factories under BOOT-INF")
                    .isNull();

            Assertions.assertThat(jarFile.getJarEntry(
                            "BOOT-INF/classes/com/ozerler/marble/config/"
                                    + "SecretFileEnvironmentPostProcessor.class"))
                    .isNotNull();
            Assertions.assertThat(jarFile.getJarEntry(
                            "BOOT-INF/classes/com/ozerler/marble/config/"
                                    + "DatabaseUrlEnvironmentPostProcessor.class"))
                    .isNotNull();

            Properties factories = loadFactories(jarFile);
            Assertions.assertThat(factories.getProperty(FACTORIES_KEY))
                    .contains(SecretFileEnvironmentPostProcessor.class.getName())
                    .contains(DatabaseUrlEnvironmentPostProcessor.class.getName());
        }

        URL jarUrl = jar.toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {jarUrl}, null)) {
            List<String> names = SpringFactoriesLoader.loadFactoryNames(
                    EnvironmentPostProcessor.class, loader);
            Assertions.assertThat(names)
                    .contains(
                            DatabaseUrlEnvironmentPostProcessor.class.getName(),
                            SecretFileEnvironmentPostProcessor.class.getName());
        }
    }

    private static Properties loadFactories(JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getJarEntry(SPRING_FACTORIES);
        Assertions.assertThat(entry).isNotNull();
        Properties properties = new Properties();
        try (InputStream input = jarFile.getInputStream(entry)) {
            properties.load(input);
        }
        Assertions.assertThat(properties.getProperty(FACTORIES_KEY))
                .isNotBlank();
        return properties;
    }

    private static Path findExecutableJar() throws IOException {
        Path target = Path.of(System.getProperty("project.build.directory", "target"));
        Assertions.assertThat(target)
                .as("project.build.directory must exist after package")
                .isDirectory();
        List<Path> jars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(target, "*.jar")) {
            for (Path candidate : stream) {
                String name = candidate.getFileName().toString();
                if (name.endsWith("-sources.jar") || name.endsWith("-javadoc.jar")) {
                    continue;
                }
                jars.add(candidate);
            }
        }
        Assertions.assertThat(jars)
                .as("the packaged executable jar must exist under target/")
                .hasSize(1);
        return jars.get(0);
    }
}
