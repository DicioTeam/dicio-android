package org.stypox.dicio.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.job
import org.stypox.dicio.R
import kotlin.math.roundToInt

interface SettingWithValue<T> {
    @Composable
    fun Render(
        value: T,
        onValueChange: (T) -> Unit,
    )
}

class BooleanSetting(
    private val title: String,
    private val icon: ImageVector? = null,
    private val descriptionOff: String? = null,
    private val descriptionOn: String? = null,
) : SettingWithValue<Boolean> {
    @Composable
    override fun Render(
        value: Boolean,
        onValueChange: (Boolean) -> Unit,
    ) {
        SettingsItem(
            modifier = Modifier.clickable { onValueChange(!value) },
            title = title,
            icon = icon,
            description = if (value) descriptionOn else descriptionOff,
            content = {
                Switch(
                    checked = value,
                    onCheckedChange = onValueChange,
                )
            },
        )
    }
}

class IntSetting(
    private val title: String,
    private val minimum: Int,
    private val maximum: Int,
    private val icon: ImageVector? = null,
    private val description: (@Composable (Int) -> String)? = null,
) : SettingWithValue<Int> {
    init {
        assert(maximum > minimum)
    }

    @Composable
    override fun Render(
        value: Int,
        onValueChange: (Int) -> Unit,
    ) {
        SettingsItem(
            title = title,
            icon = icon,
            description = description?.invoke(value),
            innerContent = {
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValueChange(it.roundToInt()) },
                    steps = maximum - minimum - 1,
                    valueRange = minimum.toFloat()..maximum.toFloat()
                )
            },
        )
    }
}

class ListSetting<T>(
    private val title: String,
    private val icon: ImageVector? = null,
    private val description: String? = null,
    private val possibleValues: List<Value<T>>,
) : SettingWithValue<T> {
    data class Value<T>(
        val value: T,
        val name: String,
        val description: String? = null,
        val icon: ImageVector? = null,
    )

    @Composable
    override fun Render(
        value: T,
        onValueChange: (T) -> Unit,
    ) {
        var dialogOpen by rememberSaveable { mutableStateOf(false) }

        val currentValueName = possibleValues.firstOrNull { it.value == value }?.name

        SettingsItem(
            modifier = Modifier.clickable { dialogOpen = true },
            title = title,
            icon = icon,
            description = if (description == null) {
                currentValueName
            } else if (currentValueName == null) {
                description
            } else {
                stringResource(
                    R.string.settings_description_with_value,
                    description,
                    currentValueName
                )
            },
        )

        if (dialogOpen) {
            ChooserDialog(
                value = value,
                onValueChange = onValueChange,
                onDismissRequest = { dialogOpen = false }
            )
        }
    }

    @Composable
    fun ChooserDialog(
        value: T,
        onValueChange: (T) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    items(possibleValues) {
                        ListSettingChooserDialogItem(
                            name = it.name,
                            description = it.description,
                            icon = it.icon,
                            selected = it.value == value,
                            onClick = {
                                onValueChange(it.value)
                                onDismissRequest()
                            }
                        )
                    }
                }
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    }
}

@Composable
fun ListSettingChooserDialogItem(
    name: String,
    description: String?,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.weight(1.0f)
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.fontSize * 1.1,
                modifier = Modifier.fillMaxWidth()
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = name,
            )
        }
    }
}

@Preview
@Composable
private fun ListSettingChooserDialogItemPreview() {
    ListSettingChooserDialogItem(
        name = "Some item",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
        icon = Icons.Default.Pets,
        selected = true,
        onClick = {}
    )
}

@Preview
@Composable
private fun ListSettingChooserDialogPreview() {
    ListSetting(
        title = "List of things",
        possibleValues = listOf(
            ListSetting.Value(true, "True!", "Lorem ".repeat(13), icon = Icons.Default.BookmarkAdded),
            ListSetting.Value(false, "False :-( ".repeat(20), icon = Icons.Default.BookmarkRemove),
        )
    ).ChooserDialog(true, {}, {})
}

class StringSetting(
    private val title: String,
    private val icon: ImageVector? = null,
    private val description: String? = null,
    private val descriptionWhenEmpty: String? = null,
) : SettingWithValue<String> {
    @Composable
    override fun Render(value: String, onValueChange: (String) -> Unit) {
        var dialogOpen by rememberSaveable { mutableStateOf(false) }

        SettingsItem(
            modifier = Modifier.clickable { dialogOpen = true },
            title = title,
            icon = icon,
            description = if (value.isEmpty()) {
                descriptionWhenEmpty ?: description
            } else if (description == null) {
                value
            } else {
                stringResource(
                    R.string.settings_description_with_value,
                    description,
                    value
                )
            },
        )

        if (dialogOpen) {
            EditDialog(
                initialValue = value,
                onValueChange = onValueChange,
                onDismissRequest = { dialogOpen = false }
            )
        }
    }

    @Composable
    fun EditDialog(
        initialValue: String,
        onValueChange: (String) -> Unit,
        onDismissRequest: () -> Unit,
    ) {
        // only send value changes when the user presses ok
        var value by rememberSaveable { mutableStateOf(initialValue) }

        Dialog(onDismissRequest = onDismissRequest) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                )

                Box(
                    contentAlignment = Alignment.TopCenter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val focusRequester = remember { FocusRequester() }
                    TextField(
                        value = value,
                        onValueChange = { value = it },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                    LaunchedEffect(null) {
                        coroutineContext.job.invokeOnCompletion {
                            focusRequester.requestFocus()
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Spacer(modifier = Modifier.weight(1.0f))
                    TextButton(
                        onClick = {
                            // only send value changes when the user presses ok
                            onValueChange(value)
                            onDismissRequest()
                        }
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun ListSettingEditDialogPreview() {
    StringSetting(
        title = "A string setting",
        descriptionWhenEmpty = LoremIpsum(20).values.first()
    ).EditDialog("Initial value", {}, {})
}
