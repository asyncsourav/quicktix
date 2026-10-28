

package com.asyncsourav.quicktix;


import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;



@SpringBootTest
@Testcontainers
@ActiveProfiles("dev")
public abstract class BaseIntegrationTest {


    @SuppressWarnings("resource")
    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("quicktix_test")
            .withUsername("testuser")
            .withPassword("testpass")
            .withCommand("postgres", "-c", "timezone=UTC");
}
