package com.tcgptrading.tcgptrading.features.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tcgptrading.tcgptrading.core.data.TradingApi
import com.tcgptrading.tcgptrading.core.data.runApi
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButton
import com.tcgptrading.tcgptrading.core.presentation.components.TcgButtonStyle
import com.tcgptrading.tcgptrading.core.presentation.theme.TcgColors
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/** Shows digits in groups of 4 ("1190 4583 2201 7764") while the field stores digits only. */
object FriendIdTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val digits = text.text
        val out = digits.chunked(4).joinToString(" ")
        val mapping = object : OffsetMapping {
            // One space is inserted before every group after the first: "1234 5678"
            override fun originalToTransformed(offset: Int): Int =
                (if (offset <= 0) 0 else offset + (offset - 1) / 4).coerceIn(0, out.length)
            override fun transformedToOriginal(offset: Int): Int = (offset - offset / 5).coerceIn(0, digits.length)
        }
        return TransformedText(AnnotatedString(out), mapping)
    }
}

/** The 16-digit Friend ID field with a live "n of 16 digits" hint. */
@Composable
fun FriendIdField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    hint: String? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(friendIdDigits(it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Friend ID") },
            placeholder = { Text("0000 0000 0000 0000", color = TcgColors.Faint) },
            singleLine = true,
            isError = error != null,
            visualTransformation = FriendIdTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TcgColors.Surface, unfocusedContainerColor = TcgColors.Surface,
                focusedBorderColor = TcgColors.Ink, unfocusedBorderColor = TcgColors.Line, cursorColor = TcgColors.Ink,
                focusedLabelColor = TcgColors.Ink,
            ),
        )
        when {
            error != null -> Text(error, style = MaterialTheme.typography.bodySmall, color = TcgColors.RedInk)
            hint != null -> Text(hint, style = MaterialTheme.typography.bodySmall, color = TcgColors.OkText)
            else -> Text(
                if (value.length == 16) "16 of 16 digits" else "${value.length} of 16 digits. Find it on the Friends screen in Pokémon TCG Pocket.",
                style = MaterialTheme.typography.bodySmall, color = TcgColors.Muted,
            )
        }
    }
}

/** Asks for the 16-digit Friend ID; onSaved gets the saved ID. */
@Composable
fun FriendIdDialog(onSaved: (friendId: String) -> Unit, onDismiss: () -> Unit) {
    val api = koinInject<TradingApi>()
    val scope = rememberCoroutineScope()
    var value by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = TcgColors.Surface,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Add your Friend ID", style = MaterialTheme.typography.headlineSmall, color = TcgColors.Ink) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Players you trade with use it to add you in Pokémon TCG Pocket. Only they see it, after an offer is accepted.",
                    style = MaterialTheme.typography.bodyMedium, color = TcgColors.Muted,
                )
                FriendIdField(value = value, onValueChange = { value = it; error = null }, error = error)
            }
        },
        confirmButton = {
            TcgButton(
                "Save", loading = saving, enabled = value.length == 16,
                onClick = {
                    scope.launch {
                        saving = true
                        runApi { api.setFriendId(value) }
                            .onSuccess { ok ->
                                if (ok) onSaved(value) else error = "That Friend ID is already used by another account."
                            }
                            .onFailure { error = it.message ?: "Could not save. Try again." }
                        saving = false
                    }
                },
            )
        },
        dismissButton = { TcgButton("Later", onClick = onDismiss, style = TcgButtonStyle.Secondary, enabled = !saving) },
    )
}
