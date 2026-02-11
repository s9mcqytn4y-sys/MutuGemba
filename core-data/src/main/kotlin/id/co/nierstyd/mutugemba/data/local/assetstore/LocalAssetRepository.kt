package id.co.nierstyd.mutugemba.data.local.assetstore

import id.co.nierstyd.mutugemba.data.local.db.SqliteDatabase
import id.co.nierstyd.mutugemba.domain.model.AssetRef
import id.co.nierstyd.mutugemba.domain.repository.AssetRepository
import id.co.nierstyd.mutugemba.domain.repository.AssetStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAssetRepository(
    private val database: SqliteDatabase,
    private val store: AssetStore,
) : AssetRepository {
    override suspend fun getActiveImageRef(uniqNo: String): AssetRef? =
        withContext(Dispatchers.IO) {
            database.read { connection ->
                connection
                    .prepareStatement(
                        """
                        SELECT
                          ia.storage_relpath,
                          ia.sha256,
                          ia.mime,
                          ia.size_bytes
                        FROM image_asset ia
                        JOIN part p ON p.part_id = ia.part_id
                        WHERE p.uniq_no = ?
                          AND ia.active = 1
                        LIMIT 1
                        """.trimIndent(),
                    ).use { statement ->
                        statement.setString(1, uniqNo)
                        statement.executeQuery().use { rs ->
                            if (!rs.next()) {
                                null
                            } else {
                                AssetRef(
                                    storageRelPath = rs.getString("storage_relpath"),
                                    sha256 = rs.getString("sha256"),
                                    mime = rs.getString("mime"),
                                    sizeBytes = rs.getLong("size_bytes"),
                                )
                            }
                        }
                    }
            }
        }

    override suspend fun loadImageBytes(ref: AssetRef): ByteArray? = store.getBytes(ref)
}
