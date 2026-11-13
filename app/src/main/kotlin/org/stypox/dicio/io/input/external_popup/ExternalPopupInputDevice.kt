package org.stypox.dicio.io.input.external_popup

import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.result.ActivityResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.stypox.dicio.R
import org.stypox.dicio.di.ActivityForResultManager
import org.stypox.dicio.di.LocaleManager
import org.stypox.dicio.io.input.InputEvent
import org.stypox.dicio.io.input.SttInputDevice
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.util.distinctUntilChangedBlockingFirst
import java.util.Locale


class ExternalPopupInputDevice(
    @param:ApplicationContext val context: Context,
    private val activityForResultManager: ActivityForResultManager,
    localeManager: LocaleManager,
) : SttInputDevice {

    private var locale: Locale
    // unfortunately some apps like "speech recognition and synthesis from google" require
    // the country to also be specified in the locale, hence this hack (hopefully other apps handle
    // it well)
    private val localeWithCountry: Locale
        get() = if (locale.country.isEmpty()) {
            Locale.getAvailableLocales()
                .firstOrNull { it.language == locale.language && it.country.isNotEmpty() }
                ?: locale
        } else {
            locale
        }

    private val _state: MutableStateFlow<ExternalPopupState>
    private val _uiState: MutableStateFlow<SttState>
    override val uiState: StateFlow<SttState>

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        // Run blocking, because the locale is always available right away since LocaleManager also
        // initializes in a blocking way.
        val (firstLocale, nextLocaleFlow) = localeManager.locale
            .distinctUntilChangedBlockingFirst()
        locale = firstLocale

        val initialState = stateFromResolveActivity()
        _state = MutableStateFlow(initialState)
        _uiState = MutableStateFlow(initialState.toUiState())
        uiState = _uiState

        scope.launch {
            _state.collect { _uiState.value = it.toUiState() }
        }

        scope.launch {
            // perform initialization again every time the locale changes
            nextLocaleFlow.collect { newLocale ->
                locale = newLocale
                _state.emit(stateFromResolveActivity())
            }
        }
    }

    override fun tryLoad(thenStartListeningEventListener: ((InputEvent) -> Unit)?): Boolean {
        if (thenStartListeningEventListener != null) {
            return startListening(thenStartListeningEventListener)
        }
        return when (_state.value) {
            ExternalPopupState.NotAvailable, is ExternalPopupState.ErrorStartingActivity -> false
            else -> true
        }
    }

    override fun stopListening() {
        // no way to implement
    }

    override fun onClick(eventListener: (InputEvent) -> Unit) {
        startListening(eventListener)
    }

    override suspend fun destroy() {
        scope.cancel()
    }

    private fun getIntent(): Intent {
        // Unfortunately the user could choose Dicio itself (starting SttPopupActivity), but there
        // is no way to avoid this unless we use an Intent.createChooser() with
        // `EXTRA_EXCLUDE_COMPONENTS`. A chooser, however, wouldn't allow the user to press
        // "Always"/"Just once", and also wouldn't make it possible to check for availability like
        // with .resolveIntent() (since the intent always resolves to the choooser).
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeWithCountry.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.stt_say_something))
        }
    }

    private fun stateFromResolveActivity(): ExternalPopupState {
        return if (getIntent().resolveActivity(context.packageManager) == null) {
            ExternalPopupState.NotAvailable
        } else {
            ExternalPopupState.Available
        }
    }

    private fun startListening(eventListener: (InputEvent) -> Unit): Boolean {
        if (_state.compareAndSet(
                ExternalPopupState.Available,
                ExternalPopupState.WaitingForResult(eventListener)
        ) || _state.compareAndSet(
                ExternalPopupState.ErrorStartingActivity(Throwable()),
                ExternalPopupState.WaitingForResult(eventListener)
        ) || _state.compareAndSet(
                ExternalPopupState.ErrorActivityResult(0),
                ExternalPopupState.WaitingForResult(eventListener)
        )) {
            try {
                if (!activityForResultManager.launch(getIntent(), this::onActivityResult)) {
                    // There is no activity to launch the popup from (e.g. the wake word was
                    // detected while no activity was open), so go back to being available, so
                    // that an activity can start listening later.
                    _state.compareAndSet(
                        ExternalPopupState.WaitingForResult { },
                        ExternalPopupState.Available
                    )
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Could not start STT activity", e)
                _state.compareAndSet(
                    ExternalPopupState.WaitingForResult { },
                    ExternalPopupState.ErrorStartingActivity(e)
                )
            }
        }
        return false
    }

    private fun onActivityResult(result: ActivityResult) {
        // all activity requesters are used just once since the activity might change
        val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
        val confidences = result.data?.getFloatArrayExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES)
        val eventListener = _state.value as? ExternalPopupState.WaitingForResult ?: return

        if (result.resultCode == RESULT_OK && !results.isNullOrEmpty()) {
            // this is not atomic but there is no alternative in Kotlin to compare, update and get
            // the previous value
            _state.compareAndSet(
                ExternalPopupState.WaitingForResult { },
                ExternalPopupState.Available
            )

            if (results.size == confidences?.size) {
                eventListener.listener(InputEvent.Final(results.zip(confidences.map { it })))
            } else {
                eventListener.listener(InputEvent.Final(results.map { Pair(it, 1.0f) }))
            }

        } else if (result.resultCode == RESULT_CANCELED) {
            _state.compareAndSet(
                ExternalPopupState.WaitingForResult { },
                ExternalPopupState.Available
            )

            eventListener.listener(InputEvent.None)

        } else {
            _state.compareAndSet(
                ExternalPopupState.WaitingForResult { },
                ExternalPopupState.ErrorActivityResult(result.resultCode)
            )

            eventListener.listener(InputEvent.Error(ResultCodeException(result.resultCode)))
        }
    }

    companion object {
        val TAG = ExternalPopupInputDevice::class.simpleName
    }
}