package com.enterprise.fabric.security

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import io.github.bucket4j.Refill
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class RateLimitFilter : Filter {
    private val buckets = ConcurrentHashMap<String, Bucket>()

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req = request as HttpServletRequest
        val res = response as HttpServletResponse

        if (req.requestURI.startsWith("/api/")) {
            val tenantId = extractTenantId(req.requestURI)
            
            if (tenantId != null) {
                val bucket = buckets.computeIfAbsent(tenantId) {
                    val limit = Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1)))
                    Bucket.builder().addLimit(limit).build()
                }

                if (!bucket.tryConsume(1)) {
                    res.status = HttpStatus.TOO_MANY_REQUESTS.value()
                    res.writer.write("""{"error": "Rate limit exceeded for tenant"}""")
                    return
                }
            }
        }
        chain.doFilter(request, response)
    }

    private fun extractTenantId(uri: String): String? {
        val parts = uri.split("/")
        val tIdx = parts.indexOf("tenants")
        if (tIdx != -1 && tIdx + 1 < parts.size) {
            return parts[tIdx + 1]
        }
        return null
    }
}
