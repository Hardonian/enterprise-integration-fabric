package com.enterprise.fabric.security

import com.enterprise.fabric.AppProperties
import com.enterprise.fabric.api.TenantAccessException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Service

@Service
class TenantAccessService(private val props: AppProperties, private val request: HttpServletRequest) {
    fun requireTenant(tenantId: String) {
        if (!canAccess(tenantId)) throw TenantAccessException("Caller is not authorized for tenant: $tenantId")
    }

    fun visibleTenantIds(allTenantIds: List<String>): List<String> {
        val allowed = allowedTenantIds()
        if (isAdmin() || allowed == null) return allTenantIds
        return allTenantIds.filter { allowed.contains(it) }
    }

    private fun canAccess(tenantId: String): Boolean {
        if (isAdmin()) return true
        val allowed = allowedTenantIds()
        return allowed == null || allowed.contains(tenantId)
    }

    private fun allowedTenantIds(): Set<String>? {
        if (props.devAuthEnabled) {
            val header = request.getHeader("X-Dev-Tenant-Ids")?.trim().orEmpty()
            if (header.isBlank()) return null
            return header.split(',').map { it.trim() }.filter { it.isNotBlank() }.toSet()
        }
        val auth = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken ?: return emptySet()
        val jwt: Jwt = auth.token
        val tenants = mutableSetOf<String>()
        jwt.getClaimAsString("tenant_id")?.takeIf { it.isNotBlank() }?.let { tenants.add(it) }
        jwt.getClaimAsStringList("tenant_ids")?.let { tenants.addAll(it) }
        jwt.getClaimAsStringList("tenants")?.let { tenants.addAll(it) }
        return tenants
    }

    private fun isAdmin(): Boolean {
        if (props.devAuthEnabled && request.getHeader("X-Dev-Admin") == "true") return true
        val auth = SecurityContextHolder.getContext().authentication as? JwtAuthenticationToken ?: return false
        val jwt = auth.token
        val realmAccess = jwt.claims["realm_access"] as? Map<*, *>
        val roles = realmAccess?.get("roles") as? Collection<*>
        return roles?.contains("fabric-admin") == true || auth.authorities.any { it.authority == "ROLE_fabric-admin" || it.authority == "SCOPE_fabric-admin" }
    }
}
