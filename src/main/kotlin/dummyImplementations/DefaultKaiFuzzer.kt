package org.example.dummyImplementations

import kotlinx.coroutines.delay
import org.example.baseInterfaces.KaiFuzzer
import org.example.baseInterfaces.KaiInputGenerator
import org.example.baseInterfaces.KaiIssueManager
import org.example.baseInterfaces.KaiOracle
import org.example.baseInterfaces.KaiReducer
import org.example.baseInterfaces.KaiSutHandler
import org.example.dataModels.FuzzInput
import org.example.dataModels.SutResult
import org.example.dataModels.Verdict
import org.example.dataModels.VerdictStatus
import org.example.dataModels.toOutput
import kotlin.time.Duration.Companion.seconds

class DefaultKaiFuzzer(
    private val inputGenerator: KaiInputGenerator,
    private val sutHandler: KaiSutHandler,
    private val oracle: KaiOracle,
    private val issueManager: KaiIssueManager,
    private val reducer: KaiReducer? = null
) : KaiFuzzer {
    override suspend fun run() {
        val input = inputGenerator.generateFuzzInput()
        val sutResult = sutHandler.runCompilers(input)
        var verdict = oracle.evaluate(sutResult)

        if (reducer != null && verdict.status != VerdictStatus.CORRECT) { // reduces only anomaly inducing programs
            val rxInput: FuzzInput = reducer.reduce(verdict.result.input)
            val rxSutResult: SutResult = sutHandler.runCompilers(rxInput)
            val rxVerdict: Verdict = oracle.evaluate(rxSutResult)

            if (rxVerdict.result.executions.toOutput() == verdict.result.executions.toOutput()) {
                verdict = rxVerdict
            } else {
                throw RuntimeException("Error in reduction algorithm. Outputs of programs do not match.")
            }
        }
        issueManager.processIssue(verdict)
    }
}