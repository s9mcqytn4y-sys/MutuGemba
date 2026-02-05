package id.co.nierstyd.mutugemba.desktop.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import id.co.nierstyd.mutugemba.desktop.ui.components.AppTextField
import id.co.nierstyd.mutugemba.desktop.ui.components.FieldSpec
import id.co.nierstyd.mutugemba.desktop.ui.components.InfoCard
import id.co.nierstyd.mutugemba.desktop.ui.components.PrimaryButton
import id.co.nierstyd.mutugemba.desktop.ui.components.SectionHeader
import id.co.nierstyd.mutugemba.desktop.ui.components.StatusBanner
import id.co.nierstyd.mutugemba.desktop.ui.theme.NeutralTextMuted
import id.co.nierstyd.mutugemba.desktop.ui.theme.Spacing
import id.co.nierstyd.mutugemba.usecase.UserFeedback

@Composable
fun LoginScreen(
    feedback: UserFeedback?,
    onLogin: (String, String) -> Unit,
) {
    var nameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.widthIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        SectionHeader(
            title = "Selamat Datang",
            subtitle = "Masuk untuk melanjutkan ke MutuGemba QC TPS.",
        )

        InfoCard(title = "Login") {
            AppTextField(
                spec =
                    FieldSpec(
                        label = "Nama",
                        placeholder = "Masukkan nama user",
                    ),
                value = nameInput,
                onValueChange = { nameInput = it },
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            PasswordField(
                label = "Password",
                value = passwordInput,
                onValueChange = { passwordInput = it },
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            PrimaryButton(
                text = "Masuk",
                onClick = { onLogin(nameInput, passwordInput) },
                enabled = nameInput.isNotBlank() && passwordInput.isNotBlank(),
            )
        }

        feedback?.let { StatusBanner(feedback = it) }

        Row(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
            Text(
                text = "Tip: gunakan akun resmi QC untuk akses yang aman.",
                style = MaterialTheme.typography.body2,
                color = NeutralTextMuted,
            )
        }
    }
}

@Composable
private fun PasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text("Masukkan password") },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = PasswordVisualTransformation(),
        colors = TextFieldDefaults.outlinedTextFieldColors(),
    )
}
