package com.enterprise.fabric.repo

import com.enterprise.fabric.api.NotFoundException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class Tenant(val id: String, val name: String, val status: String, val createdAt: Instant? = null)
data class Connector(val id: String, val tenantId: String, val name: String, val systemType: String, val baseUrl: String, val credentialReference: String, val status: String, val createdAt: Instant? = null)
data class IntegrationEvent(val id: String, val tenantId: String, val connectorId: String?, val eventType: String, val correlationId: String, val payload: Map<String, Any?>, val status: String, val createdAt: Instant? = null)
data class EventTrace(val id: String, val tenantId: String, val eventId: String?, val correlationId: String, val routeName: String, val step: String, val status: String, val message: String, val createdAt: Instant? = null)
data class DeadLetterEvent(val id: String, val tenantId: String, val eventType: String, val correlationId: String, val failureReason: String, val payload: Map<String, Any?>, val createdAt: Instant? = null)
data class Entitlement(val id: String, val tenantId: String, val subjectId: String, val productCode: String, val status: String)

@Repository
class TenantRepo(private val jdbc: JdbcTemplate) {
    private val mapper = RowMapper { rs: ResultSet, _: Int -> Tenant(rs.getString("id"), rs.getString("name"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()) }
    fun list() = jdbc.query("select * from tenants order by created_at desc", mapper)
    fun get(id: String) = jdbc.query("select * from tenants where id=?", mapper, id).firstOrNull() ?: throw NotFoundException("Tenant not found: $id")
    fun create(t: Tenant): Tenant { jdbc.update("insert into tenants(id,name,status) values (?,?,?)", t.id, t.name, t.status); return get(t.id) }
    fun update(id: String, t: Tenant): Tenant { get(id); jdbc.update("update tenants set name=?, status=? where id=?", t.name, t.status, id); return get(id) }
    fun delete(id: String) { get(id); jdbc.update("delete from tenants where id=?", id) }
    fun exists(id: String) = jdbc.queryForObject("select count(*) from tenants where id=?", Long::class.java, id)!! > 0
}

@Repository
class ConnectorRepo(private val jdbc: JdbcTemplate, private val tenants: TenantRepo) {
    private val mapper = RowMapper { rs: ResultSet, _: Int -> Connector(rs.getString("id"), rs.getString("tenant_id"), rs.getString("name"), rs.getString("system_type"), rs.getString("base_url"), rs.getString("credential_reference"), rs.getString("status"), rs.getTimestamp("created_at").toInstant()) }
    fun list(tenantId: String) = jdbc.query("select * from connectors where tenant_id=? order by created_at desc", mapper, tenantId)
    fun get(tenantId: String, id: String) = jdbc.query("select * from connectors where tenant_id=? and id=?", mapper, tenantId, id).firstOrNull() ?: throw NotFoundException("Connector not found for tenant")
    fun create(c: Connector): Connector { tenants.get(c.tenantId); jdbc.update("insert into connectors(id,tenant_id,name,system_type,base_url,credential_reference,status) values (?,?,?,?,?,?,?)", c.id, c.tenantId, c.name, c.systemType, c.baseUrl, c.credentialReference, c.status); return get(c.tenantId, c.id) }
    fun update(tenantId: String, id: String, c: Connector): Connector { get(tenantId,id); jdbc.update("update connectors set name=?, system_type=?, base_url=?, credential_reference=?, status=? where tenant_id=? and id=?", c.name, c.systemType, c.baseUrl, c.credentialReference, c.status, tenantId, id); return get(tenantId,id) }
    fun delete(tenantId: String, id: String) { get(tenantId,id); jdbc.update("delete from connectors where tenant_id=? and id=?", tenantId, id) }
}

@Repository
class EventRepo(private val jdbc: JdbcTemplate, private val mapper: ObjectMapper) {
    fun saveEvent(tenantId: String, connectorId: String?, eventType: String, correlationId: String, payload: Map<String, Any?>, status: String): String {
        val id = "evt-" + UUID.randomUUID().toString()
        jdbc.update("insert into integration_events(id,tenant_id,connector_id,event_type,correlation_id,payload,status) values (?,?,?,?,?,?,?)", id, tenantId, connectorId, eventType, correlationId, mapper.writeValueAsString(payload), status)
        return id
    }
    fun trace(tenantId: String, eventId: String?, correlationId: String, routeName: String, step: String, status: String, message: String) {
        jdbc.update("insert into event_traces(id,tenant_id,event_id,correlation_id,route_name,step,status,message) values (?,?,?,?,?,?,?,?)", "trace-" + UUID.randomUUID(), tenantId, eventId, correlationId, routeName, step, status, message)
    }
    fun deadLetter(tenantId: String, eventType: String, correlationId: String, reason: String, payload: Map<String, Any?>) {
        jdbc.update("insert into dead_letter_events(id,tenant_id,event_type,correlation_id,failure_reason,payload) values (?,?,?,?,?,?)", "dlq-" + UUID.randomUUID(), tenantId, eventType, correlationId, reason, mapper.writeValueAsString(payload))
    }
    fun traces(tenantId: String, correlationId: String): List<EventTrace> = jdbc.query("select * from event_traces where tenant_id=? and correlation_id=? order by created_at asc", traceMapper, tenantId, correlationId)
    fun dlq(tenantId: String): List<DeadLetterEvent> = jdbc.query("select * from dead_letter_events where tenant_id=? order by created_at desc", dlqMapper, tenantId)
    fun entitlements(tenantId: String, subjectId: String?) = if (subjectId == null) jdbc.query("select * from entitlements where tenant_id=? order by created_at desc", entMapper, tenantId) else jdbc.query("select * from entitlements where tenant_id=? and subject_id=? order by created_at desc", entMapper, tenantId, subjectId)
    private val traceMapper = RowMapper { rs: ResultSet, _: Int -> EventTrace(rs.getString("id"), rs.getString("tenant_id"), rs.getString("event_id"), rs.getString("correlation_id"), rs.getString("route_name"), rs.getString("step"), rs.getString("status"), rs.getString("message"), rs.getTimestamp("created_at").toInstant()) }
    private val dlqMapper = RowMapper { rs: ResultSet, _: Int -> DeadLetterEvent(rs.getString("id"), rs.getString("tenant_id"), rs.getString("event_type"), rs.getString("correlation_id"), rs.getString("failure_reason"), mapper.readValue(rs.getString("payload"), Map::class.java) as Map<String, Any?>, rs.getTimestamp("created_at").toInstant()) }
    private val entMapper = RowMapper { rs: ResultSet, _: Int -> Entitlement(rs.getString("id"), rs.getString("tenant_id"), rs.getString("subject_id"), rs.getString("product_code"), rs.getString("status")) }
}
