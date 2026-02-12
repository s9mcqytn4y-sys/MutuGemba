package id.co.nierstyd.mutugemba.desktop

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import id.co.nierstyd.mutugemba.desktop.ui.resources.AppStrings

fun main() =
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = AppStrings.App.WindowTitle,
            icon = painterResource("branding/app_icon_512.png"),
        ) {
            MutuGembaApp()
        }
    }
