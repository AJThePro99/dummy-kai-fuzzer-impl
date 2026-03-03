package org.example.dataModels

enum class VerdictStatus {
    CORRECT,        // All backends/versions match expected behavior
    BUG_FOUND,      // A crash or differential mismatch was detected
    UNKNOWN         // Fuzzer error or unclassifiable output
}

data class Verdict(
    val result: SutResult,
    val status: VerdictStatus,
    val description: String? = null
)