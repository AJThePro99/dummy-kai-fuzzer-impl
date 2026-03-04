package org.example.dataModels

// All data models used for the System under test (SUT) Handler module exist here

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

fun Map<SutBackend, List<CompilerExecution>>.toOutput() : Map<SutBackend, List<CompilerExecutionOutput>> {
    return this.mapValues { (_, compilerExecutions) -> compilerExecutions.map { it.output } }
}