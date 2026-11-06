package com.example.trudomix.data.local

import java.io.File
import java.security.MessageDigest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ResponseSnapshot(val body: String, val mediaType: String, val code: Int = 200, val savedAt: Long = System.currentTimeMillis())

/** Private, bounded storage. Keys include the server, full URL and session fingerprint. */
class ResponseSnapshots(private val root: File, private val maxBytes: Long = 48L * 1024 * 1024) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    @Volatile var generation: Long = 0; private set
    private val warmed = mutableMapOf<String, Long>()

    fun key(server: String, token: String?, url: String): String =
        MessageDigest.getInstance("SHA-256").digest("$server\n${token.orEmpty()}\n$url".toByteArray()).joinToString("") { "%02x".format(it) }

    @Synchronized fun read(key: String, now: Long = System.currentTimeMillis()): ResponseSnapshot? = runCatching {
        val file = File(root, key)
        if (!file.isFile || file.length() > 4 * 1024 * 1024) return null
        json.decodeFromString<ResponseSnapshot>(file.readText()).takeIf { now - it.savedAt in 0..7L * 24 * 60 * 60 * 1000 }
    }.getOrNull()

    @Synchronized fun write(key: String, snapshot: ResponseSnapshot, expectedGeneration: Long, warm: Boolean) {
        if (expectedGeneration != generation || snapshot.body.length > 2 * 1024 * 1024) return
        runCatching {
            root.mkdirs()
            val temporary = File(root, "$key.tmp")
            temporary.writeText(json.encodeToString(snapshot))
            val target = File(root, key)
            try {
                java.nio.file.Files.move(temporary.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                java.nio.file.Files.move(temporary.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
            if (warm) warmed[key] = System.currentTimeMillis() + 60_000
            val files = root.listFiles()?.filter { !it.name.endsWith(".tmp") }?.sortedBy { it.lastModified() }.orEmpty()
            var size = files.sumOf { it.length() }
            for (file in files) {
                if (size <= maxBytes) break
                val bytes = file.length()
                if (file.delete()) { size -= bytes; warmed.remove(file.name) }
            }
        }
    }

    @Synchronized fun takeWarm(key: String, consume: Boolean, now: Long = System.currentTimeMillis()): Boolean {
        val until = warmed[key] ?: return false
        if (consume || until < now) warmed.remove(key)
        return until >= now
    }
    @Synchronized fun clearWarm() { warmed.clear() }
    @Synchronized fun remove(key: String) { warmed.remove(key); File(root,key).delete() }
    @Synchronized fun clear() {
        generation++
        warmed.clear()
        root.listFiles()?.forEach { it.delete() }
    }
}
