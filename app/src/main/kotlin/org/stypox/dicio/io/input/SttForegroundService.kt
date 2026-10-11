package org.stypox.dicio.io.input

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.stypox.dicio.R
import org.stypox.dicio.di.SttInputDeviceWrapper
import javax.inject.Inject

/**
 * Foreground service of type microphone that is kept alive only while the STT is listening after
 * an assist intent (e.g. a Bluetooth headset button). Without it, Android silences the microphone
 * as soon as the activity is not visible anymore (e.g. behind the lock screen, or when the screen
 * turns off while listening), and the STT would only receive silence (upstream issue #154).
 *
 * Must be started while the app is in the foreground (see [start]), otherwise Android 14+ refuses
 * to start a foreground service of type microphone.
 */
@AndroidEntryPoint
class SttForegroundService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var observeJob: Job? = null

    @Inject
    lateinit var sttInputDevice: SttInputDeviceWrapper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForegroundWithNotification()
        } catch (e: Exception) {
            // e.g. SecurityException if RECORD_AUDIO is missing or the app is not in the foreground
            Log.e(TAG, "Could not start microphone foreground service", e)
            stopSelf()
            return START_NOT_STICKY
        }

        observeJob?.cancel()
        observeJob = scope.launch {
            val startedListening = withTimeoutOrNull(START_LISTENING_TIMEOUT_MILLIS) {
                sttInputDevice.uiState.first { it == SttState.Listening }
            } != null
            if (startedListening) {
                withTimeoutOrNull(MAX_LISTENING_MILLIS) {
                    sttInputDevice.uiState.first { it != SttState.Listening }
                }
            } else {
                Log.w(TAG, "STT did not start listening within $START_LISTENING_TIMEOUT_MILLIS ms")
            }
            Log.d(TAG, "Listening finished, stopping microphone foreground service")
            ServiceCompat.stopForeground(this@SttForegroundService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startForegroundWithNotification() {
        val notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java)!!
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.stt_foreground_service_label),
                NotificationManager.IMPORTANCE_LOW,
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hearing_white)
            .setContentTitle(getString(R.string.stt_foreground_service_notification))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            else
                0,
        )
    }

    companion object {
        private val TAG = SttForegroundService::class.simpleName
        private const val NOTIFICATION_CHANNEL_ID =
            "org.stypox.dicio.io.input.SttForegroundService.FOREGROUND"
        private const val NOTIFICATION_ID = 73920145
        private const val START_LISTENING_TIMEOUT_MILLIS = 15_000L
        private const val MAX_LISTENING_MILLIS = 60_000L

        /**
         * Call this only while an activity of the app is resumed.
         */
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(
                    context, Intent(context, SttForegroundService::class.java)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Could not start SttForegroundService", e)
            }
        }
    }
}
