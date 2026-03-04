package org.example.dataModels

data class IssueManagerConfig(
    val saveDirectory: String = "./fuzzer_output",
    val saveAllGenerated: Boolean = false, // if true, this saves all generated programs, regardless of correctness
)