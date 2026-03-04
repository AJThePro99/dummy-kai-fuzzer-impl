package org.example.baseInterfaces

import org.example.dataModels.FuzzInput
import org.example.dataModels.SutResult
import org.example.dataModels.Verdict

interface KaiFuzzer {
    suspend fun run()
}

interface KaiInputGenerator {
    suspend fun generateFuzzInput(): FuzzInput
}

interface KaiSutHandler {
    suspend fun runCompilers(input: FuzzInput) : SutResult
}

interface KaiOracle {
    suspend fun evaluate(result: SutResult) : Verdict
}

interface KaiIssueManager {
    suspend fun processIssue(verdict: Verdict)
}

interface KaiReducer {
    suspend fun reduce(
        input: FuzzInput
    ) : FuzzInput
}