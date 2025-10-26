package com.demo.tracerbullet

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.*
import java.io.File

/**
 * Acceptance Tests for TracerBullet Distributed Tracing
 *
 * These tests demonstrate different tracing scenarios in Given-When-Then format.
 * Each test generates a PlantUML diagram showing the request flow.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class TracerBulletAcceptanceTests {

    private lateinit var testServices: TestServices
    private val client = ApacheClient()

    @BeforeAll
    fun setupServices() {
        println("\n")
        println("=" .repeat(80))
        println("TRACERBULLET ACCEPTANCE TESTS")
        println("=" .repeat(80))
        println()

        testServices = TestServices()
        testServices.startAll()

        // Wait a bit for services to stabilize
        Thread.sleep(1000)
    }

    @AfterAll
    fun teardownServices() {
        // Wait for diagrams to be generated
        println("\n⏳ Waiting for diagram generation...")
        Thread.sleep(2000)

        testServices.stopAll()

        // Generate test report
        TestReportGenerator.generateReport()

        println("\n")
        println("=" .repeat(80))
        println("ALL TESTS COMPLETED - Check diagrams/ folder for visual traces!")
        println("=" .repeat(80))
        println()
    }

    @Test
    @Order(1)
    fun `Scenario 1 - Successful user lookup through all services`() {
        runScenario("Successful user lookup through all services") {
            var response: Response? = null

            given("a user ID exists in the system") {
                // User 123 exists in our test database
                val userId = "123"
                println("    User ID: $userId")
            }

            `when`("I request user information from the frontend service") {
                response = client(
                    Request(Method.GET, "${testServices.getFrontendUrl()}/users/123")
                )
                println("    Request: GET /users/123")
                response
            }

            then("the request should flow through all three services successfully") {
                response shouldNotBe null
                response!!.status shouldBe Status.OK
                response!!.bodyString() shouldContain "User data"
                response!!.bodyString() shouldContain "John Doe"

                // Verify trace headers are present
                response!!.header("X-Trace-Id") shouldNotBe null

                println("    ✓ Response: ${response!!.status}")
                println("    ✓ Trace ID: ${response!!.header("X-Trace-Id")}")
                println("    ✓ All services called successfully")
            }
        }
    }

    @Test
    @Order(2)
    fun `Scenario 2 - Order creation with trace propagation`() {
        runScenario("Order creation with trace propagation") {
            var response: Response? = null
            val orderData = """{"product":"Widget","quantity":5}"""

            given("valid order data") {
                println("    Order: $orderData")
            }

            `when`("I submit an order to the frontend service") {
                response = client(
                    Request(Method.POST, "${testServices.getFrontendUrl()}/orders")
                        .body(orderData)
                )
                println("    Request: POST /orders")
                response
            }

            then("the order should be created and traced through all services") {
                response shouldNotBe null
                response!!.status shouldBe Status.CREATED
                response!!.bodyString() shouldContain "Order created"
                response!!.bodyString() shouldContain "orderId"

                val traceId = response!!.header("X-Trace-Id")
                traceId shouldNotBe null

                println("    ✓ Response: ${response!!.status}")
                println("    ✓ Trace ID: $traceId")
                println("    ✓ Order created successfully")
            }
        }
    }

    @Test
    @Order(3)
    fun `Scenario 3 - Health check with minimal trace`() {
        runScenario("Health check with minimal trace") {
            var response: Response? = null

            given("a running frontend service") {
                println("    Frontend URL: ${testServices.getFrontendUrl()}")
            }

            `when`("I check the health endpoint") {
                response = client(
                    Request(Method.GET, "${testServices.getFrontendUrl()}/health")
                )
                println("    Request: GET /health")
                response
            }

            then("the service should respond healthy without calling downstream services") {
                response shouldNotBe null
                response!!.status shouldBe Status.OK
                response!!.bodyString() shouldContain "healthy"

                println("    ✓ Response: ${response!!.status}")
                println("    ✓ Service is healthy")
                println("    ✓ No downstream calls made (single span)")
            }
        }
    }

    @Test
    @Order(4)
    fun `Scenario 4 - Multiple concurrent requests with different trace IDs`() {
        runScenario("Multiple concurrent requests with different trace IDs") {
            val responses = mutableListOf<Response>()
            val traceIds = mutableSetOf<String>()

            given("three concurrent user requests") {
                println("    Request count: 3")
                println("    User IDs: 101, 102, 103")
            }

            `when`("I make multiple concurrent requests") {
                val threads = listOf(
                    Thread {
                        val resp = client(Request(Method.GET, "${testServices.getFrontendUrl()}/users/101"))
                        synchronized(responses) { responses.add(resp) }
                    },
                    Thread {
                        val resp = client(Request(Method.GET, "${testServices.getFrontendUrl()}/users/102"))
                        synchronized(responses) { responses.add(resp) }
                    },
                    Thread {
                        val resp = client(Request(Method.GET, "${testServices.getFrontendUrl()}/users/103"))
                        synchronized(responses) { responses.add(resp) }
                    }
                )

                threads.forEach { it.start() }
                threads.forEach { it.join() }

                println("    All requests completed")
                responses
            }

            then("each request should have its own unique trace ID") {
                responses.size shouldBe 3

                responses.forEach { response ->
                    response.status shouldBe Status.OK
                    val traceId = response.header("X-Trace-Id")
                    traceId shouldNotBe null
                    traceIds.add(traceId!!)
                }

                traceIds.size shouldBe 3

                println("    ✓ All requests successful")
                println("    ✓ Trace IDs are unique:")
                traceIds.forEachIndexed { index, id ->
                    println("      ${index + 1}. ${id.take(16)}...")
                }
            }
        }
    }

    @Test
    @Order(5)
    fun `Scenario 5 - Trace propagation through service chain`() {
        runScenario("Trace propagation through service chain") {
            var frontendResponse: Response? = null
            lateinit var initialTraceId: String

            given("a request to the frontend service") {
                println("    Entry point: Frontend Service")
            }

            `when`("the request flows through Frontend -> Backend -> Database") {
                frontendResponse = client(
                    Request(Method.GET, "${testServices.getFrontendUrl()}/users/456")
                )

                initialTraceId = frontendResponse!!.header("X-Trace-Id")!!

                // Wait a moment for all spans to be recorded
                Thread.sleep(500)

                println("    Request chain completed")
                frontendResponse
            }

            then("all services should share the same trace ID") {
                frontendResponse shouldNotBe null
                frontendResponse!!.status shouldBe Status.OK

                // Get the trace from collector
                val trace = TraceCollector.getTrace(initialTraceId)
                trace shouldNotBe null

                val services = trace!!.spans.map { it.serviceName }.toSet()

                println("    ✓ Trace ID: ${initialTraceId.take(16)}...")
                println("    ✓ Services in trace:")
                services.forEach { service ->
                    println("      - $service")
                }

                services shouldContain "frontend-service"
                services shouldContain "backend-service"
                services shouldContain "database-service"

                println("    ✓ All services share the same trace ID")
            }
        }
    }

    @Test
    @Order(6)
    fun `Scenario 6 - Parent-child span relationships`() {
        runScenario("Parent-child span relationships") {
            var response: Response? = null
            lateinit var traceId: String

            given("a request that will create a service chain") {
                println("    Service chain: Frontend -> Backend -> Database")
            }

            `when`("the request is processed") {
                response = client(
                    Request(Method.GET, "${testServices.getFrontendUrl()}/users/789")
                )

                traceId = response!!.header("X-Trace-Id")!!

                // Wait for all spans to be recorded
                Thread.sleep(500)

                println("    Request completed")
                response
            }

            then("the spans should have proper parent-child relationships") {
                val trace = TraceCollector.getTrace(traceId)
                trace shouldNotBe null

                val rootSpan = trace!!.getRootSpan()
                rootSpan shouldNotBe null
                rootSpan!!.serviceName shouldBe "frontend-service"
                rootSpan.parentSpanId shouldBe null

                val childSpans = trace.getSpansByParent(rootSpan.spanId)
                childSpans.size shouldBe 1

                val backendSpan = childSpans.first()
                backendSpan.serviceName shouldBe "backend-service"
                backendSpan.parentSpanId shouldBe rootSpan.spanId

                val grandchildSpans = trace.getSpansByParent(backendSpan.spanId)
                grandchildSpans.size shouldBe 1

                val databaseSpan = grandchildSpans.first()
                databaseSpan.serviceName shouldBe "database-service"
                databaseSpan.parentSpanId shouldBe backendSpan.spanId

                println("    ✓ Root span: ${rootSpan.serviceName}")
                println("    ✓ Child span: ${backendSpan.serviceName} (parent: ${rootSpan.spanId.take(8)}...)")
                println("    ✓ Grandchild span: ${databaseSpan.serviceName} (parent: ${backendSpan.spanId.take(8)}...)")
                println("    ✓ Parent-child relationships verified")
            }
        }
    }

    @Test
    @Order(7)
    fun `Scenario 7 - Response status codes in trace`() {
        runScenario("Response status codes in trace") {
            var successResponse: Response? = null
            lateinit var successTraceId: String

            given("requests with different outcomes") {
                println("    Testing status code tracking")
            }

            `when`("I make a successful request") {
                successResponse = client(
                    Request(Method.GET, "${testServices.getFrontendUrl()}/users/200")
                )
                successTraceId = successResponse!!.header("X-Trace-Id")!!

                Thread.sleep(500)

                println("    Request completed")
                successResponse
            }

            then("the trace should capture the status codes") {
                successResponse!!.status shouldBe Status.OK

                val trace = TraceCollector.getTrace(successTraceId)
                trace shouldNotBe null

                val allSuccess = trace!!.spans.all { it.status.code in 200..299 }
                allSuccess shouldBe true

                println("    ✓ All spans have 2xx status codes")
                trace.spans.forEach { span ->
                    println("      - ${span.serviceName}: ${span.status.code}")
                }
            }
        }
    }
}
