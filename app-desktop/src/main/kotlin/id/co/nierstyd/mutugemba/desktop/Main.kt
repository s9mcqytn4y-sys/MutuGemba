package id.co.nierstyd.mutugemba.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "MutuGemba - QC TPS",
        ) {
            MutuGembaApp()
        }
    }
