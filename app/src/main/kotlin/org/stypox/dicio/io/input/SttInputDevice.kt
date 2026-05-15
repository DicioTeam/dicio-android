package org.stypox.dicio.io.input

import kotlinx.coroutines.flow.StateFlow

interface SttInputDevice {
    val uiState: StateFlow<SttState>

    fun tryLoad(thenStartListeningEventListener: ((InputEvent) -> Unit)?): Boolean

    fun stopListening()

    fun onClick(eventListener: (InputEvent) -> Unit)

    suspend fun destroy()

    companion object {
        const val DEFAULT_STT_SILENCE_DURATION = 2
    }
}
