# Kotlin Compiler Fuzzer [High level overview]

A dummy Kotlin compiler fuzzer demonstrating a pluggable architecture that supports future evolution.

## Proposed Architecture

The proposed architecture consists of 5 major independent modules, which when brought together, constitute the parts of
a working compiler fuzzer. The tester can swap any parts of the module for another implementation or their own custom
implementation.

The modules are:

1. The Input Generator
2. The System under test (SUT) handler
3. The Oracle
4. The Issue Manager
5. The Reducer

---

### Code Overview

Driver Code

```kotlin
suspend fun main(): Unit = coroutineScope {
    val myGenerator = DummyGenerator(seed = 100L)

    val mySutHandler = DummySutHandler(
        backends = mapOf(
            SutBackend.JVM to listOf("1.5.0", "1.9.20", "2.3.0"),
            SutBackend.JS to listOf("1.8.0", "2.3.0", "2.3.10")
        )
    )

    val myOracle = DummyOracle()

    val fuzzer: KaiFuzzer = DefaultKaiFuzzer(
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
```

The Input Generator

```kotlin
interface KaiInputGenerator {
    suspend fun generateFuzzInput(): FuzzInput
}

data class FuzzInput(
    val id: UUID = UUID.randomUUID(),
    val sourceCode: String,
    val generatorId: String,
    val seedUsed: Long? = null
)

class DummyGenerator(private val seed: Long?) : KaiInputGenerator {
    override suspend fun generateFuzzInput(): FuzzInput {
        return FuzzInput(
            sourceCode = "fun main() { println(\"Dummy Generator with seed $seed\") }",
            generatorId = "DummyGenerator",
            seedUsed = seed
        )
    }
}
```

System Under Test (SUT) handler

```kotlin
interface KaiSutHandler {
    suspend fun runCompilers(input: FuzzInput): SutResult
}

enum class SutBackend {
    JVM,
    NATIVE,
    JS,
    WASM
}

data class CompilerExecution(
    val backend: SutBackend,
    val version: String,
    val exitCode: Int,
    val standardOutput: String,
    val errorOutput: String?,
    val isCrash: Boolean
)

data class SutResult(
    val input: FuzzInput,
    // map each backend to a list of the outputs of each compiler version
    val executions: Map<SutBackend, List<CompilerExecution>>
)

class DummySutHandler(private val backends: Map<SutBackend, List<String>>) : KaiSutHandler {
    override suspend fun runCompilers(input: FuzzInput): SutResult {
        return SutResult(input, emptyMap())
    }
}
```

The Oracle (Validator)

```kotlin
interface KaiOracle {
    suspend fun evaluate(result: SutResult): Verdict
}

enum class VerdictStatus {
    CORRECT,        // All backends/versions match expected behavior
    BUG_FOUND,      // A crash or differential mismatch was detected
    UNKNOWN         // Fuzzer error or unclassifiable output
}

data class Verdict(
    val result: SutResult,
    val status: VerdictStatus,
    val description: String? = null
)

class DummyOracle : KaiOracle {
    override suspend fun evaluate(result: SutResult): Verdict {
        // Logic to compare outputs across different backends/versions
        return Verdict(result, VerdictStatus.CORRECT, "Outputs Match")
    }
}
```

The Issue Manager

```kotlin
interface KaiIssueManager {
    suspend fun processIssue(verdict: Verdict)
}

data class IssueManagerConfig(
    val saveDirectory: String = "./fuzzer_output",
    val saveAllGenerated: Boolean = false, // if true, this saves all generated programs, regardless of correctness
    val enableReduction: Boolean = true
)

class DummyIssueManager(
    private val config: IssueManagerConfig = IssueManagerConfig(),
) : KaiIssueManager {
    override suspend fun processIssue(verdict: Verdict) {
        if (verdict.status == VerdictStatus.BUG_FOUND || config.saveAllGenerated) {
            println("Saving issue ${verdict.result.input.id} to ${config.saveDirectory}")
        }
    }
}
```

The Reducer

```kotlin
interface KaiReducer {
    suspend fun reduce(
        faultyVerdict: Verdict,
        sutHandler: KaiSutHandler,
        oracle: KaiOracle
    ): String
}

class DummyReducer : KaiReducer {
    override suspend fun reduce(faultyVerdict: Verdict, sutHandler: KaiSutHandler, oracle: KaiOracle): String {
        // run reducing operations, and then return the reduced code.
        return faultyVerdict.result.input.sourceCode
    }
}
```

The Default `KaiFuzzer()` class to bring it all together

```kotlin
class DefaultKaiFuzzer(
    private val inputGenerator: KaiInputGenerator,
    private val sutHandler: KaiSutHandler,
    private val oracle: KaiOracle,
    private val issueManager: KaiIssueManager,
    private val reducer: KaiReducer? = null
) : KaiFuzzer {
    private var isRunning = false

    override suspend fun run() {
        isRunning = true

        while (isRunning) {
            val input = inputGenerator.generateFuzzInput()
            val sutResult = sutHandler.runCompilers(input)
            val verdict = oracle.evaluate(sutResult)

            if (verdict.status == VerdictStatus.BUG_FOUND) {
                // if reducer is attached, the code will be reduced here
                issueManager.processIssue(verdict)
            }
        }

    }

    override fun stop() {
        isRunning = false
    }
}
```
A detailed gist explaining all the design choices and working of the components and interaction with each other will be present [here]() (`in progress`)