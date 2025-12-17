package org.stypox.dicio

import android.Manifest
import android.content.Intent
import android.content.Intent.ACTION_ASSIST
import android.content.Intent.ACTION_VOICE_COMMAND
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import dev.shreyaspatil.permissionFlow.PermissionFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.stypox.dicio.di.SttInputDeviceWrapper
import org.stypox.dicio.di.WakeDeviceWrapper
import org.stypox.dicio.eval.SkillEvaluator
import org.stypox.dicio.io.audio.AudioFocusManager
import org.stypox.dicio.io.wake.WakeService
import org.stypox.dicio.io.wake.WakeState.Loaded
import org.stypox.dicio.io.wake.WakeState.Loading
import org.stypox.dicio.io.wake.WakeState.NotLoaded
import org.stypox.dicio.ui.home.wakeWordPermissions
import org.stypox.dicio.ui.nav.Navigation
import org.stypox.dicio.util.BaseActivity
import java.time.Instant
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : BaseActivity() {

    @Inject
    lateinit var skillEvaluator: SkillEvaluator
    @Inject
    lateinit var sttInputDevice: SttInputDeviceWrapper
    @Inject
    lateinit var wakeDevice: WakeDeviceWrapper
    @Inject
    lateinit var audioFocusManager: AudioFocusManager

    private var sttPermissionJob: Job? = null
    private var wakeServiceJob: Job? = null

    private var nextAssistAllowed = Instant.MIN

    /**
     * Automatically loads the LLM and the STT when the [ACTION_ASSIST] intent is received. Applies
     * a backoff of [INTENT_BACKOFF_MILLIS], since during testing Android would send the assist
     * intent to the app twice in a row.
     */
    private fun onAssistIntentReceived() {
        val now = Instant.now()
        if (nextAssistAllowed < now) {
            nextAssistAllowed = now.plusMillis(INTENT_BACKOFF_MILLIS)
            Log.d(TAG, "Received assist intent")
            sttInputDevice.tryLoad(skillEvaluator::processInputEvent)
        } else {
            Log.w(TAG, "Ignoring duplicate assist intent")
        }
    }

    private fun handleWakeWordTurnOnScreen(intent: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1 &&
            intent?.action == ACTION_WAKE_WORD
        ) {
            // Dicio was started anew based on a wake word,
            // turn on the screen to let the user see what is happening
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        // the wake word triggered notification is not needed anymore
        WakeService.cancelTriggeredNotification(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleWakeWordTurnOnScreen(intent)
        if (isAssistIntent(intent)) {
            onAssistIntentReceived()
        }
    }

    override fun onStart() {
        isInForeground += 1
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
        isInForeground -= 1

        // Release audio focus when the app goes to background
        audioFocusManager.releaseFocus()

        // once the activity is swiped away from the lock screen (or put in the background in any
        // other way), we don't want to show it on the lock screen anymore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(false)
            setTurnScreenOn(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCreated += 1

        handleWakeWordTurnOnScreen(intent)
        if (isAssistIntent(intent)) {
            onAssistIntentReceived()
        } else if (intent.action != ACTION_WAKE_WORD) {
            // load the input device, without starting to listen
            sttInputDevice.tryLoad(null)
        }

        WakeService.start(this)
        wakeServiceJob?.cancel()
        wakeServiceJob = lifecycleScope.launch {
            wakeDevice.state
                .map { it == NotLoaded || it == Loading || it == Loaded }
                .combine(
                    PermissionFlow.getInstance().getMultiplePermissionState(*wakeWordPermissions)
                ) { wakeState, permGranted ->
                    wakeState && permGranted.allGranted
                }
                // avoid restarting the service if the state changes but the resulting value
                // in the flow remains true (which happens when the user stops the WakeService from
                // the notification, which releases resources and makes the WakeDevice go from
                // Loaded to NotLoaded)
                .distinctUntilChanged()
                .filter { it }
                .collect { WakeService.start(this@MainActivity) }
        }

        sttPermissionJob?.cancel()
        sttPermissionJob = lifecycleScope.launch {
            // if the STT failed to load because of the missing permission, this will try again
            PermissionFlow.getInstance().getPermissionState(Manifest.permission.RECORD_AUDIO)
                .drop(1)
                .filter { it.isGranted }
                .collect { sttInputDevice.tryLoad(null) }
        }

        composeSetContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier.safeDrawingPadding()
                ) {
                    Navigation()
                }
            }
        }
    }

    override fun onDestroy() {
        // the wake word service remains active in the background,
        // so we need to release resources that it does not need manually
        sttInputDevice.reinitializeToReleaseResources()
        isCreated -= 1
        super.onDestroy()
    }

    companion object {
        private const val INTENT_BACKOFF_MILLIS = 100L
        private val TAG = MainActivity::class.simpleName
        const val ACTION_WAKE_WORD = "org.stypox.dicio.MainActivity.ACTION_WAKE_WORD"

        var isInForeground: Int = 0
            private set
        var isCreated: Int = 0
            private set

        private fun isAssistIntent(intent: Intent?): Boolean {
            return when (intent?.action) {
                ACTION_ASSIST, ACTION_VOICE_COMMAND -> true
                else -> false
            }
        }
    }
}
