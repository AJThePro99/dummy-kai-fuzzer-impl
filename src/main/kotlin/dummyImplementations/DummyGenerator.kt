package org.example.dummyImplementations

import org.example.baseInterfaces.KaiInputGenerator
import org.example.dataModels.FuzzInput

class DummyGenerator(private val seed: Long?) : KaiInputGenerator {
    override suspend fun generateFuzzInput(): FuzzInput {
        return FuzzInput(
            sourceCode = "fun main() { println(\"Dummy Generator with seed $seed\") }",
            generatorId = "DummyGenerator",
            seedUsed = seed
        )
    }
}