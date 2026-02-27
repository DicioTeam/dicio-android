package org.stypox.dicio.settings.ui

import android.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import androidx.compose.ui.unit.dp

@Composable
fun SettingsItem(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    description: String? = null,
    content: (@Composable () -> Unit)? = null,
    innerContent: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                // no content description here as it would be duplicate with the text below
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(24.dp))
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.0f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                fontWeight = FontWeight.Medium,
            )
            if (description != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (innerContent != null) {
                Spacer(modifier = Modifier.height(3.dp))
                innerContent()
            }
        }
        if (content != null) {
            Spacer(modifier = Modifier.width(16.dp))
            content()
        }
    }
}

@Composable
@Preview(showBackground = true, backgroundColor = Color.WHITE.toLong())
private fun SettingsItemPreview() {
    val loremIpsumShort = { LoremIpsum(4).values.first() }
    val loremIpsumLong = { LoremIpsum(9).values.first() }

    Column {
        SettingsItem(
            title = "Test",
            modifier = Modifier.clickable {  },
        )
        SettingsItem(
            title = "Input and output methods",
            icon = Icons.Default.QuestionAnswer,
        )
        SettingsItem(
            title = loremIpsumLong(),
            description = "A nice description",
        )
        SettingsItem(
            title = loremIpsumShort(),
            icon = Icons.Default.QuestionAnswer,
            description = loremIpsumLong(),
            modifier = Modifier.clickable {  },
        )
        var checked1 by remember { mutableStateOf(false) }
        SettingsItem(
            title = loremIpsumLong(),
            content = {
                Switch(checked = checked1, onCheckedChange = { checked1 = it })
            }
        )
        var checked2 by remember { mutableStateOf(true) }
        SettingsItem(
            title = loremIpsumShort(),
            icon = Icons.Default.QuestionAnswer,
            modifier = Modifier.clickable { checked2 = !checked2 },
            content = {
                Switch(checked = checked2, onCheckedChange = { checked2 = it })
            }
        )
        var checked3 by remember { mutableStateOf(true) }
        SettingsItem(
            title = "Test",
            description = loremIpsumShort(),
            modifier = Modifier.clickable { checked3 = !checked3 },
            content = {
                Checkbox(checked = checked3, onCheckedChange = { checked3 = it })
            }
        )
        var checked4 by remember { mutableStateOf(false) }
        SettingsItem(
            title = "Input and output methods",
            icon = Icons.Default.QuestionAnswer,
            description = "A nice long nice long nice long nice long nice long nice long nice long description",
            content = {
                Checkbox(checked = checked4, onCheckedChange = { checked4 = it })
            }
        )
    }
}
