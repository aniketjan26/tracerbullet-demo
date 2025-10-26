package com.demo.tracerbullet

import org.http4k.core.*
import org.http4k.filter.ResponseFilters
import java.time.Duration
import java.time.Instant

/**
 * TracerBullet is an http4k Filter that implements distributed tracing.
 *
 * It works by:
 * 1. Extracting trace information from incoming request headers (or creating new trace)
 * 2. Storing trace context in thread-local storage
 * 3. Adding trace headers to the response
 * 4. Logging the complete trace information
 *
 * This allows you to track a request as it flows through multiple services,
 * creating a "tracer bullet" path through your system.
 */
object TracerBullet {
    // Standard trace header names
    const val TRACE_ID_HEADER = "X-Trace-Id"
    const val SPAN_ID_HEADER = "X-Span-Id"
    const val PARENT_SPAN_ID_HEADER = "X-Parent-Span-Id"
    const val SERVICE_NAME_HEADER = "X-Service-Name"

    /**
     * Creates a server-side filter that handles incoming requests
     */
    fun serverFilter(serviceName: String): Filter = Filter { next ->
        { request ->
            // Extract or create trace context from request headers
            val traceContext = extractTraceContext(request, serviceName)

            // Store in thread-local for easy access throughout the request
            TraceContextHolder.set(traceContext)

            val startTime = Instant.now()

            try {
                // Process the request
                val response = next(request)

                val duration = Duration.between(startTime, Instant.now())

                // Log the trace
                logTrace(traceContext, request, response, duration)

                // Add trace headers to response
                addTraceHeaders(response, traceContext)
            } finally {
                // Clean up thread-local
                TraceContextHolder.clear()
            }
        }
    }

    /**
     * Creates a client-side filter that propagates trace context to downstream services
     */
    fun clientFilter(targetServiceName: String): Filter = Filter { next ->
        { request ->
            val currentContext = TraceContextHolder.get()

            val newRequest = if (currentContext != null) {
                // Create a child span for the downstream service call
                val childContext = currentContext.createChild(targetServiceName)

                request
                    .header(TRACE_ID_HEADER, childContext.traceId)
                    .header(SPAN_ID_HEADER, childContext.spanId)
                    .header(PARENT_SPAN_ID_HEADER, childContext.parentSpanId ?: "")
                    .header(SERVICE_NAME_HEADER, targetServiceName)
            } else {
                request
            }

            next(newRequest)
        }
    }

    private fun extractTraceContext(request: Request, serviceName: String): TraceContext {
        val traceId = request.header(TRACE_ID_HEADER)
        val spanId = request.header(SPAN_ID_HEADER)
        val parentSpanId = request.header(PARENT_SPAN_ID_HEADER)

        return if (traceId != null && spanId != null) {
            // Continue existing trace
            TraceContext(
                traceId = traceId,
                spanId = spanId,
                parentSpanId = parentSpanId?.takeIf { it.isNotEmpty() },
                serviceName = serviceName
            )
        } else {
            // Start new trace
            TraceContext(serviceName = serviceName)
        }
    }

    private fun addTraceHeaders(response: Response, context: TraceContext): Response {
        return response
            .header(TRACE_ID_HEADER, context.traceId)
            .header(SPAN_ID_HEADER, context.spanId)
            .header(PARENT_SPAN_ID_HEADER, context.parentSpanId ?: "")
    }

    private fun logTrace(
        context: TraceContext,
        request: Request,
        response: Response,
        duration: Duration
    ) {
        val parentInfo = context.parentSpanId?.let { " [parent=$it]" } ?: " [root]"
        println("""
            |========================================
            |TRACE: ${context.traceId}
            |SPAN: ${context.spanId}$parentInfo
            |SERVICE: ${context.serviceName}
            |METHOD: ${request.method} ${request.uri}
            |STATUS: ${response.status.code}
            |DURATION: ${duration.toMillis()}ms
            |========================================
        """.trimMargin())
    }
}
