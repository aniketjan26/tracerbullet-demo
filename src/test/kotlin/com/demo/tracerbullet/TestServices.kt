package com.demo.tracerbullet

import org.http4k.core.then
import org.http4k.server.Http4kServer
import org.http4k.server.Netty
import org.http4k.server.asServer

/**
 * Test fixture that manages service lifecycle for testing
 */
class TestServices {
    private val servers = mutableListOf<Http4kServer>()
    private val testPorts = TestPorts()

    private lateinit var frontendService: FrontendService
    private lateinit var backendService: BackendService
    private lateinit var databaseService: DatabaseService

    fun startAll() {
        println("\n🚀 Starting test services...")

        // Clear any existing traces
        TraceCollector.clear()

        // Start database service first
        databaseService = DatabaseService()
        val dbApp = TracerBullet.serverFilter("database-service")
            .then(databaseService.createTestApp())
        servers.add(dbApp.asServer(Netty(testPorts.database)).start())
        println("  ✓ Database Service: http://localhost:${testPorts.database}")

        // Start backend service
        backendService = BackendService()
        val backendApp = TracerBullet.serverFilter("backend-service")
            .then(backendService.createTestApp(testPorts.database))
        servers.add(backendApp.asServer(Netty(testPorts.backend)).start())
        println("  ✓ Backend Service: http://localhost:${testPorts.backend}")

        // Start frontend service
        frontendService = FrontendService()
        val frontendApp = TracerBullet.serverFilter("frontend-service")
            .then(frontendService.createTestApp(testPorts.backend))
        servers.add(frontendApp.asServer(Netty(testPorts.frontend)).start())
        println("  ✓ Frontend Service: http://localhost:${testPorts.frontend}")

        // Give services time to start
        Thread.sleep(500)
        println("✓ All test services started\n")
    }

    fun stopAll() {
        println("\n🛑 Stopping test services...")
        servers.forEach { it.stop() }
        servers.clear()
        println("✓ All test services stopped\n")
    }

    fun getFrontendUrl() = "http://localhost:${testPorts.frontend}"
    fun getBackendUrl() = "http://localhost:${testPorts.backend}"
    fun getDatabaseUrl() = "http://localhost:${testPorts.database}"
}

/**
 * Test port configuration (different from production ports)
 */
data class TestPorts(
    val frontend: Int = 9080,
    val backend: Int = 9081,
    val database: Int = 9082
)
