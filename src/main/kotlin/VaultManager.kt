package utils

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import models.ProcessedFile
import java.io.File

private val json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

class VaultManager {
    private val vaultDir  = File(System.getProperty("user.home"), "GradeCalculator").also { it.mkdirs() }
    private val vaultFile = File(vaultDir, "vault.json")

    fun saveVault(files: List<ProcessedFile>) {
        try {
            vaultFile.writeText(json.encodeToString(files))
        } catch (e: Exception) {
            System.err.println("VaultManager: failed to save – ${e.message}")
        }
    }

    fun loadVault(): List<ProcessedFile> {
        if (!vaultFile.exists()) return emptyList()
        return try {
            json.decodeFromString(vaultFile.readText())
        } catch (e: Exception) {
            System.err.println("VaultManager: failed to load – ${e.message}")
            emptyList()
        }
    }
}
