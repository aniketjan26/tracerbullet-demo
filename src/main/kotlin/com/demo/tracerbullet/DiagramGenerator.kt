package com.demo.tracerbullet

import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Generates PlantUML sequence diagrams from trace data
 */
object DiagramGenerator {
    private val outputDir = File("diagrams")
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")

    init {
        // Create diagrams directory
        outputDir.mkdirs()
    }

    /**
     * Generate a PlantUML sequence diagram for a trace
     */
    fun generateDiagram(trace: TraceData): File {
        val plantuml = buildPlantUML(trace)
        val fileName = "trace-${trace.traceId.take(8)}.puml"
        val file = File(outputDir, fileName)

        file.writeText(plantuml)

        println("""
            |
            |📊 DIAGRAM GENERATED: ${file.absolutePath}
            |   View online: https://www.plantuml.com/plantuml/uml/
            |   Or use VSCode PlantUML extension
            |
        """.trimMargin())

        // Also generate a simple text summary
        generateTextSummary(trace)

        return file
    }

    /**
     * Build PlantUML content from trace data
     */
    private fun buildPlantUML(trace: TraceData): String {
        val spans = trace.getAllSpansOrdered()
        if (spans.isEmpty()) return ""

        val sb = StringBuilder()
        val rootSpan = trace.getRootSpan()

        // Header
        sb.appendLine("@startuml")
        sb.appendLine("title Trace: ${trace.traceId.take(12)}...")
        sb.appendLine()

        // Styling
        sb.appendLine("skinparam sequenceMessageAlign center")
        sb.appendLine("skinparam responseMessageBelowArrow true")
        sb.appendLine("skinparam BoxPadding 10")
        sb.appendLine()

        // Participants (services)
        val services = spans.map { it.serviceName }.distinct()
        services.forEach { service ->
            sb.appendLine("participant \"$service\" as ${sanitizeId(service)}")
        }
        sb.appendLine()

        // Add actor for the initial request
        if (rootSpan != null) {
            sb.appendLine("actor Client")
            sb.appendLine()
        }

        // Generate sequence of calls
        generateSequence(sb, trace, spans)

        // Footer with summary
        sb.appendLine()
        sb.appendLine("note over ${sanitizeId(services.first())}")
        sb.appendLine("**Trace Summary**")
        sb.appendLine("Trace ID: ${trace.traceId.take(16)}...")
        sb.appendLine("Total Spans: ${spans.size}")
        sb.appendLine("Total Duration: ${calculateTotalDuration(spans)}ms")
        val errors = spans.count { it.status.code >= 400 }
        if (errors > 0) {
            sb.appendLine("⚠️ Errors: $errors")
        } else {
            sb.appendLine("✓ Success")
        }
        sb.appendLine("end note")

        sb.appendLine()
        sb.appendLine("@enduml")

        return sb.toString()
    }

    /**
     * Generate the sequence of interactions
     */
    private fun generateSequence(sb: StringBuilder, trace: TraceData, spans: List<SpanData>) {
        val rootSpan = trace.getRootSpan() ?: return

        // Initial request from client
        val rootService = sanitizeId(rootSpan.serviceName)
        sb.appendLine("Client -> $rootService: ${rootSpan.method} ${rootSpan.uri}")
        sb.appendLine("activate $rootService")

        // Build a map of parent to children
        val spanMap = spans.associateBy { it.spanId }
        val childrenMap = spans.groupBy { it.parentSpanId }

        // Process each span recursively
        processSpan(sb, rootSpan, childrenMap, spanMap, 1)

        // Return response
        val statusColor = getStatusColor(rootSpan.status.code)
        sb.appendLine("$rootService --> Client: $statusColor **${rootSpan.status.code}** ${rootSpan.status.description}")
        sb.appendLine("deactivate $rootService")
        sb.appendLine("note right: ${rootSpan.duration.toMillis()}ms")
    }

    /**
     * Recursively process spans and their children
     */
    private fun processSpan(
        sb: StringBuilder,
        span: SpanData,
        childrenMap: Map<String?, List<SpanData>>,
        spanMap: Map<String, SpanData>,
        depth: Int
    ) {
        val children = childrenMap[span.spanId] ?: emptyList()

        for (child in children) {
            val fromService = sanitizeId(span.serviceName)
            val toService = sanitizeId(child.serviceName)

            // Request
            sb.appendLine("$fromService -> $toService: ${child.method} ${child.uri}")
            sb.appendLine("activate $toService")

            // Process this child's children
            processSpan(sb, child, childrenMap, spanMap, depth + 1)

            // Response
            val statusColor = getStatusColor(child.status.code)
            sb.appendLine("$toService --> $fromService: $statusColor **${child.status.code}** ${child.status.description}")
            sb.appendLine("deactivate $toService")
            sb.appendLine("note right: ${child.duration.toMillis()}ms")
        }
    }

    /**
     * Generate a text-based summary of the trace
     */
    private fun generateTextSummary(trace: TraceData) {
        val spans = trace.getAllSpansOrdered()
        val fileName = "trace-${trace.traceId.take(8)}.txt"
        val file = File(outputDir, fileName)

        val sb = StringBuilder()
        sb.appendLine("=" .repeat(80))
        sb.appendLine("TRACE SUMMARY")
        sb.appendLine("=" .repeat(80))
        sb.appendLine()
        sb.appendLine("Trace ID: ${trace.traceId}")
        sb.appendLine("Total Spans: ${spans.size}")
        sb.appendLine("Total Duration: ${calculateTotalDuration(spans)}ms")
        sb.appendLine()
        sb.appendLine("Request Flow:")
        sb.appendLine("-" .repeat(80))

        // Build visual tree
        val rootSpan = trace.getRootSpan()
        if (rootSpan != null) {
            val childrenMap = spans.groupBy { it.parentSpanId }
            buildTextTree(sb, rootSpan, childrenMap, "", true)
        }

        sb.appendLine()
        sb.appendLine("Detailed Spans:")
        sb.appendLine("-" .repeat(80))

        spans.forEachIndexed { index, span ->
            sb.appendLine()
            sb.appendLine("Span ${index + 1}:")
            sb.appendLine("  Service: ${span.serviceName}")
            sb.appendLine("  Span ID: ${span.spanId}")
            sb.appendLine("  Parent: ${span.parentSpanId ?: "ROOT"}")
            sb.appendLine("  Request: ${span.method} ${span.uri}")
            sb.appendLine("  Status: ${span.status.code} ${span.status.description}")
            sb.appendLine("  Duration: ${span.duration.toMillis()}ms")
            sb.appendLine("  Start: ${timeFormatter.format(span.startTime)}")

            if (span.status.code >= 400) {
                sb.appendLine("  ⚠️ ERROR RESPONSE")
            }
        }

        sb.appendLine()
        sb.appendLine("=" .repeat(80))

        file.writeText(sb.toString())
    }

    /**
     * Build a text tree representation
     */
    private fun buildTextTree(
        sb: StringBuilder,
        span: SpanData,
        childrenMap: Map<String?, List<SpanData>>,
        prefix: String,
        isLast: Boolean
    ) {
        val statusIcon = if (span.status.code < 400) "✓" else "✗"
        val connector = if (prefix.isEmpty()) "" else if (isLast) "└─ " else "├─ "

        sb.append(prefix)
        sb.append(connector)
        sb.append("$statusIcon [${span.serviceName}] ${span.method} ${span.uri}")
        sb.append(" → ${span.status.code} (${span.duration.toMillis()}ms)")
        sb.appendLine()

        val children = childrenMap[span.spanId] ?: emptyList()
        children.forEachIndexed { index, child ->
            val isChildLast = index == children.size - 1
            val newPrefix = if (prefix.isEmpty()) {
                if (isChildLast) "   " else "│  "
            } else {
                prefix + if (isLast) "   " else "│  "
            }
            buildTextTree(sb, child, childrenMap, newPrefix, isChildLast)
        }
    }

    /**
     * Get color code for status
     */
    private fun getStatusColor(statusCode: Int): String {
        return when (statusCode) {
            in 200..299 -> "<color:green>"
            in 300..399 -> "<color:blue>"
            in 400..499 -> "<color:orange>"
            in 500..599 -> "<color:red>"
            else -> ""
        }
    }

    /**
     * Calculate total duration (from first to last span)
     */
    private fun calculateTotalDuration(spans: List<SpanData>): Long {
        if (spans.isEmpty()) return 0
        return spans.sumOf { it.duration.toMillis() }
    }

    /**
     * Sanitize service name for PlantUML ID
     */
    private fun sanitizeId(name: String): String {
        return name.replace("-", "_").replace(" ", "_")
    }

    /**
     * Generate diagram for a specific trace ID
     */
    fun generateDiagramForTrace(traceId: String): File? {
        val trace = TraceCollector.getTrace(traceId) ?: return null
        return generateDiagram(trace)
    }

    /**
     * Generate diagrams for all traces
     */
    fun generateAllDiagrams() {
        TraceCollector.getAllTraces().forEach { trace ->
            if (trace.completed) {
                generateDiagram(trace)
            }
        }
    }
}
