package com.enterprise.fabric.api

import jakarta.servlet.http.HttpServletRequest
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val code: String,
    val message: String,
    val path: String
)

class NotFoundException(message: String): RuntimeException(message)
class BadRequestException(message: String): RuntimeException(message)
class TenantAccessException(message: String): RuntimeException(message)

@RestControllerAdvice
class ApiExceptionHandler {
    @ExceptionHandler(NotFoundException::class)
    fun notFound(ex: NotFoundException, req: HttpServletRequest) = error(HttpStatus.NOT_FOUND, "NOT_FOUND", ex.message ?: "Not found", req)

    @ExceptionHandler(BadRequestException::class, MethodArgumentNotValidException::class, IllegalArgumentException::class)
    fun badRequest(ex: Exception, req: HttpServletRequest) = error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.message ?: "Bad request", req)

    @ExceptionHandler(TenantAccessException::class)
    fun forbidden(ex: TenantAccessException, req: HttpServletRequest) = error(HttpStatus.FORBIDDEN, "TENANT_ACCESS_DENIED", ex.message ?: "Tenant access denied", req)

    @ExceptionHandler(DuplicateKeyException::class)
    fun conflict(ex: DuplicateKeyException, req: HttpServletRequest) = error(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "Resource already exists", req)

    @ExceptionHandler(Exception::class)
    fun generic(ex: Exception, req: HttpServletRequest) = error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", req)

    private fun error(status: HttpStatus, code: String, message: String, req: HttpServletRequest): ResponseEntity<ApiError> =
        ResponseEntity.status(status).body(ApiError(status = status.value(), error = status.reasonPhrase, code = code, message = message, path = req.requestURI))
}
