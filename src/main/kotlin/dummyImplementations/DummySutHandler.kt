package org.example.dummyImplementations

import org.example.baseInterfaces.KaiSutHandler
import org.example.dataModels.CompilerExecution
import org.example.dataModels.FuzzInput
import org.example.dataModels.SutBackend
import org.example.dataModels.SutResult

class DummySutHandler(private val backends: Map<SutBackend, List<String>>) : KaiSutHandler {
    override suspend fun runCompilers(input: FuzzInput): SutResult {
        return SutResult(input, emptyMap())
    }
}