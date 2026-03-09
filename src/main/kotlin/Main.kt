package org.example

import kotlinx.coroutines.coroutineScope
import org.example.baseInterfaces.KaiFuzzer
import org.example.dataModels.SutBackend
import org.example.dummyImplementations.*

suspend fun main(): Unit = coroutineScope {
    val myGenerator = DummyGenerator(seed = 100L)

    val mySutHandler = DummySutHandler(
        backends = mapOf(
            SutBackend.JVM to listOf("1.5.0", "1.9.20", "2.3.0"),
            SutBackend.JS to listOf("1.8.0", "2.3.0")
        )
    )

    val myOracle = DummyOracle()

    val fuzzer : KaiFuzzer = DefaultKaiFuzzer(
        inputGenerator = myGenerator,
        sutHandler = mySutHandler,
        oracle = myOracle,
        issueManager = DummyIssueManager(),
        reducer = DummyReducer(),
    )

    println("===Initiating Kai Fuzzer===")
    fuzzer.run(programs = 10_000_000, jobs = 8)
    println("===Fuzzing Complete===")

    // need graceful shutdown, for safe exiting of progress for with Ctrl + C
}