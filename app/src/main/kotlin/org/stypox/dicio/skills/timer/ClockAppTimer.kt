package org.stypox.dicio.skills.timer

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.provider.AlarmClock
import android.util.Log
import java.time.Duration

/**
 * Sets timers in the system Clock app through the standard [AlarmClock] intents.
 *
 * Other apps can neither query nor cancel the timers of the Clock app: [AlarmClock.ACTION_DISMISS_TIMER]
 * without a timer URI only stops the timers that are ringing, and the Clock app does not tell the
 * URIs of the timers it creates. So the timers set from Dicio are remembered here (until Dicio is
 * closed), to answer how much time is left and to know whether a timer is ringing.
 */
internal object ClockAppTimer {
    class Remembered(val name: String?, val endElapsedRealtime: Long) {
        val remainingMillis: Long get() = endElapsedRealtime - SystemClock.elapsedRealtime()
    }

    /** Timers set in the Clock app from Dicio, in the order they were set */
    val timers: MutableList<Remembered> = ArrayList()

    private val TAG = ClockAppTimer::class.simpleName

    /** [AlarmClock.EXTRA_LENGTH] must be between 1 second and 24 hours */
    private const val MAX_LENGTH_SECONDS = 24 * 60 * 60L

    fun canSet(context: Context, duration: Duration): Boolean {
        return duration.seconds in 1..MAX_LENGTH_SECONDS &&
                Intent(AlarmClock.ACTION_SET_TIMER).resolveActivity(context.packageManager) != null
    }

    /** @return whether the Clock app accepted the timer */
    fun set(context: Context, duration: Duration, name: String?): Boolean {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, duration.seconds.toInt())
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (name != null) {
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, name)
        }
        if (!start(context, intent)) {
            return false
        }
        timers.add(Remembered(name, SystemClock.elapsedRealtime() + duration.toMillis()))
        return true
    }

    /** Timers that should be ringing now, since their time is up */
    fun expiredTimers(): List<Remembered> = timers.filter { it.remainingMillis <= 0 }

    /** Timers that are still running */
    fun runningTimers(): List<Remembered> = timers.filter { it.remainingMillis > 0 }

    /**
     * Stops the timers of the Clock app that are ringing.
     * @return whether the Clock app accepted the request
     */
    fun dismissExpired(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            return false
        }
        val dismissed = start(
            context,
            Intent(AlarmClock.ACTION_DISMISS_TIMER)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        if (dismissed) {
            timers.removeAll { it.remainingMillis <= 0 }
        }
        return dismissed
    }

    /** @return whether the Clock app's list of timers could be opened */
    fun showTimers(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return false
        }
        return start(
            context,
            Intent(AlarmClock.ACTION_SHOW_TIMERS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun start(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "No Clock app handles ${intent.action}", e)
            false
        } catch (e: SecurityException) {
            Log.w(TAG, "Not allowed to send ${intent.action}", e)
            false
        }
    }
}
