package org.example.dataModels

import java.util.UUID

// All data models used for the Input Generator module exist here

data class FuzzInput(
    val id: UUID = UUID.randomUUID(),
    val sourceCode: String,
    val generatorId: String,
    val seedUsed: Long? = null
)