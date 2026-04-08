package com.baobao.fatloss.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.baobao.fatloss.R
import com.baobao.fatloss.data.local.LanguageStore

@Composable
fun LanguagePickerDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val options = listOf(
        Option(stringResource(R.string.settings_language_system), LanguageStore.LANG_SYSTEM),
        Option(stringResource(R.string.settings_language_zh), LanguageStore.LANG_ZH),
        Option(stringResource(R.string.settings_language_en), LanguageStore.LANG_EN)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_language)) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (option.value == currentLanguage),
                                onClick = {
                                    if (option.value != currentLanguage) {
                                        onLanguageSelected(option.value)
                                        // 保存并重启以刷新语言
                                        // 逻辑在调用处处理，也可以在这里处理
                                    }
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (option.value == currentLanguage),
                            onClick = null // 由 row 处理
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_confirm))
            }
        }
    )
}

private data class Option(val label: String, val value: String)
