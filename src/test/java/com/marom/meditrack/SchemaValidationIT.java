package com.marom.meditrack;

import com.marom.meditrack.support.MediTrackMariaDb;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

/**
 * Guards the {@code spring.jpa.hibernate.ddl-auto=validate} contract: the JPA
 * entities must line up with {@code db/meditrack_schema.sql} exactly. The
 * container loads that file on start; if an entity drifts from a column type or
 * name, Hibernate's schema validation fails and the context does not load.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
class SchemaValidationIT {

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        MediTrackMariaDb.registerProperties(registry);
    }

    @Test
    void should_validateEntitiesAgainstTheCheckedInSchema() {
        // The assertion is that the Spring context loaded at all.
    }
}
