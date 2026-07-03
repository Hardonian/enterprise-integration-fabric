package com.enterprise.fabric

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Optional Testcontainers probe for developer machines/CI runners whose Docker API supports docker-java.
 * Default tests use H2 for speed and reliability in restricted Docker-in-Docker environments.
 */
@Testcontainers
@Tag("container")
@EnabledIfEnvironmentVariable(named = "RUN_TESTCONTAINERS", matches = "true")
class MariaDbTestcontainersTemplate {
    companion object {
        @Container val db = MariaDBContainer("mariadb:11.4")
            .withDatabaseName("fabric_test")
            .withUsername("fabric")
            .withPassword("fabric")
    }
    @Test fun `mariadb testcontainer starts`() { assertTrue(db.isRunning) }
}
