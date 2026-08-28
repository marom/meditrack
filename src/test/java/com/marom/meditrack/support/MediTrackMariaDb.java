package com.marom.meditrack.support;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * One MariaDB container shared by every {@code *IT} in the JVM. Started once in a
 * static initializer and never stopped explicitly — Ryuk tears it down when the
 * JVM exits.
 *
 * <p>The schema is loaded from the real {@code db/meditrack_schema.sql} so the
 * tests fail if it drifts from the entities. Only the environment-specific
 * preamble/epilogue is stripped: a fresh per-run container already <em>is</em> an
 * isolated {@code meditrack_db}, so {@code DROP/CREATE/USE DATABASE} and the
 * trailing verification {@code SELECT}s don't apply.
 */
public final class MediTrackMariaDb {

    private static final Path SCHEMA_FILE = Path.of("db/meditrack_schema.sql");

    private static final MariaDBContainer<?> CONTAINER =
            new MariaDBContainer<>(DockerImageName.parse("mariadb:11.4"))
                    .withDatabaseName("meditrack_db");

    static {
        CONTAINER.start();
        loadSchema();
    }

    private MediTrackMariaDb() {
    }

    /** Point Spring's datasource at the running container. */
    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", CONTAINER::getUsername);
        registry.add("spring.datasource.password", CONTAINER::getPassword);
        registry.add("spring.datasource.driver-class-name", CONTAINER::getDriverClassName);
    }

    private static void loadSchema() {
        String raw;
        try {
            raw = Files.readString(SCHEMA_FILE);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + SCHEMA_FILE.toAbsolutePath(), e);
        }
        String sql = raw.lines()
                .filter(line -> {
                    String t = line.strip().toUpperCase(Locale.ROOT);
                    return !(t.startsWith("DROP DATABASE")
                            || t.startsWith("CREATE DATABASE")
                            || t.startsWith("USE ")
                            || t.startsWith("SELECT "));
                })
                .collect(Collectors.joining("\n"));

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8), SCHEMA_FILE.toString()));
        populator.setSeparator(";");
        populator.setCommentPrefixes("--");
        populator.setContinueOnError(false);

        try (Connection connection = CONTAINER.createConnection("")) {
            populator.populate(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to load " + SCHEMA_FILE + " into the test container", e);
        }
    }
}
