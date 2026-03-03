package org.example.dummyImplementations

import org.example.baseInterfaces.KaiOracle
import org.example.baseInterfaces.KaiReducer
import org.example.baseInterfaces.KaiSutHandler
import org.example.dataModels.Verdict

class DummyReducer : KaiReducer {
    override suspend fun reduce(faultyVerdict: Verdict, sutHandler: KaiSutHandler, oracle: KaiOracle): String {
        // run reducing operations, and then return the reduced code.
        return faultyVerdict.result.input.sourceCode
    }
}