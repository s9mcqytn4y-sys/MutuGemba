package id.co.nierstyd.mutugemba.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import id.co.nierstyd.mutugemba.usecase.GetGreetingTextUseCase
import id.co.nierstyd.mutugemba.usecase.GetSuccessTextUseCase

private const val APP_NAME = "MutuGemba"
private const val SUCCESS_SYMBOL = "✅"

fun main() =
    application {
        val greetingUseCase = GetGreetingTextUseCase(APP_NAME)
        val successUseCase = GetSuccessTextUseCase(SUCCESS_SYMBOL)

        Window(
            onCloseRequest = ::exitApplication,
            title = "MutuGemba – Hello",
        ) {
            var text by remember { mutableStateOf(greetingUseCase.execute()) }

            MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text)
                    Button(onClick = { text = successUseCase.execute() }) {
                        Text("Tes Berhasil")
                    }
                }
            }
        }
    }
