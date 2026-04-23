package org.stypox.dicio.io.input.stt_popup

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import org.stypox.dicio.R
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.ui.home.SttFab
import org.stypox.dicio.ui.theme.AppTheme
import org.stypox.dicio.util.ShareUtils

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SttPopupBottomSheet(
    customHint: String?,
    onDoneClicked: ((List<Pair<String, Float>>) -> Unit)?,
    onDismissRequest: () -> Unit,
) {
    val viewModel: SttPopupViewModel = hiltViewModel()
    val textFieldValue = viewModel.textFieldValue.collectAsState()
    val sttState = viewModel.sttInputDevice.uiState.collectAsState().value ?: SttState.NotAvailable

    // Calculate insets here instead of inside ModalBottomSheet, because on old APIs (tested on API
    // 27) ModalBottomSheet seems to consume them and they become zero.
    // Use `systemBars + displayCutout` instead of `safeDrawing` because `safeDrawing` also includes
    // the `ime`, which is instead added as insets directly to the `ModalBottomSheet` window,
    // since these insets are not really meant to be drawn under.
    val isKeyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val isLandscape = // no horizontal padding if the dialog will not stretch the whole screen
        LocalConfiguration.current.screenWidthDp.dp > BottomSheetDefaults.SheetMaxWidth
    val insets = if (isLandscape && isKeyboardOpen) {
        PaddingValues(0.dp)
    } else {
        WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .only(
                if (isLandscape) {
                    WindowInsetsSides.Bottom
                } else if (isKeyboardOpen) {
                    WindowInsetsSides.Horizontal
                } else {
                    WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                }
            )
            .asPaddingValues()
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        // insets are applied on the content, to also draw the background under the navigation bars,
        // except for the `ime` insets which are not meant to be drawn under
        contentWindowInsets = { WindowInsets.ime },
    ) {
        SttPopupBottomSheet(
            customHint = customHint,
            onDoneClicked = if (onDoneClicked == null) {
                null
            } else { ->
                onDoneClicked(viewModel.getUtterancesWithConfidence())
            },
            onDismissRequest = onDismissRequest,
            sttState = sttState,
            onSttClick = viewModel::onSttClick,
            textFieldValue = textFieldValue.value,
            onTextFieldChange = viewModel::setTextFieldValue,
            modifier = Modifier.padding(insets),
        )
    }
}

@Composable
private fun SttPopupBottomSheet(
    customHint: String?,
    onDoneClicked: (() -> Unit)?,
    onDismissRequest: () -> Unit,
    sttState: SttState,
    onSttClick: () -> Unit,
    textFieldValue: String,
    onTextFieldChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        SttPopupTextField(
            value = textFieldValue,
            onValueChange = onTextFieldChange,
            customHint = customHint,
            enabled = sttState != SttState.Listening &&
                sttState != SttState.SilenceDetected &&
                sttState != SttState.Thinking,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SttPopupButton(
                icon = Icons.Default.ContentCopy,
                contentDescription = R.string.copy_to_clipboard,
                onClick = {
                    ShareUtils.copyToClipboard(context, textFieldValue)
                    onDismissRequest()
                },
            )

            // boxing the SttFab so that the Box can stretch as much as weight(1.0f) allows,
            // but the nested SttFab will always be as small as possible
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1.0f)
                    .padding(vertical = 16.dp),
            ) {
                SttFab(
                    state = sttState,
                    onClick = onSttClick,
                )
            }

            if (onDoneClicked == null) {
                SttPopupButton(
                    icon = Icons.Default.Share,
                    contentDescription = R.string.share,
                    onClick = {
                        ShareUtils.shareText(context, "", textFieldValue)
                        onDismissRequest()
                    },
                )
            } else {
                SttPopupButton(
                    icon = Icons.Default.Done,
                    contentDescription = R.string.done,
                    onClick = {
                        onDoneClicked()
                        onDismissRequest()
                    },
                )
            }
        }
    }
}

@Composable
private fun SttPopupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    customHint: String?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val fontStyle = MaterialTheme.typography.titleLarge
        .copy(textAlign = TextAlign.Center, fontWeight = FontWeight.Normal)

    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = customHint ?: stringResource(R.string.stt_say_something),
                style = fontStyle,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            errorContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        textStyle = fontStyle,
        enabled = enabled,
        modifier = modifier,
    )
}

@Composable
private fun SttPopupButton(
    icon: ImageVector,
    @StringRes contentDescription: Int,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = stringResource(contentDescription),
            modifier = Modifier.size(32.dp),
        )
    }
}

@Preview
@Composable
private fun SttPopupBottomSheetPreview() {
    val density = LocalDensity.current
    var textFieldValue by rememberSaveable { mutableStateOf("") }

    AppTheme {
        @OptIn(ExperimentalMaterial3Api::class)
        BottomSheetScaffold(
            sheetContent = {
                SttPopupBottomSheet(
                    customHint = null,
                    onDoneClicked = {},
                    onDismissRequest = {},
                    sttState = SttState.NotAvailable,
                    onSttClick = {},
                    textFieldValue = textFieldValue,
                    onTextFieldChange = { textFieldValue = it },
                )
            },
            // make the preview always expanded
            scaffoldState = BottomSheetScaffoldState(
                SheetState(
                    skipPartiallyExpanded = true,
                    confirmValueChange = { true },
                    initialValue = SheetValue.Expanded,
                    skipHiddenState = false,
                    positionalThreshold = { with(density) { 56.dp.toPx() } } ,
                    velocityThreshold = { with(density) { 125.dp.toPx() } },
                ),
                SnackbarHostState(),
            )
        ) {
            Spacer(modifier = Modifier.padding(it))
        }
    }
}
