package org.stypox.dicio.util

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.stypox.dicio.di.ActivityForResultManager
import org.stypox.dicio.di.LocaleManager
import org.stypox.dicio.di.LocaleManagerModule
import org.stypox.dicio.settings.datastore.UserSettings
import org.stypox.dicio.ui.theme.AppTheme
import java.util.Locale
import javax.inject.Inject

abstract class BaseActivity : ComponentActivity() {

    @Inject
    lateinit var activityForResultManager: ActivityForResultManager
    // this launcher is kept here just to keep a reference to it, since ActivityForResultManager
    // only holds a WeakReference
    private lateinit var launcher: ActivityResultLauncher<Intent>

    @Inject
    lateinit var dataStore: DataStore<UserSettings>

    /**
     * Sets the locale according to value calculated by the injected [LocaleManager].
     */
    private fun setLocale(locale: Locale) {
        Locale.setDefault(locale)
        for (resources in sequenceOf(resources, applicationContext.resources)) {
            val configuration = resources.configuration
            configuration.setLocale(locale)
            @Suppress("DEPRECATION") // there is no other way to do this
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        if (Build.VERSION.SDK_INT >= 29) {
            // also draw under the system navigation bar: https://stackoverflow.com/a/78237081
            window.isNavigationBarContrastEnforced = false
        }

        // can't use @Inject because Hilt initializes only when super.onCreate() is called
        val localeManager =
            EntryPointAccessors.fromApplication(this, LocaleManagerModule::class.java)
        val (firstLocale, nextLocaleFlow) = localeManager.getLocaleManager().locale
            .distinctUntilChangedBlockingFirst()
        setLocale(firstLocale)
        lifecycleScope.launch {
            nextLocaleFlow.collect {
                recreate()
            }
        }

        super.onCreate(savedInstanceState)

        // this will only hold a weak reference, so no need to remove it afterwards
        launcher = activityForResultManager.addLauncher(this)
    }

    /**
     * Calls [setContent] with the provided [content], but it also listens for changes in
     * [LocaleManager.locale] and themes and forces recompositions in case of changes.
     *
     * `LocalContext provides createConfigurationContext()` can't be used because then the
     * `LocalContext.current` wouldn't be a [ComponentActivity] anymore but just a plain `Context`,
     * causing things relying on activities to fail (e.g. `registerLauncherForActivityResult` or
     * https://slack-chats.kotlinlang.org/t/511933/if-i-handle-config-changes-manually-no-activity-recreation-a#30b520aa-70a5-40e9-b49c-475d88fa72f4 )
     */
    fun composeSetContent(content: @Composable () -> Unit) {
        setContent {
            val theme = remember {
                dataStore.data
                    .map { Pair(it.theme, it.dynamicColors) }
            }
                .collectAsState(
                    // run blocking, because we can't start the app if we don't know the theme
                    initial = runBlocking {
                        val data = dataStore.data.first()
                        Pair(data.theme, data.dynamicColors)
                    }
                )

            AppTheme(
                theme = theme.value.first,
                dynamicColors = theme.value.second,
                content = content,
            )
        }
    }
}
