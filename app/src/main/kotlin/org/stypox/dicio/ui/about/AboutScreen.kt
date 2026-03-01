@file:OptIn(ExperimentalMaterial3Api::class)

package org.stypox.dicio.ui.about

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.stypox.dicio.BuildConfig
import org.stypox.dicio.R
import org.stypox.dicio.error.ErrorActivity
import org.stypox.dicio.settings.ui.SettingsItem
import org.stypox.dicio.ui.theme.AppTheme
import org.stypox.dicio.util.ShareUtils

@Composable
fun AboutScreen(
    navigationIcon: @Composable () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about)) },
                navigationIcon = navigationIcon,
            )
        },
    ) { paddingValues ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 4.dp),
            modifier = Modifier.padding(paddingValues)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        imageVector = DicioSquircleIcon,
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    val versionTextSizeUnit = MaterialTheme.typography.titleMedium.fontSize.value.dp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(versionTextSizeUnit * 0.25f),
                        modifier = Modifier.clickable(
                            onClickLabel = stringResource(R.string.about_version_copy)
                        ) { ShareUtils.copyToClipboard(context, getVersionInfoString()) }
                    ) {
                        Text(
                            text = stringResource(
                                R.string.about_version,
                                BuildConfig.VERSION_NAME,
                                BuildConfig.VERSION_CODE
                            ),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Light),
                            textAlign = TextAlign.Center
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(versionTextSizeUnit * 0.8f)
                        )
                    }
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.about_description),
                        textAlign = TextAlign.Center
                    )
                }
            }

            item {
                AboutItem(
                    title = R.string.about_repository_title,
                    icon = Icons.Default.Code,
                    description = R.string.about_repository_description,
                    link = R.string.about_repository_link
                )
            }

            item {
                AboutItem(
                    title = R.string.about_issues_title,
                    icon = Icons.Default.BugReport,
                    description = R.string.about_issues_description,
                    link = R.string.about_issues_link
                )
            }

            item {
                AboutItem(
                    title = R.string.about_contributing_title,
                    icon = Icons.Default.Group,
                    description = R.string.about_contributing_description,
                    link = R.string.about_contributing_link
                )
            }

            item {
                AboutItem(
                    title = R.string.about_privacy_title,
                    icon = Icons.Default.Policy,
                    description = R.string.about_privacy_description,
                    link = R.string.about_privacy_link
                )
            }
        }
    }
}

@Composable
fun AboutItem(
    @StringRes title: Int,
    icon: ImageVector,
    @StringRes description: Int,
    @StringRes link: Int
) {
    val context = LocalContext.current
    SettingsItem(
        title = stringResource(title),
        icon = icon,
        description = stringResource(description),
        modifier = Modifier.clickable {
            ShareUtils.openUrlInBrowser(context, context.getString(link))
        },
    )
}

private fun getVersionInfoString(): String {
    return "${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME} (" +
            "${BuildConfig.VERSION_CODE}) running on ${ErrorActivity.getOsInfo()}"
}

@Preview
@Composable
private fun AboutScreenPreview() {
    AppTheme {
        AboutScreen(
            navigationIcon = {},
        )
    }
}
