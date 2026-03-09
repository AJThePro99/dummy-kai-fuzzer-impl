# Kotlin Compiler Fuzzer [Proof of Concept]

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

## Code Overview

### Driver Code

```kotlin
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
        reducer = DummyReducer()            // reducer is optional but recommended
    )

    println("===Initiating Kai Fuzzer===")
    fuzzer.run(programs = 10_000_000, jobs = 8)
    println("===Fuzzing Complete===")
}
```

### The Input Generator

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

### System Under Test (SUT) handler

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
    val output: CompilerExecutionOutput
)

data class CompilerExecutionOutput(
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

// helper function to compare the outputs between reduced & non-reduced version
fun Map<SutBackend, List<CompilerExecution>>.toOutput() : Map<SutBackend, List<CompilerExecutionOutput>> {
    return this.mapValues { (_, compilerExecutions) -> compilerExecutions.map { it.output } }
}
```

### The Oracle (Validator)

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

### The Issue Manager

```kotlin
interface KaiIssueManager {
    suspend fun processIssue(verdict: Verdict)
}

data class IssueManagerConfig(
    val saveDirectory: String = "./fuzzer_output",
    val saveAllGenerated: Boolean = false, // if true, this saves all generated programs, regardless of correctness
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

### The Reducer

```kotlin
interface KaiReducer {
    suspend fun reduce(
        input: FuzzInput
    ) : FuzzInput
}

class DummyReducer : KaiReducer {
    override suspend fun reduce(input: FuzzInput): FuzzInput {
        // run reducing operations, and then return the reduced code.
        val reducedSourceCode = input.sourceCode

        return FuzzInput(
            id = input.id,
            sourceCode = reducedSourceCode,
            generatorId = "Reducer",
            seedUsed = input.seedUsed
        )
    }
}
```

### The `DefaultKaiFuzzer()` class to bring it all together

```kotlin
class DefaultKaiFuzzer(
    private val inputGenerator: KaiInputGenerator,
    private val sutHandler: KaiSutHandler,
    private val oracle: KaiOracle,
    private val issueManager: KaiIssueManager,
    private val reducer: KaiReducer? = null
) : KaiFuzzer {
    override suspend fun run(programs: Long, jobs: Int): Unit = coroutineScope{
        val inputChannel = Channel<FuzzInput>(capacity = 64)
        val resultChannel = Channel<SutResult>(capacity = 64)
        val reductionChannel = Channel<Verdict>(capacity = 64)

        launch {
            for (i in 1..programs) {
                val input = inputGenerator.generateFuzzInput()
                inputChannel.send(input)
            }
            inputChannel.close() // closing channel once all programs are generated
        }

        val sutWorkers = (1..jobs).map {
            launch {
                for (input in inputChannel) {
                    val result = sutHandler.runCompilers(input)
                    resultChannel.send(result)
                }
            }
        }

        val reducerWorkers = (1..jobs).map {
            launch {
                for (verdict in reductionChannel) {
                    // comparing reduced input & original input's exit codes, stack traces, error messages.
                    val reducedInput = reducer?.reduce(verdict.result.input) ?: verdict.result.input

                    val reducedResult = SutResult(
                        input = reducedInput,
                        executions = verdict.result.executions
                    )

                    val reducedVerdict = Verdict(
                        result = reducedResult,
                        status = verdict.status,
                        description = verdict.description,
                    )

                    issueManager.processIssue(reducedVerdict)
                }
            }
        }

        launch {
            sutWorkers.joinAll()
            resultChannel.close() // closing channel once all programs are compiled
        }

        launch {
            for (result in resultChannel) {
                val verdict = oracle.evaluate(result)

                if (reducer != null && verdict.status == VerdictStatus.BUG_FOUND) {
                    reductionChannel.send(verdict)
                } else {
                    issueManager.processIssue(verdict)
                }
            }
            reductionChannel.close() // Close reduction channel once all results are evaluated
        }
    }
}
```
A detailed gist explaining all the design choices and working of the components and interaction with each other will be present [here]() (`in progress`)