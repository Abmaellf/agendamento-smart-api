package com.agendamento.smart;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTestSupport {

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:18")
                    .withDatabaseName("agenda_integration")
                    .withUsername("agenda_test")
                    .withPassword("agenda_test_password");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("api.security.token.secret",
                () -> "integration-test-secret-structure-change-2026");
        registry.add("api.security.cookie.secure", () -> false);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    protected void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    PROFESSIONAL_SERVICE,
                    APPOINTMENT,
                    PATIENT_UNIT,
                    PROFESSIONAL,
                    SERVICE_OFFERING,
                    PATIENT,
                    CLINIC_UNIT,
                    USERS,
                    CLINIC
                CASCADE
                """);
    }
}
