package org.example.dummyImplementations

import org.example.baseInterfaces.KaiFuzzer
import org.example.baseInterfaces.KaiInputGenerator
import org.example.baseInterfaces.KaiIssueManager
import org.example.baseInterfaces.KaiOracle
import org.example.baseInterfaces.KaiReducer
import org.example.baseInterfaces.KaiSutHandler
import org.example.dataModels.VerdictStatus

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