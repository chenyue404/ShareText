package com.chenyue404.sharetext

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.app.ActivityCompat

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyStatusBarStyle()

        val oldPort = PortConfig.load(this)

        setContent {
            MaterialTheme {
                SettingsScreen(
                    initialPort = oldPort,
                    initialLanguage = AppLanguageManager.currentOption(this),
                    onSave = { port ->
                        when {
                            port !in 1..65535 -> SaveResult.Invalid
                            port != oldPort && !PortConfig.isPortAvailable(port) -> SaveResult.Occupied
                            else -> {
                                PortConfig.save(this, port)
                                WebService.setPort(port)
                                WebService.requestRestart(this)
                                UiNoticeStore.savePortChanged(this, port)
                                SaveResult.Success
                            }
                        }
                    },
                    onApplyLanguage = { option ->
                        AppLanguageManager.apply(this, option)
                        WebService.requestRestart(this)
                        ActivityCompat.recreate(this)
                    },
                    onDone = { finish() },
                    onBack = { finish() }
                )
            }
        }
    }
}

private enum class SaveResult {
    Success,
    Invalid,
    Occupied
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsScreen(
    initialPort: Int,
    initialLanguage: AppLanguageOption,
    onSave: (Int) -> SaveResult,
    onApplyLanguage: (AppLanguageOption) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var portText by rememberSaveable { mutableStateOf(initialPort.toString()) }
    var selectedLanguage by rememberSaveable { mutableStateOf(initialLanguage) }
    val context = LocalContext.current
    val typedPort = portText.toIntOrNull()
    val suggestedPort =
        if (typedPort != null && typedPort in 1..65535 && !PortConfig.isPortAvailable(typedPort)) {
            PortConfig.suggestAvailablePort(typedPort)
        } else {
            null
        }

    Scaffold(
        containerColor = UiStyle.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UiStyle.TopBarBackground
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.desc_back),
                            tint = UiStyle.PrimaryAction
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(UiStyle.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = UiStyle.ListCardBackground),
                shape = RoundedCornerShape(UiStyle.CornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(UiStyle.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
                ) {
                    val currentLanguageText = when (selectedLanguage) {
                        AppLanguageOption.SYSTEM -> stringResource(R.string.settings_language_system)
                        AppLanguageOption.ENGLISH -> stringResource(R.string.settings_language_english)
                        AppLanguageOption.SIMPLIFIED_CHINESE -> stringResource(R.string.settings_language_chinese_simplified)
                    }
                    Text(
                        text = stringResource(R.string.settings_language_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_language_current,
                            currentLanguageText
                        ),
                        color = UiStyle.SecondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedLanguage = AppLanguageOption.SYSTEM }
                    ) {
                        Text(stringResource(R.string.settings_language_system))
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedLanguage = AppLanguageOption.ENGLISH }
                    ) {
                        Text(stringResource(R.string.settings_language_english))
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedLanguage = AppLanguageOption.SIMPLIFIED_CHINESE }
                    ) {
                        Text(stringResource(R.string.settings_language_chinese_simplified))
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            onApplyLanguage(selectedLanguage)
                            Toast.makeText(
                                context,
                                context.getString(R.string.settings_language_applied),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Text(stringResource(R.string.settings_language_apply))
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = UiStyle.StatusCardBackground),
                shape = RoundedCornerShape(UiStyle.CornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(UiStyle.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
                ) {
                    Text(
                        text = stringResource(R.string.settings_service_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(text = stringResource(R.string.settings_current_port, initialPort))
                    Text(
                        text = stringResource(R.string.settings_port_range),
                        color = UiStyle.SecondaryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = UiStyle.ListCardBackground),
                shape = RoundedCornerShape(UiStyle.CornerRadius),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(UiStyle.CardPadding),
                    verticalArrangement = Arrangement.spacedBy(UiStyle.InnerSpacing)
                ) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text(stringResource(R.string.settings_port_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    if (suggestedPort != null) {
                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { portText = suggestedPort.toString() }
                        ) {
                            Text(stringResource(R.string.settings_use_suggested_port, suggestedPort))
                        }
                    }

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val port = portText.toIntOrNull()
                            if (port == null) {
                                Toast.makeText(context, context.getString(R.string.toast_port_number_only), Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            when (onSave(port)) {
                                SaveResult.Invalid -> {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.toast_port_out_of_range),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                SaveResult.Occupied -> {
                                    Toast.makeText(
                                        context,
                                        PortAdvice.saveFailedOccupiedMessage(context, port),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                                SaveResult.Success -> {
                                    Toast.makeText(
                                        context,
                                        PortAdvice.saveSuccessMessage(context, port),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onDone()
                                }
                            }
                        }
                    ) {
                        Text(stringResource(R.string.settings_save_restart))
                    }
                }
            }
        }
    }
}
