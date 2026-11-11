package com.tk.toolkit.tv.launcher.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.tk.toolkit.tv.R
import com.tk.toolkit.tv.launcher.data.AppLockManager
import com.tk.toolkit.tv.launcher.data.LocaleHelper
import com.tk.toolkit.tv.launcher.data.PinVerifyResult
import com.tk.toolkit.tv.ui.theme.ToolkitColors
import kotlinx.coroutines.delay

private val PinCardShape = RoundedCornerShape(24.dp)
private val PinDotShape = RoundedCornerShape(12.dp)
private val PinKeyShape = RoundedCornerShape(12.dp)

@Composable
fun PinVerifyDialog(
    subtitle: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LocaleHelper.ProvideAppLocale {
            PinVerifyCard(
                subtitle = subtitle,
                onVerified = onVerified,
                onDismiss = onDismiss,
            )
        }
    }
}

@Composable
fun PinVerifyCard(
    subtitle: String,
    onVerified: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var entered by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(AppLockManager.currentLockoutRemainingSeconds()) }
    val firstDigitFocus = remember { FocusRequester() }

    LaunchedEffect(lockoutSeconds) {
        while (lockoutSeconds > 0) {
            delay(1_000L)
            lockoutSeconds = AppLockManager.currentLockoutRemainingSeconds()
        }
    }

    LaunchedEffect(Unit) {
        firstDigitFocus.requestFocusWhenReady()
    }

    fun submitPin(pin: String) {
        when (val result = AppLockManager.verifyPin(context, pin)) {
            PinVerifyResult.Success -> onVerified()
            is PinVerifyResult.Wrong -> {
                entered = ""
                errorMessage = context.getString(R.string.pin_wrong_attempts, result.remainingAttempts)
            }
            is PinVerifyResult.LockedOut -> {
                entered = ""
                lockoutSeconds = result.remainingSeconds
                errorMessage = null
            }
        }
    }

    fun appendDigit(digit: Char) {
        if (lockoutSeconds > 0 || entered.length >= AppLockManager.PIN_LENGTH) return
        errorMessage = null
        val updated = entered + digit
        entered = updated
        if (updated.length == AppLockManager.PIN_LENGTH) {
            submitPin(updated)
        }
    }

    val statusText = when {
        lockoutSeconds > 0 -> stringResource(R.string.pin_locked_out, lockoutSeconds)
        errorMessage != null -> errorMessage
        else -> null
    }

    PinPadCard(
        title = stringResource(R.string.pin_verify_title),
        subtitle = subtitle,
        enteredLength = entered.length,
        statusText = statusText,
        isError = statusText != null,
        firstDigitFocus = firstDigitFocus,
        onDigit = ::appendDigit,
        onBackspace = {
            if (entered.isNotEmpty()) {
                entered = entered.dropLast(1)
                errorMessage = null
            }
        },
        onCancel = onDismiss,
    )
}

@Composable
fun PinSetupDialog(
    onPinSaved: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(1) }
    var firstPin by remember { mutableStateOf("") }
    var secondPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var matchedReady by remember { mutableStateOf(false) }

    val firstDigitFocus = remember { FocusRequester() }
    val saveButtonFocus = remember { FocusRequester() }

    LaunchedEffect(step) {
        firstDigitFocus.requestFocusWhenReady()
    }
    LaunchedEffect(matchedReady) {
        if (matchedReady) {
            saveButtonFocus.requestFocusWhenReady()
        }
    }

    fun onDigit(digit: Char) {
        errorMessage = null
        if (step == 1) {
            if (firstPin.length >= AppLockManager.PIN_LENGTH) return
            val updated = firstPin + digit
            firstPin = updated
            if (updated.length == AppLockManager.PIN_LENGTH) {
                step = 2
                secondPin = ""
                matchedReady = false
            }
        } else if (!matchedReady) {
            if (secondPin.length >= AppLockManager.PIN_LENGTH) return
            val updated = secondPin + digit
            secondPin = updated
            if (updated.length == AppLockManager.PIN_LENGTH) {
                if (updated == firstPin) {
                    matchedReady = true
                } else {
                    secondPin = ""
                    matchedReady = false
                    errorMessage = context.getString(R.string.pin_setup_mismatch)
                }
            }
        }
    }

    fun onBackspace() {
        errorMessage = null
        if (matchedReady) {
            matchedReady = false
            secondPin = secondPin.dropLast(1)
            return
        }
        if (step == 1) {
            if (firstPin.isNotEmpty()) firstPin = firstPin.dropLast(1)
        } else {
            if (secondPin.isNotEmpty()) {
                secondPin = secondPin.dropLast(1)
            } else {
                step = 1
                firstPin = firstPin.dropLast(1)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LocaleHelper.ProvideAppLocale {
            PinPadCard(
                title = if (step == 1) {
                    stringResource(R.string.pin_setup_step1_title)
                } else {
                    stringResource(R.string.pin_setup_step2_title)
                },
                subtitle = if (step == 1) {
                    stringResource(R.string.pin_setup_step1_subtitle)
                } else {
                    stringResource(R.string.pin_setup_step2_subtitle)
                },
                enteredLength = if (step == 1) firstPin.length else secondPin.length,
                statusText = errorMessage,
                isError = errorMessage != null,
                firstDigitFocus = firstDigitFocus,
                onDigit = ::onDigit,
                onBackspace = ::onBackspace,
                onCancel = onDismiss,
                confirmAction = if (matchedReady) {
                    {
                        Button(
                            onClick = { onPinSaved(firstPin) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .focusRequester(saveButtonFocus),
                            shape = ButtonDefaults.shape(PinKeyShape),
                            colors = ButtonDefaults.colors(
                                containerColor = ToolkitColors.Accent,
                                contentColor = Color(0xFF0A0F18),
                                focusedContainerColor = ToolkitColors.OnSurface,
                                focusedContentColor = ToolkitColors.Background,
                            ),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.pin_setup_save_btn),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                )
                            }
                        }
                    }
                } else null,
            )
        }
    }
}

@Composable
fun PinConfirmRemoveDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { cancelFocus.requestFocusWhenReady() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        LocaleHelper.ProvideAppLocale {
            Column(
                Modifier
                    .width(400.dp)
                    .clip(PinCardShape)
                    .background(ToolkitColors.SurfaceRaised)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), PinCardShape)
                    .ignoreHeldSelectKey()
                    .padding(24.dp),
            ) {
                Text(
                    stringResource(R.string.app_lock_remove_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = ToolkitColors.OnSurface,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.app_lock_remove_confirm_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = ToolkitColors.OnSurfaceMuted,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .focusRequester(cancelFocus),
                        shape = ButtonDefaults.shape(PinKeyShape),
                        colors = ButtonDefaults.colors(
                            containerColor = ToolkitColors.OnSurface.copy(alpha = 0.08f),
                            contentColor = ToolkitColors.OnSurface,
                            focusedContainerColor = ToolkitColors.OnSurface,
                            focusedContentColor = ToolkitColors.Background,
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.app_lock_cancel_btn))
                        }
                    }
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = ButtonDefaults.shape(PinKeyShape),
                        colors = ButtonDefaults.colors(
                            containerColor = Color(0xFFFF6B6B).copy(alpha = 0.18f),
                            contentColor = Color(0xFFFF8585),
                            focusedContainerColor = Color(0xFFFF6B6B),
                            focusedContentColor = Color(0xFF140707),
                        ),
                    ) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.app_lock_remove_confirm_btn))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinPadCard(
    title: String,
    subtitle: String,
    enteredLength: Int,
    statusText: String?,
    isError: Boolean,
    firstDigitFocus: FocusRequester,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onCancel: () -> Unit,
    confirmAction: (@Composable () -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(380.dp)
            .clip(PinCardShape)
            .background(ToolkitColors.SurfaceRaised)
            .border(1.dp, Color.White.copy(alpha = 0.1f), PinCardShape)
            .ignoreHeldSelectKey()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val digit = keyCodeToDigit(event.nativeKeyEvent.keyCode)
                if (digit != null) {
                    onDigit(digit)
                    return@onPreviewKeyEvent true
                }
                if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DEL) {
                    onBackspace()
                    return@onPreviewKeyEvent true
                }
                false
            }
            .padding(horizontal = 24.dp, vertical = 22.dp),
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ToolkitColors.Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Lock,
                contentDescription = null,
                tint = ToolkitColors.Accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = ToolkitColors.OnSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = ToolkitColors.OnSurfaceMuted,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(18.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(AppLockManager.PIN_LENGTH) { idx ->
                val filled = idx < enteredLength
                val borderColor = when {
                    isError -> Color(0xFFFF6B6B).copy(alpha = 0.7f)
                    filled -> ToolkitColors.Accent
                    else -> Color.White.copy(alpha = 0.14f)
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(PinDotShape)
                        .background(ToolkitColors.Surface)
                        .border(1.5.dp, borderColor, PinDotShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (filled) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(ToolkitColors.Accent),
                        )
                    } else {
                        Box(
                            Modifier
                                .width(12.dp)
                                .height(2.dp)
                                .background(ToolkitColors.OnSurfaceMuted.copy(alpha = 0.3f)),
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .padding(top = 10.dp, bottom = 10.dp)
                .height(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!statusText.isNullOrBlank()) {
                Text(
                    statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isError) Color(0xFFFF7A7A) else ToolkitColors.Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (confirmAction != null) {
            confirmAction()
            Spacer(Modifier.height(12.dp))
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("DEL", "0", "CANCEL"),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            rows.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    row.forEach { key ->
                        val isFirstDigit = key == "1"
                        PinKeypadButton(
                            key = key,
                            onClick = {
                                when (key) {
                                    "DEL" -> onBackspace()
                                    "CANCEL" -> onCancel()
                                    else -> onDigit(key[0])
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .then(if (isFirstDigit) Modifier.focusRequester(firstDigitFocus) else Modifier),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKeypadButton(
    key: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isAction = key == "DEL" || key == "CANCEL"
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        shape = ButtonDefaults.shape(PinKeyShape),
        colors = ButtonDefaults.colors(
            containerColor = if (isAction) {
                ToolkitColors.OnSurface.copy(alpha = 0.05f)
            } else {
                ToolkitColors.OnSurface.copy(alpha = 0.09f)
            },
            contentColor = if (isAction) ToolkitColors.OnSurfaceMuted else ToolkitColors.OnSurface,
            focusedContainerColor = ToolkitColors.OnSurface,
            focusedContentColor = ToolkitColors.Background,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (key) {
                "DEL" -> Icon(
                    Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = stringResource(R.string.pin_key_backspace),
                    modifier = Modifier.size(18.dp),
                )
                "CANCEL" -> Text(
                    stringResource(R.string.pin_key_cancel),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                )
                else -> Text(
                    key,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

private fun keyCodeToDigit(keyCode: Int): Char? = when (keyCode) {
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> ('0'.code + (keyCode - KeyEvent.KEYCODE_0)).toChar()
    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> ('0'.code + (keyCode - KeyEvent.KEYCODE_NUMPAD_0)).toChar()
    else -> null
}

