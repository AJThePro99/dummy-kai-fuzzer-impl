package org.example.dummyImplementations

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.example.baseInterfaces.*
import org.example.dataModels.FuzzInput
import org.example.dataModels.SutResult
import org.example.dataModels.Verdict
import org.example.dataModels.VerdictStatus

class DefaultKaiFuzzer(
    private val inputGenerator: KaiInputGenerator,
    private val sutHandler: KaiSutHandler,
    private val oracle: KaiOracle,
    private val issueManager: KaiIssueManager,
    private val reducer: KaiReducer? = null
) : KaiFuzzer {
    override suspend fun run(programs: Long, jobs: Int): Unit = supervisorScope {
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