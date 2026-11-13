package org.stypox.dicio.di

import android.app.Activity.RESULT_CANCELED
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityForResultManager @Inject constructor() {
    private val launchers: ArrayList<WeakReference<ActivityResultLauncher<Intent>>> = ArrayList()
    private var callback: ((ActivityResult) -> Unit)? = null
    // the launcher that launched the request whose result is being waited for by [callback]
    private var callbackLauncher: ActivityResultLauncher<Intent>? = null

    fun addLauncher(activity: ComponentActivity): ActivityResultLauncher<Intent> {
        val launcher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleResult
        )

        val launcherRef = WeakReference(launcher)
        synchronized(launchers) {
            launchers.add(launcherRef)
        }

        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                // the launcher of a destroyed activity can't be used anymore
                synchronized(launchers) {
                    launchers.remove(launcherRef)
                }
                if (!activity.isChangingConfigurations) {
                    // the activity is not going to be recreated, so it would never receive the
                    // result of a request it launched, which would otherwise be waited for forever
                    cancelRequestLaunchedBy(launcher)
                }
            }
        })

        return launcher
    }

    fun launch(input: Intent, newCallback: (ActivityResult) -> Unit): Boolean {
        val launcher = synchronized(launchers) {
            launchers.removeAll { it.get() == null }
            // the most recently created activity is the most likely to be in the foreground
            launchers.map { it.get() }.lastOrNull { it != null }
        } ?: return false

        synchronized(this) {
            if (callback == null) {
                callback = newCallback
                callbackLauncher = launcher
            } else {
                throw IllegalArgumentException("An activity for result request is already active")
            }
        }

        try {
            launcher.launch(input)
        } catch (t: Throwable) {
            // the request was not launched, so there is no result to wait for
            synchronized(this) {
                callback = null
                callbackLauncher = null
            }
            throw t
        }
        return true
    }

    private fun handleResult(activityResult: ActivityResult) {
        synchronized(this) {
            callback?.invoke(activityResult)
            callback = null
            callbackLauncher = null
        }
    }

    private fun cancelRequestLaunchedBy(launcher: ActivityResultLauncher<Intent>) {
        synchronized(this) {
            if (callbackLauncher === launcher) {
                handleResult(ActivityResult(RESULT_CANCELED, null))
            }
        }
    }
}
