package org.example.dummyImplementations

import org.example.baseInterfaces.KaiReducer
import org.example.dataModels.FuzzInput

class DummyReducer : KaiReducer {
    override suspend fun reduce(input: FuzzInput): FuzzInput {
        // run reducing operations, and then return the reduced code.
        val reducedSourceCode = input.sourceCode;

        return FuzzInput(
            id = input.id,
            sourceCode = reducedSourceCode,
            generatorId = "Reducer",
            seedUsed = input.seedUsed
        )
    }
}