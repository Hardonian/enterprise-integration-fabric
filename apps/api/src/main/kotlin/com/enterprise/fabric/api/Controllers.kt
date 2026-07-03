package com.enterprise.fabric.api

import com.enterprise.fabric.AppProperties
import com.enterprise.fabric.repo.*
import com.enterprise.fabric.security.TenantAccessService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.apache.camel.CamelExecutionException
import org.apache.camel.ProducerTemplate
import org.springframework.web.bind.annotation.*
import java.time.Instant

data class TenantRequest(@field:NotBlank val id: String, @field:NotBlank val name: String, val status: String = "ACTIVE")
data class ConnectorRequest(@field:NotBlank val id: String, @field:NotBlank val name: String, @field:NotBlank val systemType: String, @field:NotBlank val baseUrl: String, @field:NotBlank val credentialReference: String, val status: String = "ACTIVE")

@RestController
class SystemController(private val props: AppProperties, private val tenants: TenantRepo) {
    @GetMapping("/health") fun health() = mapOf("status" to "UP", "service" to "enterprise-integration-fabric-api")
    @GetMapping("/ready") fun ready() = mapOf("status" to "READY", "tenant_count" to tenants.list().size)
    @GetMapping("/version") fun version() = mapOf("version" to props.version, "time" to Instant.now().toString())
}

@RestController
@RequestMapping("/api/tenants")
class TenantController(private val tenants: TenantRepo, private val access: TenantAccessService) {
    @GetMapping fun list(): List<Tenant> {
        val all = tenants.list()
        val visible = access.visibleTenantIds(all.map { it.id }.toList()).toSet()
        return all.filter { visible.contains(it.id) }
    }
    @PostMapping fun create(@Valid @RequestBody req: TenantRequest): Tenant {
        access.requireTenant(req.id)
        return tenants.create(Tenant(req.id, req.name, req.status))
    }
    @GetMapping("/{id}") fun get(@PathVariable id: String): Tenant {
        access.requireTenant(id)
        return tenants.get(id)
    }
    @PutMapping("/{id}") fun update(@PathVariable id: String, @Valid @RequestBody req: TenantRequest): Tenant {
        access.requireTenant(id)
        return tenants.update(id, Tenant(id, req.name, req.status))
    }
    @DeleteMapping("/{id}") fun delete(@PathVariable id: String): Map<String, String> {
        access.requireTenant(id)
        tenants.delete(id)
        return mapOf("deleted" to id)
    }
}

@RestController
@RequestMapping("/api/tenants/{tenantId}/connectors")
class ConnectorController(private val connectors: ConnectorRepo, private val access: TenantAccessService) {
    @GetMapping fun list(@PathVariable tenantId: String): List<Connector> { access.requireTenant(tenantId); return connectors.list(tenantId) }
    @PostMapping fun create(@PathVariable tenantId: String, @Valid @RequestBody req: ConnectorRequest): Connector { access.requireTenant(tenantId); return connectors.create(Connector(req.id, tenantId, req.name, req.systemType, req.baseUrl, req.credentialReference, req.status)) }
    @GetMapping("/{id}") fun get(@PathVariable tenantId: String, @PathVariable id: String): Connector { access.requireTenant(tenantId); return connectors.get(tenantId, id) }
    @PutMapping("/{id}") fun update(@PathVariable tenantId: String, @PathVariable id: String, @Valid @RequestBody req: ConnectorRequest): Connector { access.requireTenant(tenantId); return connectors.update(tenantId, id, Connector(id, tenantId, req.name, req.systemType, req.baseUrl, req.credentialReference, req.status)) }
    @DeleteMapping("/{id}") fun delete(@PathVariable tenantId: String, @PathVariable id: String): Map<String, String> { access.requireTenant(tenantId); connectors.delete(tenantId, id); return mapOf("deleted" to id) }
}

@RestController
@RequestMapping("/api")
class EventController(private val producer: ProducerTemplate, private val events: EventRepo, private val access: TenantAccessService) {
    @PostMapping("/events/{route}")
    fun ingest(@PathVariable route: String, @RequestBody payload: Map<String, Any?>): Any {
        val endpoint = when(route) {
            "lms-course" -> "direct:lms-course-inbound"
            "sis-enrollment" -> "direct:sis-enrollment-inbound"
            "grade-sync" -> "direct:grade-sync-outbound"
            "crm-lead" -> "direct:crm-lead-handoff"
            "billing-customer" -> "direct:billing-customer-sync"
            else -> throw NotFoundException("Unknown event route: $route")
        }
        val tenantId = payload["tenant_id"] as? String ?: throw BadRequestException("tenant_id is required")
        access.requireTenant(tenantId)
        return routeTo(endpoint, payload)
    }
    @PostMapping("/simulations/grade-sync") fun grade(@RequestBody payload: Map<String, Any?>): Any { requirePayloadTenant(payload); return routeTo("direct:grade-sync-outbound", payload) }
    @PostMapping("/simulations/crm-handoff") fun crm(@RequestBody payload: Map<String, Any?>): Any { requirePayloadTenant(payload); return routeTo("direct:crm-lead-handoff", payload) }
    @PostMapping("/simulations/billing-event") fun billing(@RequestBody payload: Map<String, Any?>): Any { requirePayloadTenant(payload); return routeTo("direct:billing-customer-sync", payload) }
    @GetMapping("/tenants/{tenantId}/traces/{correlationId}") fun traces(@PathVariable tenantId: String, @PathVariable correlationId: String): List<EventTrace> { access.requireTenant(tenantId); return events.traces(tenantId, correlationId) }
    @GetMapping("/tenants/{tenantId}/dlq") fun dlq(@PathVariable tenantId: String): List<DeadLetterEvent> { access.requireTenant(tenantId); return events.dlq(tenantId) }
    @GetMapping("/tenants/{tenantId}/entitlements") fun entitlements(@PathVariable tenantId: String, @RequestParam(required=false) subjectId: String?): List<Entitlement> { access.requireTenant(tenantId); return events.entitlements(tenantId, subjectId) }

    private fun requirePayloadTenant(payload: Map<String, Any?>) {
        val tenantId = payload["tenant_id"] as? String ?: throw BadRequestException("tenant_id is required")
        access.requireTenant(tenantId)
    }

    private fun routeTo(endpoint: String, payload: Map<String, Any?>): Any = try {
        producer.requestBody(endpoint, payload) as Any
    } catch (ex: CamelExecutionException) {
        val cause = ex.cause
        if (cause is BadRequestException) throw cause
        throw ex
    }
}
