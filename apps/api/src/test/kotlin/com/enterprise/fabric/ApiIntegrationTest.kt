package com.enterprise.fabric

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiIntegrationTest {
    @LocalServerPort var port: Int = 0
    @Autowired lateinit var rest: TestRestTemplate

    private fun url(path: String) = "http://localhost:$port$path"

    @Test fun `health and readiness return structured status`() {
        val health = rest.getForEntity(url("/health"), Map::class.java)
        assertEquals(HttpStatus.OK, health.statusCode)
        assertEquals("UP", health.body!!["status"])
        val ready = rest.getForEntity(url("/ready"), Map::class.java)
        assertEquals(HttpStatus.OK, ready.statusCode)
        assertEquals("READY", ready.body!!["status"])
    }

    @Test fun `tenant CRUD works`() {
        val req = mapOf("id" to "tenant-test", "name" to "Test Institution", "status" to "ACTIVE")
        val created = rest.postForEntity(url("/api/tenants"), req, Map::class.java)
        assertEquals(HttpStatus.OK, created.statusCode)
        assertEquals("tenant-test", created.body!!["id"])
        val fetched = rest.getForEntity(url("/api/tenants/tenant-test"), Map::class.java)
        assertEquals("Test Institution", fetched.body!!["name"])
        val updated = rest.exchange(url("/api/tenants/tenant-test"), HttpMethod.PUT, HttpEntity(mapOf("id" to "ignored", "name" to "Renamed", "status" to "ACTIVE")), Map::class.java)
        assertEquals("Renamed", updated.body!!["name"])
    }

    @Test fun `connector CRUD is tenant scoped`() {
        rest.postForEntity(url("/api/tenants"), mapOf("id" to "tenant-connectors", "name" to "Connector Tenant"), Map::class.java)
        val req = mapOf("id" to "conn-test", "name" to "Canvas", "systemType" to "LMS", "baseUrl" to "https://example.invalid", "credentialReference" to "vault://tenant-connectors/canvas")
        val created = rest.postForEntity(url("/api/tenants/tenant-connectors/connectors"), req, Map::class.java)
        assertEquals(HttpStatus.OK, created.statusCode)
        val wrongTenant = rest.getForEntity(url("/api/tenants/tenant-north/connectors/conn-test"), Map::class.java)
        assertEquals(HttpStatus.NOT_FOUND, wrongTenant.statusCode)
        assertEquals("NOT_FOUND", wrongTenant.body!!["code"])
    }

    @Test fun `event ingestion persists trace and correlation id`() {
        val payload = mapOf("tenant_id" to "tenant-north", "correlation_id" to "corr-test-ingest", "data" to mapOf("courseId" to "HIST-100", "title" to "History"))
        val accepted = rest.postForEntity(url("/api/events/lms-course"), payload, Map::class.java)
        assertEquals(HttpStatus.OK, accepted.statusCode)
        assertEquals("corr-test-ingest", accepted.body!!["correlation_id"])
        val trace = rest.getForEntity(url("/api/tenants/tenant-north/traces/corr-test-ingest"), List::class.java)
        assertEquals(HttpStatus.OK, trace.statusCode)
        assertTrue(trace.body!!.isNotEmpty())
    }

    @Test fun `missing correlation id is added`() {
        val payload = mapOf("tenant_id" to "tenant-north", "data" to mapOf("courseId" to "SCI-100", "title" to "Science"))
        val accepted = rest.postForEntity(url("/api/events/lms-course"), payload, Map::class.java)
        assertEquals(HttpStatus.OK, accepted.statusCode)
        assertNotNull(accepted.body!!["correlation_id"])
    }

    @Test fun `forced downstream failure moves to dlq`() {
        val payload = mapOf("tenant_id" to "tenant-north", "correlation_id" to "corr-test-dlq", "data" to mapOf("forceFailure" to true))
        val result = rest.postForEntity(url("/api/events/lms-course"), payload, Map::class.java)
        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("DLQ", result.body!!["status"])
        val dlq = rest.getForEntity(url("/api/tenants/tenant-north/dlq"), List::class.java)
        assertTrue(dlq.body!!.any { (it as Map<*, *>)["correlationId"] == "corr-test-dlq" || it["correlation_id"] == "corr-test-dlq" })
    }

    @Test fun `tenant isolation enforcement hides other tenant dlq`() {
        val north = rest.getForEntity(url("/api/tenants/tenant-north/dlq"), List::class.java)
        val east = rest.getForEntity(url("/api/tenants/tenant-east/dlq"), List::class.java)
        assertNotEquals(north.body, east.body)
    }

    @Test fun `malformed payload rejection is structured`() {
        val result = rest.postForEntity(url("/api/events/lms-course"), mapOf("tenant_id" to "tenant-north", "data" to mapOf("title" to "No Course Id")), Map::class.java)
        assertEquals(HttpStatus.BAD_REQUEST, result.statusCode)
        assertEquals("BAD_REQUEST", result.body!!["code"])
    }

    @Test fun `dev tenant header enforces tenant authorization`() {
        val headers = HttpHeaders()
        headers.add("X-Dev-Tenant-Ids", "tenant-north")
        val allowed = rest.exchange(url("/api/tenants/tenant-north/dlq"), HttpMethod.GET, HttpEntity<Void>(headers), List::class.java)
        assertEquals(HttpStatus.OK, allowed.statusCode)
        val denied = rest.exchange(url("/api/tenants/tenant-east/dlq"), HttpMethod.GET, HttpEntity<Void>(headers), Map::class.java)
        assertEquals(HttpStatus.FORBIDDEN, denied.statusCode)
        assertEquals("TENANT_ACCESS_DENIED", denied.body!!["code"])
    }

    @Test fun `tenant list is filtered by dev tenant header`() {
        val headers = HttpHeaders()
        headers.add("X-Dev-Tenant-Ids", "tenant-north")
        val result = rest.exchange(url("/api/tenants"), HttpMethod.GET, HttpEntity<Void>(headers), List::class.java)
        assertEquals(HttpStatus.OK, result.statusCode)
        val ids = result.body!!.map { (it as Map<*, *>)["id"] }
        assertTrue(ids.contains("tenant-north"))
        assertFalse(ids.contains("tenant-east"))
    }
}
