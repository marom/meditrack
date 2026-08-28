package com.marom.meditrack.support;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for {@link DataJpaTest} repository slice tests. {@code replace = NONE}
 * keeps the Testcontainers MariaDB datasource instead of swapping in an embedded
 * DB (there is no embedded driver on the classpath anyway). {@code @DataJpaTest}
 * is transactional, so each test rolls back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryIT {

    protected final TestEntityManager em;

    protected AbstractRepositoryIT(TestEntityManager em) {
        this.em = em;
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        MediTrackMariaDb.registerProperties(registry);
    }
}
