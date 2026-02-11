package id.co.nierstyd.mutugemba.data.local.assetstore

import id.co.nierstyd.mutugemba.domain.model.AssetKey
import id.co.nierstyd.mutugemba.domain.model.AssetRef
import id.co.nierstyd.mutugemba.domain.repository.AssetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import okio.source
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.security.MessageDigest
import java.util.LinkedHashMap
import java.util.UUID

private val SHA256_REGEX = Regex("^[a-f0-9]{64}$")

interface AssetByteCache {
    fun get(key: String): ByteArray?

    fun put(
        key: String,
        value: ByteArray,
    )

    fun remove(key: String)
}

class LruAssetByteCache(
    private val maxEntries: Int = 128,
) : AssetByteCache {
    private val map =
        object : LinkedHashMap<String, ByteArray>(maxEntries, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ByteArray>?): Boolean =
                size > maxEntries
        }

    override fun get(key: String): ByteArray? = synchronized(map) { map[key] }

    override fun put(
        key: String,
        value: ByteArray,
    ) {
        synchronized(map) {
            map[key] = value
        }
    }

    override fun remove(key: String) {
        synchronized(map) {
            map.remove(key)
        }
    }
}

class HashAssetStore(
    private val fs: FileSystem,
    private val root: Path,
    private val logger: Logger = LoggerFactory.getLogger(HashAssetStore::class.java),
    private val cache: AssetByteCache? = null,
) : AssetStore {
    override suspend fun putBytes(
        key: AssetKey,
        bytes: ByteArray,
        mime: String,
    ): AssetRef =
        withContext(Dispatchers.IO) {
            require(key.type == "part_image") { "Unsupported key type: ${key.type}" }
            require(mime == "image/png") { "Only image/png supported for now." }

            val normalizedSha = key.sha256.lowercase()
            require(SHA256_REGEX.matches(normalizedSha)) { "Invalid SHA256 format." }

            val computed = sha256Hex(bytes)
            require(computed == normalizedSha) {
                "SHA256 mismatch for uniq_no=${key.uniqNo}. expected=$normalizedSha actual=$computed"
            }

            val rel = "assets_store/images/sha256/${normalizedSha.take(2)}/$normalizedSha.png"
            val target = safeResolve(root, rel)

            fs.createDirectories(target.parent ?: error("Invalid target parent for $rel"))
            if (!fs.exists(target)) {
                val tmp = (target.parent ?: error("Invalid target parent")) / "${target.name}.tmp-${UUID.randomUUID()}"
                fs.write(tmp) { write(bytes) }
                fs.atomicMove(tmp, target)
            }

            cache?.put(rel, bytes)
            AssetRef(
                storageRelPath = rel,
                sha256 = normalizedSha,
                mime = mime,
                sizeBytes = bytes.size.toLong(),
            )
        }

    override suspend fun getBytes(ref: AssetRef): ByteArray? =
        withContext(Dispatchers.IO) {
            cache?.get(ref.storageRelPath)?.let { return@withContext it }
            val path = safeResolve(root, ref.storageRelPath)
            if (!fs.exists(path)) return@withContext null

            val bytes = fs.read(path) { readByteArray() }
            cache?.put(ref.storageRelPath, bytes)
            bytes
        }

    override suspend fun exists(ref: AssetRef): Boolean =
        withContext(Dispatchers.IO) {
            fs.exists(safeResolve(root, ref.storageRelPath))
        }

    override suspend fun openStream(ref: AssetRef): InputStream? =
        withContext(Dispatchers.IO) {
            val path = safeResolve(root, ref.storageRelPath)
            if (!fs.exists(path)) return@withContext null
            fs.source(path).buffer().inputStream()
        }

    override suspend fun delete(ref: AssetRef): Boolean =
        withContext(Dispatchers.IO) {
            val path = safeResolve(root, ref.storageRelPath)
            if (!fs.exists(path)) return@withContext false
            runCatching {
                fs.delete(path)
                cache?.remove(ref.storageRelPath)
                true
            }.onFailure { throwable ->
                logger.warn("Failed deleting asset {}", ref.storageRelPath, throwable)
            }.getOrDefault(false)
        }
}

fun createDesktopHashAssetStore(rootDir: java.nio.file.Path): HashAssetStore =
    HashAssetStore(
        fs = FileSystem.SYSTEM,
        root =
            rootDir
                .toAbsolutePath()
                .normalize()
                .toString()
                .toPath(normalize = true),
    )

private fun safeResolve(
    root: Path,
    relpath: String,
): Path {
    val normalizedRel = relpath.replace('\\', '/').trimStart('/')
    require(!normalizedRel.contains("..")) { "Path traversal not allowed: $relpath" }
    return root / normalizedRel
}

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest
        .getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { b -> "%02x".format(b) }
