package org.stypox.dicio.io.assistant

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.ActivityOptionsCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.AndroidEntryPoint
import org.stypox.dicio.R
import org.stypox.dicio.di.SkillContextInternal
import org.stypox.dicio.di.SttInputDeviceWrapper
import org.stypox.dicio.eval.SkillEvaluator
import javax.inject.Inject

@AndroidEntryPoint
class AssistantOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner, ActivityResultRegistryOwner {

    @Inject
    lateinit var skillEvaluator: SkillEvaluator
    @Inject
    lateinit var sttInputDevice: SttInputDeviceWrapper
    @Inject
    lateinit var skillContext: SkillContextInternal

    private lateinit var windowManager: WindowManager
    private var overlayView: ComposeView? = null
    private lateinit var powerManager: PowerManager

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    // Screen state monitoring
    private val handler = Handler(Looper.getMainLooper())
    private var screenOffTimeoutRunnable: Runnable? = null
    private var screenReceiver: BroadcastReceiver? = null
    
    companion object {
        private val TAG = AssistantOverlayService::class.simpleName
        private const val NOTIFICATION_CHANNEL_ID = "org.stypox.dicio.io.assistant.OVERLAY"
        private const val NOTIFICATION_ID = 87654321
        private const val ACTION_SHOW_OVERLAY = "org.stypox.dicio.io.assistant.SHOW_OVERLAY"
        private const val ACTION_HIDE_OVERLAY = "org.stypox.dicio.io.assistant.HIDE_OVERLAY"
        private const val SCREEN_OFF_TIMEOUT_MS = 60_000L // 1 minute
        
        fun start(context: Context) {
            val intent = Intent(context, AssistantOverlayService::class.java)
            intent.action = ACTION_SHOW_OVERLAY
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, AssistantOverlayService::class.java)
            intent.action = ACTION_HIDE_OVERLAY
            context.startService(intent)
        }
    }
    
    private val customActivityResultRegistry = object : ActivityResultRegistry() {
        override fun <I : Any?, O : Any?> onLaunch(
            requestCode: Int,
            contract: ActivityResultContract<I, O>,
            input: I,
            options: ActivityOptionsCompat?
        ) {
            val intent = contract.createIntent(this@AssistantOverlayService, input)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry
    
    override val activityResultRegistry: ActivityResultRegistry
        get() = customActivityResultRegistry

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager

        createForegroundNotification()
        registerScreenStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> {
                if (checkOverlayPermission()) {
                    showOverlay()
                } else {
                    Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
                    requestOverlayPermission()
                    stopSelf()
                }
            }
            ACTION_HIDE_OVERLAY -> {
                hideOverlay()
                stopSelf()
            }
            else -> {
                if (checkOverlayPermission()) {
                    showOverlay()
                } else {
                    Toast.makeText(this, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
                    requestOverlayPermission()
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        hideOverlay()
        unregisterScreenStateReceiver()
        cancelScreenOffTimeout()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDestroy()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun showOverlay() {
        if (overlayView != null) {
            return
        }

        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        params.y = 100

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@AssistantOverlayService)
            setViewTreeViewModelStoreOwner(this@AssistantOverlayService)
            setViewTreeSavedStateRegistryOwner(this@AssistantOverlayService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                CompositionLocalProvider(
                    androidx.activity.compose.LocalActivityResultRegistryOwner provides this@AssistantOverlayService
                ) {
                    AssistantOverlayContent(
                        onDismiss = {
                            hideOverlay()
                            stopSelf()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(overlayView, params)
            
            // Start listening immediately
            sttInputDevice.tryLoad(skillEvaluator::processInputEvent)
            checkScreenStateAndScheduleTimeout()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to add overlay view", e)
            Toast.makeText(this, "Failed to show assistant overlay", Toast.LENGTH_SHORT).show()
            stopSelf()
        }
    }

    private fun hideOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove overlay view", e)
            }
            overlayView = null
        }
        
        // Stop listening when overlay is hidden
        sttInputDevice.stopListening()
        cancelScreenOffTimeout()
    }
    
    private fun registerScreenStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Screen turned off, scheduling timeout")
                        scheduleScreenOffTimeout()
                    }
                    Intent.ACTION_SCREEN_ON -> {
                        Log.d(TAG, "Screen turned on, canceling timeout")
                        cancelScreenOffTimeout()
                    }
                }
            }
        }
        
        registerReceiver(screenReceiver, filter)
    }
    
    private fun unregisterScreenStateReceiver() {
        screenReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unregister screen receiver", e)
            }
            screenReceiver = null
        }
    }
    
    private fun checkScreenStateAndScheduleTimeout() {
        if (!isScreenOn()) {
            Log.d(TAG, "Overlay shown with screen off, scheduling timeout")
            scheduleScreenOffTimeout()
        }
    }
    
    private fun isScreenOn(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            powerManager.isInteractive
        } else {
            @Suppress("DEPRECATION")
            powerManager.isScreenOn
        }
    }
    
    private fun scheduleScreenOffTimeout() {
        cancelScreenOffTimeout()
        
        // Only schedule if overlay is visible
        if (overlayView == null) {
            return
        }
        
        screenOffTimeoutRunnable = Runnable {
            Log.d(TAG, "Screen off timeout reached, dismissing overlay")
            hideOverlay()
            stopSelf()
        }
        
        handler.postDelayed(screenOffTimeoutRunnable!!, SCREEN_OFF_TIMEOUT_MS)
        Log.d(TAG, "Scheduled screen off timeout for ${SCREEN_OFF_TIMEOUT_MS}ms")
    }
    
    private fun cancelScreenOffTimeout() {
        screenOffTimeoutRunnable?.let {
            handler.removeCallbacks(it)
            screenOffTimeoutRunnable = null
            Log.d(TAG, "Canceled screen off timeout")
        }
    }

    @Composable
    private fun AssistantOverlayContent(onDismiss: () -> Unit) {
        val interactionLog = skillEvaluator.state.collectAsState()
        val sttState = sttInputDevice.uiState.collectAsState()

        AssistantOverlay(
            skillContext = skillContext,
            interactionLog = interactionLog.value,
            sttState = sttState.value,
            onSttClick = {
                sttInputDevice.onClick(skillEvaluator::processInputEvent)
            },
            onDismiss = onDismiss
        )
    }

    private fun createForegroundNotification() {
        val notificationManager = getSystemService(this, NotificationManager::class.java)!!

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.assistant_overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = getString(R.string.assistant_overlay_notification_description)
            notificationManager.createNotificationChannel(channel)
        }

        val dismissIntent = Intent(this, AssistantOverlayService::class.java).apply {
            action = ACTION_HIDE_OVERLAY
        }
        val dismissPendingIntent = PendingIntent.getService(
            this,
            0,
            dismissIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hearing_white)
            .setContentTitle(getString(R.string.assistant_overlay_active))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_stop_circle_white,
                    getString(R.string.dismiss),
                    dismissPendingIntent
                )
            )
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }
}
