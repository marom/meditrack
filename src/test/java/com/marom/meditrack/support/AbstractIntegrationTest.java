package com.marom.meditrack.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Base for end-to-end HTTP → controller → service → repository → MariaDB tests.
 *
 * <p>{@code webEnvironment = MOCK} + {@link MockMvc} + {@link Transactional} keeps
 * each request on the test thread and rolls it back afterwards: the schema seed
 * data (4 specialties, 5 doctors, 3 patients) survives, rows a test creates do
 * not. Do not switch to {@code RANDOM_PORT}/{@code TestRestTemplate} — that runs
 * the request on another thread and the rollback no longer applies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Transactional
public abstract class AbstractIntegrationTest {

    protected final MockMvc mockMvc;
    protected final ObjectMapper objectMapper;

    protected AbstractIntegrationTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        MediTrackMariaDb.registerProperties(registry);
    }
}
