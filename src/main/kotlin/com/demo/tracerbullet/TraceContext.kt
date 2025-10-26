package com.demo.tracerbullet

import java.time.Instant
import java.util.*

/**
 * TraceContext represents a single trace through the system.
 * It tracks a request as it flows through multiple services/endpoints.
 */
data class TraceContext(
    val traceId: String = UUID.randomUUID().toString(),
    val spanId: String = UUID.randomUUID().toString(),
    val parentSpanId: String? = null,
    val startTime: Instant = Instant.now(),
    val serviceName: String = "unknown"
) {
    fun createChild(newServiceName: String): TraceContext {
        return TraceContext(
            traceId = this.traceId,
            spanId = UUID.randomUUID().toString(),
            parentSpanId = this.spanId,
            startTime = Instant.now(),
            serviceName = newServiceName
        )
    }

    override fun toString(): String {
        return "TraceContext(traceId=$traceId, spanId=$spanId, parentSpanId=$parentSpanId, service=$serviceName)"
    }
}

/**
 * Holds trace contexts in a thread-local storage for easy access
 */
object TraceContextHolder {
    private val context = ThreadLocal<TraceContext>()

    fun set(traceContext: TraceContext) {
        context.set(traceContext)
    }

    fun get(): TraceContext? = context.get()

    fun clear() {
        context.remove()
    }
}
