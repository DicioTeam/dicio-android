package org.stypox.dicio.io.input.scribe

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import org.stypox.dicio.di.LocaleManager
import org.stypox.dicio.io.input.InputEvent
import org.stypox.dicio.io.input.SttInputDevice
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.util.distinctUntilChangedBlockingFirst
import java.util.Locale

class ScribeRealtimeInputDevice(
    private val okHttpClient: OkHttpClient,
    localeManager: LocaleManager,
    private val apiKey: StateFlow<String>,
    private val silencesBeforeStop: StateFlow<Int>,
) : SttInputDevice {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _uiState: MutableStateFlow<SttState>
    override val uiState: StateFlow<SttState>

    @Volatile
    private var currentLanguageCode: String
    @Volatile
    private var currentApiKey: String

    @Volatile
    private var webSocket: WebSocket? = null
    @Volatile
    private var audioRecord: AudioRecord? = null
    @Volatile
    private var shouldRecordAudio = false

    private var activeEventListener: ((InputEvent) -> Unit)? = null
    private var audioStreamingJob: Job? = null

    init {
        val (firstLocale, nextLocaleFlow) = localeManager.locale
            .distinctUntilChangedBlockingFirst()
        currentLanguageCode = languageCodeFromLocale(firstLocale)
        currentApiKey = apiKey.value.trim()

        val initialState = readyStateFromApiKey(currentApiKey)
        _uiState = MutableStateFlow(initialState)
        uiState = _uiState

        scope.launch {
            nextLocaleFlow.collect { locale ->
                currentLanguageCode = languageCodeFromLocale(locale)
            }
        }

        scope.launch {
            apiKey.collect { key ->
                currentApiKey = key.trim()
                if (_uiState.value != SttState.Listening) {
                    _uiState.value = readyStateFromApiKey(currentApiKey)
                }
            }
        }
    }

    override fun tryLoad(thenStartListeningEventListener: ((InputEvent) -> Unit)?): Boolean {
        if (currentApiKey.isBlank()) {
            _uiState.value = SttState.NotAvailable
            return false
        }
        if (thenStartListeningEventListener == null) {
            _uiState.value = SttState.Loaded
            return true
        }

        startListening(thenStartListeningEventListener)
        return true
    }

    override fun onClick(eventListener: (InputEvent) -> Unit) {
        when (_uiState.value) {
            SttState.Listening -> stopListeningInternal(sendNoneEvent = true)
            else -> {
                if (currentApiKey.isBlank()) {
                    _uiState.value = SttState.NotAvailable
                    return
                }
                startListening(eventListener)
            }
        }
    }

    override fun stopListening() {
        stopListeningInternal(sendNoneEvent = true)
    }

    private fun startListening(eventListener: (InputEvent) -> Unit) {
        if (_uiState.value == SttState.Listening) {
            return
        }

        val apiKey = currentApiKey
        if (apiKey.isBlank()) {
            _uiState.value = SttState.NotAvailable
            return
        }

        activeEventListener = eventListener
        _uiState.value = SttState.Listening
        shouldRecordAudio = true

        try {
            val request = Request.Builder()
                .url(buildRealtimeUrl())
                .addHeader("xi-api-key", apiKey)
                .build()

            webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(TAG, "Scribe realtime websocket opened")
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    handleServerMessage(webSocket, text)
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    Log.w(TAG, "Ignoring unexpected binary message from Scribe realtime websocket")
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "Scribe realtime websocket failure", t)
                    emitErrorAndStop(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "Scribe realtime websocket closed: code=$code reason=$reason")
                    if (_uiState.value == SttState.Listening) {
                        if (code == 1000) {
                            emitNoneAndStop()
                        } else {
                            emitErrorAndStop(Exception("Scribe connection closed: $code $reason"))
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Scribe realtime websocket", e)
            emitErrorAndStop(e)
        }
    }

    private fun handleServerMessage(webSocket: WebSocket, text: String) {
        val message = try {
            JSONObject(text)
        } catch (e: Exception) {
            Log.e(TAG, "Invalid Scribe JSON message: $text", e)
            emitErrorAndStop(e)
            return
        }

        val messageType = message.optString("message_type", message.optString("type", ""))
        when (messageType) {
            "session_started" -> {
                startAudioStreaming(webSocket)
            }
            "partial_transcript" -> {
                val partial = message.optString("text", "")
                if (partial.isNotBlank()) {
                    activeEventListener?.invoke(InputEvent.Partial(partial))
                }
            }
            "committed_transcript",
            "committed_transcript_with_timestamps" -> {
                val transcript = message.optString("text", "")
                if (transcript.isNotBlank()) {
                    val eventListener = activeEventListener
                    stopListeningInternal(sendNoneEvent = false)
                    eventListener?.invoke(InputEvent.Final(listOf(Pair(transcript, 1.0f))))
                }
            }
            in ERROR_MESSAGE_TYPES -> {
                val errorMessage = message.optString("error", message.optString("message", messageType))
                emitErrorAndStop(Exception("Scribe realtime error ($messageType): $errorMessage"))
            }
        }
    }

    private fun startAudioStreaming(webSocket: WebSocket) {
        if (audioStreamingJob?.isActive == true) {
            return
        }

        audioStreamingJob = scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val minBufferSize = AudioRecord.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                    if (minBufferSize <= 0) {
                        throw Exception("Invalid AudioRecord min buffer size: $minBufferSize")
                    }

                    val recordBufferSize = maxOf(minBufferSize, CHUNK_SAMPLES * BYTES_PER_SAMPLE * 4)
                    val recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        recordBufferSize,
                    )
                    audioRecord = recorder

                    if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                        throw Exception("AudioRecord initialization failed")
                    }

                    recorder.startRecording()
                    val shortBuffer = ShortArray(CHUNK_SAMPLES)

                    while (shouldRecordAudio && _uiState.value == SttState.Listening) {
                        val readSize = recorder.read(shortBuffer, 0, shortBuffer.size)
                        if (readSize <= 0) {
                            if (readSize == AudioRecord.ERROR_INVALID_OPERATION ||
                                readSize == AudioRecord.ERROR_BAD_VALUE) {
                                throw Exception("AudioRecord read error: $readSize")
                            }
                            continue
                        }

                        val pcmBytes = toLittleEndianPcm(shortBuffer, readSize)
                        val payload = JSONObject()
                            .put("message_type", "input_audio_chunk")
                            .put("audio_base_64", Base64.encodeToString(pcmBytes, Base64.NO_WRAP))
                            .put("sample_rate", SAMPLE_RATE)

                        if (!webSocket.send(payload.toString())) {
                            throw Exception("Failed to send audio chunk to Scribe realtime")
                        }
                    }
                } catch (e: Exception) {
                    if (_uiState.value == SttState.Listening) {
                        emitErrorAndStop(e)
                    }
                } finally {
                    releaseRecorder()
                }
            }
        }
    }

    private fun toLittleEndianPcm(samples: ShortArray, readSize: Int): ByteArray {
        val bytes = ByteArray(readSize * BYTES_PER_SAMPLE)
        for (i in 0 until readSize) {
            val sample = samples[i].toInt()
            bytes[i * 2] = (sample and 0xFF).toByte()
            bytes[i * 2 + 1] = ((sample ushr 8) and 0xFF).toByte()
        }
        return bytes
    }

    private fun emitErrorAndStop(throwable: Throwable) {
        val eventListener = activeEventListener
        stopListeningInternal(sendNoneEvent = false)
        eventListener?.invoke(InputEvent.Error(throwable))
    }

    private fun emitNoneAndStop() {
        val eventListener = activeEventListener
        stopListeningInternal(sendNoneEvent = false)
        eventListener?.invoke(InputEvent.None)
    }

    @Synchronized
    private fun stopListeningInternal(sendNoneEvent: Boolean) {
        val eventListener = activeEventListener
        val wasListening = _uiState.value == SttState.Listening

        if (wasListening) {
            _uiState.value = readyStateFromApiKey(currentApiKey)
        }

        activeEventListener = null

        shouldRecordAudio = false
        audioStreamingJob?.cancel()
        audioStreamingJob = null

        webSocket?.close(1000, "normal")
        webSocket = null

        releaseRecorder()

        if (wasListening && sendNoneEvent) {
            eventListener?.invoke(InputEvent.None)
        }
    }

    private fun releaseRecorder() {
        try {
            audioRecord?.stop()
        } catch (_: Exception) {
        }
        try {
            audioRecord?.release()
        } catch (_: Exception) {
        }
        audioRecord = null
    }

    private fun buildRealtimeUrl(): String {
        val languageCode = currentLanguageCode.ifBlank { DEFAULT_LANGUAGE_CODE }
        val vadSilenceThresholdSecs = silencesBeforeStop.value
            .toDouble()
            .coerceIn(MIN_VAD_SILENCE_THRESHOLD_SECS, MAX_VAD_SILENCE_THRESHOLD_SECS)

        return Uri.parse(REALTIME_BASE_URL)
            .buildUpon()
            .appendQueryParameter("model_id", MODEL_ID)
            .appendQueryParameter("audio_format", AUDIO_FORMAT)
            .appendQueryParameter("commit_strategy", COMMIT_STRATEGY)
            .appendQueryParameter("vad_silence_threshold_secs", vadSilenceThresholdSecs.toString())
            .appendQueryParameter("language_code", languageCode)
            .build()
            .toString()
    }

    private fun readyStateFromApiKey(apiKey: String): SttState {
        return if (apiKey.isBlank()) {
            SttState.NotAvailable
        } else {
            SttState.Loaded
        }
    }

    private fun languageCodeFromLocale(locale: Locale): String {
        return locale.language.ifBlank { DEFAULT_LANGUAGE_CODE }
    }

    override suspend fun destroy() {
        stopListeningInternal(sendNoneEvent = false)
        scope.cancel()
    }

    companion object {
        private val TAG = ScribeRealtimeInputDevice::class.simpleName

        private const val REALTIME_BASE_URL = "wss://api.elevenlabs.io/v1/speech-to-text/realtime"
        private const val MODEL_ID = "scribe_v2_realtime"
        private const val AUDIO_FORMAT = "pcm_16000"
        private const val COMMIT_STRATEGY = "vad"

        private const val SAMPLE_RATE = 16000
        private const val CHUNK_SAMPLES = 1600 // 100 ms at 16 kHz
        private const val BYTES_PER_SAMPLE = 2

        private const val DEFAULT_LANGUAGE_CODE = "en"
        private const val MIN_VAD_SILENCE_THRESHOLD_SECS = 0.3
        private const val MAX_VAD_SILENCE_THRESHOLD_SECS = 3.0

        private val ERROR_MESSAGE_TYPES = setOf(
            "error",
            "auth_error",
            "quota_exceeded",
            "transcriber_error",
            "input_error",
            "commit_throttled",
            "unaccepted_terms",
            "rate_limited",
            "queue_overflow",
            "resource_exhausted",
            "session_time_limit_exceeded",
            "chunk_size_exceeded",
            "insufficient_audio_activity",
        )
    }
}
