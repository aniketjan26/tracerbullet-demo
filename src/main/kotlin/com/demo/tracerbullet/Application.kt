package com.demo.tracerbullet

import org.http4k.client.ApacheClient
import org.http4k.core.*
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer

/**
 * Main application demonstrating TracerBullet distributed tracing.
 *
 * This demo sets up three microservices:
 * - Frontend Service (port 8080): Receives user requests
 * - Backend Service (port 8081): Processes business logic
 * - Database Service (port 8082): Simulates database operations
 *
 * A request flows: Frontend -> Backend -> Database
 * TracerBullet tracks the entire flow with a single trace ID.
 */
fun main() {
    println("""
        |
        |==================================================
        |  TracerBullet Demo - http4k Distributed Tracing
        |==================================================
        |
        |Starting 3 microservices:
        |  - Frontend Service: http://localhost:8080
        |  - Backend Service:  http://localhost:8081
        |  - Database Service: http://localhost:8082
        |
        |Try these endpoints:
        |  GET  http://localhost:8080/users/123
        |  POST http://localhost:8080/orders
        |  GET  http://localhost:8080/health
        |
        |Watch the console to see TracerBullet in action!
        |==================================================
        |
    """.trimMargin())

    // Start all services
    DatabaseService().start()
    BackendService().start()
    FrontendService().start()

    println("\nAll services are running! Press Ctrl+C to stop.\n")

    // Keep the application running
    Thread.currentThread().join()
}

/**
 * Frontend Service - Entry point for user requests
 */
class FrontendService {
    private val client = ApacheClient()

    fun createTestApp(backendPort: Int = 8081): HttpHandler = routes(
        "/users/{id}" bind Method.GET to { request ->
            val userId = request.path("id")!!
            println("\n[FRONTEND] Received request for user: $userId")

            // Call backend service with trace propagation
            val backendClient = TracerBullet.clientFilter("backend-service")
                .then(client)

            val backendResponse = backendClient(
                Request(Method.GET, "http://localhost:$backendPort/api/users/$userId")
            )

            Response(Status.OK)
                .body("User data: ${backendResponse.bodyString()}")
        },

        "/orders" bind Method.POST to { request ->
            println("\n[FRONTEND] Received order creation request")

            val backendClient = TracerBullet.clientFilter("backend-service")
                .then(client)

            val backendResponse = backendClient(
                Request(Method.POST, "http://localhost:$backendPort/api/orders")
                    .body(request.bodyString())
            )

            Response(Status.CREATED)
                .body("Order created: ${backendResponse.bodyString()}")
        },

        "/health" bind Method.GET to {
            Response(Status.OK).body("Frontend service is healthy!")
        }
    )

    private val app: HttpHandler = createTestApp()

    fun start() {
        val server = TracerBullet.serverFilter("frontend-service")
            .then(app)
            .asServer(Netty(8080))
            .start()

        println("✓ Frontend Service started on port 8080")
    }
}

/**
 * Backend Service - Business logic layer
 */
class BackendService {
    private val client = ApacheClient()

    fun createTestApp(databasePort: Int = 8082): HttpHandler = routes(
        "/api/users/{id}" bind Method.GET to { request ->
            val userId = request.path("id")!!
            println("[BACKEND] Processing user lookup: $userId")

            // Call database service
            val dbClient = TracerBullet.clientFilter("database-service")
                .then(client)

            val dbResponse = dbClient(
                Request(Method.GET, "http://localhost:$databasePort/db/users/$userId")
            )

            Response(Status.OK)
                .body("""{"userId":"$userId", "name":"${dbResponse.bodyString()}"}""")
        },

        "/api/orders" bind Method.POST to { request ->
            println("[BACKEND] Processing order creation")

            // Call database service
            val dbClient = TracerBullet.clientFilter("database-service")
                .then(client)

            val dbResponse = dbClient(
                Request(Method.POST, "http://localhost:$databasePort/db/orders")
                    .body(request.bodyString())
            )

            Response(Status.CREATED)
                .body("""{"orderId":"${dbResponse.bodyString()}", "status":"created"}""")
        }
    )

    private val app: HttpHandler = createTestApp()

    fun start() {
        val server = TracerBullet.serverFilter("backend-service")
            .then(app)
            .asServer(Netty(8081))
            .start()

        println("✓ Backend Service started on port 8081")
    }
}

/**
 * Database Service - Data persistence layer
 */
class DatabaseService {
    fun createTestApp(): HttpHandler = routes(
        "/db/users/{id}" bind Method.GET to { request ->
            val userId = request.path("id")!!
            println("[DATABASE] Querying user: $userId")

            // Simulate database query
            Thread.sleep(50)

            Response(Status.OK)
                .body("John Doe")
        },

        "/db/orders" bind Method.POST to { request ->
            println("[DATABASE] Storing order")

            // Simulate database write
            Thread.sleep(100)

            Response(Status.CREATED)
                .body("ORD-${System.currentTimeMillis()}")
        },

        "/db/users/404" bind Method.GET to {
            println("[DATABASE] User not found")
            Response(Status.NOT_FOUND)
                .body("User not found")
        }
    )

    private val app: HttpHandler = createTestApp()

    fun start() {
        val server = TracerBullet.serverFilter("database-service")
            .then(app)
            .asServer(Netty(8082))
            .start()

        println("✓ Database Service started on port 8082")
    }
}
