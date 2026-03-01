package org.stypox.dicio.ui.nav

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextFieldDefaults.indicatorLine
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stypox.dicio.R
import org.stypox.dicio.ui.theme.AppTheme

@Composable
fun AppBarTitle(text: String) {
    Text(
        text,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun AppBarBackIcon(onBackClick: () -> Unit) {
    IconButton(onClick = onBackClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.back)
        )
    }
}

@Composable
fun AppBarDrawerIcon(onDrawerClick: () -> Unit, isClosed: Boolean) {
    IconButton(
        onClick = onDrawerClick,
        modifier = Modifier.testTag("drawer_handle"),
    ) {
        Icon(
            imageVector = Icons.Filled.Menu,
            contentDescription = stringResource(
                if (isClosed) {
                    R.string.drawer_open
                } else {
                    R.string.drawer_close
                }
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBarTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val textStyle = LocalTextStyle.current
    // make sure there is no background color in the decoration box
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
    )

    // If color is not provided via the text style, use content color as a default
    val textColor = textStyle.color.takeOrElse {
        MaterialTheme.colorScheme.onSurface
    }
    val mergedTextStyle = textStyle.merge(
        TextStyle(
            color = textColor,
            fontSize = MaterialTheme.typography.titleLarge.fontSize,
        )
    )

    // request focus when this composable is first initialized
    val focusRequester = remember { FocusRequester() }
    SideEffect {
        focusRequester.requestFocus()
    }

    // set the correct cursor position when this composable is first initialized
    var textFieldValueState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value, TextRange(value.length)))
    }
    val textFieldValue = textFieldValueState.copy(text = value) // keep the value updated

    // copied from the BasicTextField implementation that takes a String
    SideEffect {
        if (textFieldValue.selection != textFieldValueState.selection ||
            textFieldValue.composition != textFieldValueState.composition) {
            textFieldValueState = textFieldValue
        }
    }

    CompositionLocalProvider(
        LocalTextSelectionColors provides LocalTextSelectionColors.current
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValueState = it
                // remove newlines to avoid strange layout issues, and also because singleLine=true
                onValueChange(it.text.replace("\n", ""))
            },
            modifier = modifier
                .fillMaxWidth()
                .heightIn(32.dp)
                .indicatorLine(
                    enabled = true,
                    isError = false,
                    interactionSource = interactionSource,
                    colors = colors
                )
                .focusRequester(focusRequester),
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(textColor),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            singleLine = true,
            decorationBox = { innerTextField ->
                // places text field with placeholder and appropriate bottom padding
                TextFieldDefaults.DecorationBox(
                    value = value,
                    innerTextField = innerTextField,
                    enabled = true,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    isError = false,
                    placeholder = { Text(text = hint) },
                    colors = colors,
                    contentPadding = PaddingValues(bottom = 4.dp),
                )
            }
        )
    }
}


@Composable
fun SearchTopAppBar(
    searchString: String?,
    setSearchString: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hint: String,
    title: @Composable () -> Unit,
    searchIcon: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit
) {
    if (searchString != null) {
        SearchTopAppBarExpanded(
            searchString = searchString,
            setSearchString = setSearchString,
            onSearch = onSearch,
            hint = hint,
        )
    } else {
        SearchTopAppBarUnexpanded(
            onSearchClick = { setSearchString("") },
            title = title,
            searchIcon = searchIcon,
            navigationIcon = navigationIcon,
        )
    }
}

@Preview
@Composable
private fun SearchTopAppBarPreview() {
    var searchString by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("The initial title") }

    AppTheme {
        SearchTopAppBar(
            searchString = searchString,
            setSearchString = { searchString = it },
            onSearch = { title = it },
            hint = "The hint…",
            title = { AppBarTitle(title) },
            searchIcon = { Icon(Icons.Default.QuestionAnswer, contentDescription = null) },
            navigationIcon = { AppBarDrawerIcon(onDrawerClick = { }, isClosed = true) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopAppBarUnexpanded(
    onSearchClick: () -> Unit,
    title: @Composable () -> Unit,
    searchIcon: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = {
            IconButton(onClick = onSearchClick) {
                searchIcon()
            }
        }
    )
}

@Preview
@Composable
private fun SearchTopAppBarUnexpandedPreview() {
    AppTheme {
        SearchTopAppBarUnexpanded(
            onSearchClick = {},
            title = { AppBarTitle("The title") },
            searchIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            navigationIcon = { AppBarDrawerIcon(onDrawerClick = { }, isClosed = true) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopAppBarExpanded(
    searchString: String,
    setSearchString: (String?) -> Unit,
    onSearch: (String) -> Unit,
    hint: String,
) {
    // if setSearchString(null) were called without first setting exiting=true, the onValueChange
    // below would call setSearchString() one last time with the old input, causing the search to
    // reopen right away
    var exiting by remember { mutableStateOf(false) }
    fun exitSearch() {
        exiting = true
        setSearchString(null)
    }

    TopAppBar(
        title = {
            AppBarTextField(
                value = searchString,
                onValueChange = {
                    if (!exiting) {
                        setSearchString(it)
                    }
                },
                hint = hint,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onSearch(searchString)
                    exitSearch()
                }),
                modifier = Modifier.onKeyEvent {
                    if (it.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_ENTER) {
                        onSearch(searchString)
                        exitSearch()
                        return@onKeyEvent true
                    }
                    return@onKeyEvent false
                },
            )
        },
        navigationIcon = {
            AppBarBackIcon {
                exitSearch()
            }
        },
        actions = {
            IconButton(
                onClick = {
                    setSearchString("")
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = stringResource(R.string.clear)
                )
            }
        }
    )

    BackHandler {
        // this will only be triggered if the user presses back when the keyboard is already closed
        exitSearch()
    }
}

@Preview
@Composable
private fun SearchTopAppBarExpandedPreview() {
    var searchString by rememberSaveable { mutableStateOf("") }

    AppTheme {
        SearchTopAppBarExpanded(
            searchString = searchString,
            setSearchString = { searchString = it ?: "" },
            onSearch = {},
            hint = "The hint…",
        )
    }
}
