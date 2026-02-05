package id.co.nierstyd.mutugemba.usecase

class GetGreetingTextUseCase(
    private val appName: String,
) {
    fun execute(): String = "Halo! $appName siap."
}

class GetSuccessTextUseCase(
    private val successSymbol: String,
) {
    fun execute(): String = "Berhasil $successSymbol"
}
