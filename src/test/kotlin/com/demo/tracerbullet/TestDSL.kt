package com.demo.tracerbullet

/**
 * BDD-style test DSL for Given-When-Then acceptance tests
 */
class Scenario(val name: String) {
    private var givenBlock: (() -> Unit)? = null
    private var whenBlock: (() -> Any?)? = null
    private var thenBlock: ((Any?) -> Unit)? = null

    fun given(description: String, block: () -> Unit): Scenario {
        println("  GIVEN $description")
        givenBlock = block
        return this
    }

    fun `when`(description: String, block: () -> Any?): Scenario {
        println("  WHEN $description")
        whenBlock = block
        return this
    }

    fun then(description: String, block: (Any?) -> Unit): Scenario {
        println("  THEN $description")
        thenBlock = block
        return this
    }

    fun execute() {
        println("\n📝 SCENARIO: $name")
        println("=" .repeat(80))

        givenBlock?.invoke()
        val result = whenBlock?.invoke()
        thenBlock?.invoke(result)

        println("✓ Scenario passed")
        println("=" .repeat(80))
    }
}

/**
 * Create a new scenario
 */
fun scenario(name: String, block: Scenario.() -> Unit): Scenario {
    val scenario = Scenario(name)
    scenario.block()
    return scenario
}

/**
 * Execute a scenario
 */
fun runScenario(name: String, block: Scenario.() -> Unit) {
    val scenario = scenario(name, block)
    scenario.execute()
}
