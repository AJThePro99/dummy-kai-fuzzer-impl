package org.example

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.example.baseInterfaces.KaiFuzzer
import org.example.dataModels.SutBackend
import org.example.dummyImplementations.*

suspend fun main(): Unit = coroutineScope {
    val myGenerator = DummyGenerator(seed = 100L)

    val mySutHandler = DummySutHandler(
        backends = mapOf(
            SutBackend.JVM to listOf("1.5.0", "1.9.20", "2.3.0"),
            SutBackend.JS to listOf("1.8.0", "2.3.0", "2.3.10")
        )
    )

    val myOracle = DummyOracle()

    val fuzzer : KaiFuzzer = DefaultKaiFuzzer(
        inputGenerator = myGenerator,
        sutHandler = mySutHandler,
        oracle = myOracle,
        issueManager = DummyIssueManager(),
        reducer = null // reducer is optional
    )

    println("===Initiating Kai Fuzzer===")

    val fuzzJob = launch {
        fuzzer.run()
    }

    delay(10_000) // run for 10_000 ms
    fuzzer.stop()
    fuzzJob.join()

    println("Fuzzing is complete")
}