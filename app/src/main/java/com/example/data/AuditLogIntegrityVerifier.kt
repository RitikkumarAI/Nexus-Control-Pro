package com.example.data

import com.example.model.AuditLogEntry
import java.security.MessageDigest

data class AuditChainVerificationResult(
    val isChainIntact: Boolean,
    val totalEntries: Int,
    val verifiedEntries: Int,
    val tamperedEntryIds: Set<String> = emptySet(),
    val headDigest: String = "GENESIS"
)

/**
 * Cryptographic SHA-256 hash-chaining utility that seals and verifies
 * tamper-evident audit logs for administrative accountability.
 */
object AuditLogIntegrityVerifier {

    fun computeEntryHash(entry: AuditLogEntry, previousHash: String = entry.previousHash): String {
        val canonicalPayload = buildString {
            append(entry.id).append('|')
            append(entry.timestamp).append('|')
            append(entry.action).append('|')
            append(entry.initiatorRole).append('|')
            append(entry.adminUser).append('|')
            append(entry.targetDeviceId).append('|')
            append(entry.details).append('|')
            append(entry.outcome).append('|')
            append(previousHash)
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonicalPayload.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Links an ordered list of entries (oldest to newest or newest to oldest) into
     * a verified chronological SHA-256 chain. Returns entries sorted newest-first.
     */
    fun sealChronologicalChain(entries: List<AuditLogEntry>): List<AuditLogEntry> {
        if (entries.isEmpty()) return emptyList()
        val sortedOldestFirst = entries.sortedBy { it.timestamp }
        val sealed = mutableListOf<AuditLogEntry>()
        var runningPreviousHash = "GENESIS"

        for (entry in sortedOldestFirst) {
            val hash = computeEntryHash(entry, runningPreviousHash)
            val sealedEntry = entry.copy(
                previousHash = runningPreviousHash,
                entryHash = hash
            )
            sealed.add(sealedEntry)
            runningPreviousHash = hash
        }
        return sealed.reversed()
    }

    /**
     * Independently verifies every entry in the audit history against its
     * predecessor's hash and recomputed SHA-256 digest.
     */
    fun verifyChain(entries: List<AuditLogEntry>): AuditChainVerificationResult {
        if (entries.isEmpty()) {
            return AuditChainVerificationResult(
                isChainIntact = true,
                totalEntries = 0,
                verifiedEntries = 0,
                tamperedEntryIds = emptySet(),
                headDigest = "GENESIS"
            )
        }

        val oldestFirst = entries.sortedBy { it.timestamp }
        val tamperedIds = mutableSetOf<String>()
        var verifiedCount = 0
        var expectedPrev = "GENESIS"
        var lastHash = "GENESIS"

        for (entry in oldestFirst) {
            val expectedHash = computeEntryHash(entry, expectedPrev)
            val prevMatches = entry.previousHash == expectedPrev
            val hashMatches = entry.entryHash == expectedHash

            if (prevMatches && hashMatches) {
                verifiedCount++
            } else {
                tamperedIds.add(entry.id)
            }
            expectedPrev = entry.entryHash.ifBlank { expectedHash }
            lastHash = expectedPrev
        }

        return AuditChainVerificationResult(
            isChainIntact = tamperedIds.isEmpty(),
            totalEntries = oldestFirst.size,
            verifiedEntries = verifiedCount,
            tamperedEntryIds = tamperedIds,
            headDigest = lastHash
        )
    }
}
