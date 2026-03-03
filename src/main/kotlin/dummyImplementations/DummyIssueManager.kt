package org.example.dummyImplementations

import org.example.baseInterfaces.KaiIssueManager
import org.example.dataModels.IssueManagerConfig
import org.example.dataModels.Verdict
import org.example.dataModels.VerdictStatus

class DummyIssueManager(
    private val config: IssueManagerConfig = IssueManagerConfig(),
) : KaiIssueManager {
    override suspend fun processIssue(verdict: Verdict) {
        if (verdict.status == VerdictStatus.BUG_FOUND || config.saveAllGenerated) {
            println("Saving issue ${verdict.result.input.id} to ${config.saveDirectory}")
        }
    }
}