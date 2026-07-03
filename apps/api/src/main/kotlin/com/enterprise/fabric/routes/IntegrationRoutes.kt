package com.enterprise.fabric.routes

import com.enterprise.fabric.AppProperties
import com.enterprise.fabric.api.BadRequestException
import com.enterprise.fabric.repo.EventRepo
import com.enterprise.fabric.repo.TenantRepo
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.UUID

@Component
class IntegrationRoutes(private val events: EventRepo, private val tenants: TenantRepo, private val props: AppProperties) : RouteBuilder() {
    override fun configure() {
        onException(BadRequestException::class.java)
            .handled(false)

        onException(Exception::class.java)
            .maximumRedeliveries(2).redeliveryDelay(50).handled(true)
            .process { ex ->
                val body = ex.getIn().body as Map<String, Any?>
                val tenantId = body["tenant_id"] as? String ?: "unknown"
                val type = body["type"] as? String ?: "unknown"
                val corr = body["correlation_id"] as? String ?: UUID.randomUUID().toString()
                val reason = ex.getProperty(Exchange.EXCEPTION_CAUGHT, Exception::class.java)?.message ?: "Route failure"
                events.deadLetter(tenantId, type, corr, reason, body)
                events.trace(tenantId, null, corr, ex.fromRouteId ?: "route", "dead-letter", "DLQ", reason)
                ex.message.body = mapOf("status" to "DLQ", "tenant_id" to tenantId, "correlation_id" to corr, "reason" to reason)
            }

        route("lms-course-inbound", "lms.course.created")
        route("sis-enrollment-inbound", "sis.enrollment.updated")
        route("grade-sync-outbound", "lms.grade.published")
        route("crm-lead-handoff", "crm.lead.created")
        route("billing-customer-sync", "billing.customer.updated")

        val brokerRoute = from("direct:brokerOut").routeId("broker-out")
        if (props.brokerEnabled) {
            brokerRoute.toD("kafka:\${header.eventType}?brokers=${props.kafkaBootstrap}")
        } else {
            brokerRoute.log("Broker disabled; accepted event \${header.eventType} correlation \${header.correlationId}")
        }
    }

    private fun route(routeId: String, eventType: String) {
        from("direct:$routeId").routeId(routeId)
            .process { ex ->
                val raw = ex.getIn().body
                @Suppress("UNCHECKED_CAST") val incoming = raw as? Map<String, Any?> ?: throw BadRequestException("Payload must be an object")
                val normalized = envelope(incoming, eventType)
                val tenantId = normalized["tenant_id"] as String
                if (!tenants.exists(tenantId)) throw BadRequestException("Unknown tenant_id: $tenantId")
                @Suppress("UNCHECKED_CAST") val data = normalized["data"] as Map<String, Any?>
                validateData(eventType, data)
                if (data["forceFailure"] == true) throw IllegalStateException("Forced downstream failure for DLQ verification")
                val corr = normalized["correlation_id"] as String
                val eventId = events.saveEvent(tenantId, normalized["connector_id"] as? String, eventType, corr, normalized, "ACCEPTED")
                events.trace(tenantId, eventId, corr, routeId, "validate-persist", "OK", "Payload accepted")
                ex.getIn().setHeader("eventType", eventType)
                ex.getIn().setHeader("correlationId", corr)
                ex.getIn().body = normalized + mapOf("id" to eventId)
            }
            .wireTap("direct:brokerOut")
            .process { ex ->
                @Suppress("UNCHECKED_CAST") val body = ex.getIn().body as Map<String, Any?>
                ex.message.body = mapOf("status" to "ACCEPTED", "event_id" to body["id"], "tenant_id" to body["tenant_id"], "correlation_id" to body["correlation_id"], "type" to body["type"])
            }
    }

    private fun envelope(payload: Map<String, Any?>, defaultType: String): Map<String, Any?> {
        val tenant = payload["tenant_id"] as? String ?: throw BadRequestException("tenant_id is required")
        return mapOf(
            "id" to (payload["id"] ?: UUID.randomUUID().toString()),
            "source" to (payload["source"] ?: "enterprise-integration-fabric/api"),
            "specversion" to (payload["specversion"] ?: "1.0"),
            "type" to (payload["type"] ?: defaultType),
            "subject" to (payload["subject"] ?: tenant),
            "time" to (payload["time"] ?: Instant.now().toString()),
            "datacontenttype" to (payload["datacontenttype"] ?: "application/json"),
            "tenant_id" to tenant,
            "connector_id" to payload["connector_id"],
            "correlation_id" to (payload["correlation_id"] ?: UUID.randomUUID().toString()),
            "data" to (payload["data"] ?: throw BadRequestException("data is required"))
        )
    }

    private fun validateData(type: String, data: Map<String, Any?>) {
        val required = when(type) {
            "lms.course.created" -> listOf("courseId", "title")
            "sis.enrollment.updated" -> listOf("courseId", "learnerId", "status")
            "lms.grade.published" -> listOf("courseId", "learnerId", "grade")
            "crm.lead.created" -> listOf("institutionId", "contactEmail")
            "billing.customer.updated" -> listOf("externalCustomerId", "status")
            else -> listOf("forceFailure")
        }
        val missing = required.filter { !data.containsKey(it) }
        if (missing.isNotEmpty() && data["forceFailure"] != true) throw BadRequestException("Missing data fields: ${'$'}missing")
    }
}
