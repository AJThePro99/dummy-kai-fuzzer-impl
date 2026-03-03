package org.example.dummyImplementations

import org.example.baseInterfaces.KaiOracle
import org.example.dataModels.SutResult
import org.example.dataModels.Verdict
import org.example.dataModels.VerdictStatus

class DummyOracle : KaiOracle {
    override suspend fun evaluate(result: SutResult) : Verdict{
        // Logic to compare outputs across different backends/versions
        return Verdict(result, VerdictStatus.CORRECT, "Outputs Match")
    }
}