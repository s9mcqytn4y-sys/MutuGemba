package id.co.nierstyd.mutugemba.usecase.asset

import id.co.nierstyd.mutugemba.domain.model.AssetRef
import id.co.nierstyd.mutugemba.domain.repository.AssetRepository

class GetActiveImageRefUseCase(
    private val repository: AssetRepository,
) {
    suspend fun execute(uniqNo: String): AssetRef? = repository.getActiveImageRef(uniqNo)
}

class LoadImageBytesUseCase(
    private val repository: AssetRepository,
) {
    suspend fun execute(ref: AssetRef): ByteArray? = repository.loadImageBytes(ref)
}
