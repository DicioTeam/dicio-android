/*
 * Taken from /e/OS Assistant
 *
 * Copyright (C) 2024 MURENA SAS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.stypox.dicio.io.input.parakeet

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.stypox.dicio.di.LocaleManager
import org.stypox.dicio.io.input.InputEvent
import org.stypox.dicio.io.input.SttInputDevice
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.io.input.parakeet.ParakeetState.Downloaded
import org.stypox.dicio.io.input.parakeet.ParakeetState.Downloading
import org.stypox.dicio.io.input.parakeet.ParakeetState.ErrorDownloading
import org.stypox.dicio.io.input.parakeet.ParakeetState.ErrorLoading
import org.stypox.dicio.io.input.parakeet.ParakeetState.Listening
import org.stypox.dicio.io.input.parakeet.ParakeetState.Loaded
import org.stypox.dicio.io.input.parakeet.ParakeetState.Loading
import org.stypox.dicio.io.input.parakeet.ParakeetState.NotAvailable
import org.stypox.dicio.io.input.parakeet.ParakeetState.NotDownloaded
import org.stypox.dicio.io.input.parakeet.ParakeetState.NotInitialized
import org.stypox.dicio.io.input.parakeet.ParakeetState.NotLoaded
import org.stypox.dicio.ui.util.Progress
import org.stypox.dicio.util.FileToDownload
import org.stypox.dicio.util.LocaleUtils
import org.stypox.dicio.util.distinctUntilChangedBlockingFirst
import org.stypox.dicio.util.downloadBinaryFilesWithPartial
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

class ParakeetInputDevice(
    @ApplicationContext appContext: Context,
    private val okHttpClient: OkHttpClient,
    localeManager: LocaleManager,
) : SttInputDevice {

    private val _state: MutableStateFlow<ParakeetState>
    private val _transientUiState = MutableStateFlow<SttState?>(null)
    private val _uiState: MutableStateFlow<SttState>
    override val uiState: StateFlow<SttState>

    private var operationsJob: Job? = null
    private var listeningJob: Job? = null
    @Volatile
    private var activeListener: ParakeetListener? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val filesDir: File = appContext.filesDir
    private val cacheDir: File = appContext.cacheDir

    // Model files on disk
    private val encoderFile: File get() = File(filesDir, "parakeet-encoder.int8.onnx")
    private val decoderJointFile: File get() = File(filesDir, "parakeet-decoder-joint.int8.onnx")
    private val vocabFile: File get() = File(filesDir, "parakeet-vocab.txt")
    private val preprocessorFile: File get() = File(filesDir, "parakeet-nemo128.onnx")

    // URL-check sentinel: contains the base URL that was last downloaded successfully
    private val sameModelUrlCheck: File get() = File(filesDir, "parakeet-model-url")

    init {
        // Run blocking, because the locale is always available right away since LocaleManager also
        // initializes in a blocking way. Moreover, if ParakeetInputDevice were not initialized
        // straight away, the tryLoad() call when MainActivity starts may do nothing.
        val (firstLocale, nextLocaleFlow) = localeManager.locale
            .distinctUntilChangedBlockingFirst()

        val initialState = init(firstLocale)
        _state = MutableStateFlow(initialState)
        _uiState = MutableStateFlow(initialState.toUiState())
        uiState = _uiState

        scope.launch {
            combine(_state, _transientUiState) { state, transientUiState ->
                transientUiState ?: state.toUiState()
            }.collect { _uiState.value = it }
        }

        scope.launch {
            // perform initialization again every time the locale changes
            nextLocaleFlow.collect { reinit(it) }
        }
    }

    private fun init(locale: Locale): ParakeetState {
        // choose the model url based on the locale
        val modelUrl = LocaleUtils.resolveValueForSupportedLocale(locale, MODEL_URLS)

        // the model url may change if the user changes app language, or in case of model updates
        val modelUrlChanged = try {
            sameModelUrlCheck.readText() != modelUrl
        } catch (_: IOException) {
            // modelUrlCheck file does not exist
            true
        }

        return when {
            // if the modelUrl is null, then the current locale is not supported by any Parakeet
            // model
            modelUrl == null -> NotAvailable
            // if the model url changed, the model needs to be re-downloaded
            modelUrlChanged -> NotDownloaded(modelUrl)
            // if all model files exist, the model has been completely downloaded and should be
            // ready to be loaded
            encoderFile.exists() && decoderJointFile.exists()
                && vocabFile.exists() && preprocessorFile.exists() -> NotLoaded
            // if any model file is missing, the model has not been downloaded yet
            else -> NotDownloaded(modelUrl)
        }
    }

    private suspend fun reinit(locale: Locale) {
        // interrupt whatever was happening before
        deinit()

        // reinitialize and emit the new state
        val initialState = init(locale)
        _state.emit(initialState)
    }

    private suspend fun deinit() {
        val prevState = _state.getAndUpdate { NotInitialized }
        when (prevState) {
            // either interrupt the current operation or wait for it to complete
            is Downloading -> {
                operationsJob?.cancel()
                operationsJob?.join()
            }
            is Loading -> {
                operationsJob?.join()
                when (val s = _state.getAndUpdate { NotInitialized }) {
                    NotInitialized -> {} // everything is ok
                    is Loaded -> {
                        s.sessions.close()
                    }
                    is Listening -> {
                        stopActiveListenerAndWait()
                        stopListening(s.sessions, s.eventListener, false)
                        s.sessions.close()
                    }
                    else -> {
                        Log.w(TAG, "Unexpected state after loading: $s")
                    }
                }
            }
            is Loaded -> {
                prevState.sessions.close()
            }
            is Listening -> {
                stopActiveListenerAndWait()
                stopListening(prevState.sessions, prevState.eventListener, false)
                prevState.sessions.close()
            }

            // these states are all resting states, so there is nothing to interrupt
            is NotInitialized,
            is NotAvailable,
            is NotDownloaded,
            is ErrorDownloading,
            is Downloaded,
            is NotLoaded,
            is ErrorLoading -> {}
        }
    }

    private suspend fun stopActiveListenerAndWait() {
        activeListener?.stopAndDiscardCurrentAudio()
        activeListener = null
        listeningJob?.cancel()
        listeningJob?.join()
        listeningJob = null
    }

    /**
     * Loads the model with [thenStartListeningEventListener] if the model is already downloaded
     * but not loaded in RAM (which will then start listening if [thenStartListeningEventListener]
     * is not `null` and pass events there), or starts listening if the model is already ready
     * and [thenStartListeningEventListener] is not `null` and passes events there.
     *
     * @param thenStartListeningEventListener if not `null`, causes the [ParakeetInputDevice] to
     * start listening after it has finished loading, and the received input events are sent there
     * @return `true` if the input device will start listening (or be ready to do so in case
     * `thenStartListeningEventListener == null`) at some point,
     * `false` if manual user intervention is required to start listening
     */
    override fun tryLoad(thenStartListeningEventListener: ((InputEvent) -> Unit)?): Boolean {
        val s = _state.value
        if (s == NotLoaded || s is ErrorLoading) {
            load(thenStartListeningEventListener)
            return true
        } else if (thenStartListeningEventListener != null && s is Loaded) {
            startListening(s.sessions, thenStartListeningEventListener)
            return true
        } else {
            return false
        }
    }

    /**
     * If the model is not being downloaded/loaded, or if there was an error in any of
     * those steps, downloads/loads the model. If the model is already loaded (or is being
     * loaded) toggles listening state.
     *
     * @param eventListener only used if this click causes Parakeet to start listening, will receive
     * all updates for this run
     */
    override fun onClick(eventListener: (InputEvent) -> Unit) {
        // the state can only be changed in the background by the jobs corresponding to Downloading
        // and Loading, but as can be seen below we don't do anything in case of Downloading. For
        // Loading however, special measures are taken in toggleThenStartListening() and in load()
        // to ensure the button click is not lost nor has any unwanted behavior if the state changes
        // right after checking its value in this switch.
        when (val s = _state.value) {
            is NotInitialized -> {} // wait for initialization to happen
            is NotAvailable -> {} // nothing to do
            is NotDownloaded -> download(s.modelUrl)
            is Downloading -> {} // wait for download to finish
            is ErrorDownloading -> download(s.modelUrl) // retry
            is Downloaded -> load(eventListener)
            is NotLoaded -> load(eventListener)
            is Loading -> toggleThenStartListening(eventListener) // wait for loading to finish
            is ErrorLoading -> load(eventListener) // retry
            is Loaded -> startListening(s.sessions, eventListener)
            is Listening -> stopListening(s.sessions, s.eventListener, true)
        }
    }

    /**
     * If the recognizer is currently listening, stops listening. Otherwise does nothing.
     */
    override fun stopListening() {
        when (val s = _state.value) {
            is Listening -> stopListening(s.sessions, s.eventListener, true)
            else -> {}
        }
    }

    /**
     * Downloads all model files (encoder, decoder+joint, preprocessor, vocab). Sets the state to
     * [Downloading], and periodically updates it with downloading progress, until either
     * [ErrorDownloading] or [NotLoaded] are set as state.
     */
    private fun download(modelUrl: String) {
        _state.value = Downloading(Progress.UNKNOWN)

        operationsJob = scope.launch(Dispatchers.IO) {
            try {
                downloadBinaryFilesWithPartial(
                    urlsFiles = listOf(
                        FileToDownload(
                            "$modelUrl/resolve/main/encoder-model.int8.onnx",
                            encoderFile,
                        ),
                        FileToDownload(
                            "$modelUrl/resolve/main/decoder_joint-model.int8.onnx",
                            decoderJointFile,
                        ),
                        FileToDownload(
                            "$modelUrl/resolve/main/nemo128.onnx",
                            preprocessorFile,
                        ),
                        FileToDownload(
                            "$modelUrl/resolve/main/vocab.txt",
                            vocabFile,
                        ),
                    ),
                    httpClient = okHttpClient,
                    cacheDir = cacheDir,
                ) { progress ->
                    _state.value = Downloading(progress)
                }

            } catch (e: IOException) {
                Log.e(TAG, "Can't download Parakeet model", e)
                _state.value = ErrorDownloading(modelUrl, e)
                return@launch
            }

            // Write the base model URL so init() can detect the model is downloaded on
            // next app restart (must match what init() compares against).
            sameModelUrlCheck.writeText(modelUrl)
            _state.value = NotLoaded
        }
    }

    /**
     * Loads the ONNX Runtime sessions for the encoder, decoder+joint, and preprocessor models.
     * Also reads the vocabulary file. Initially sets the state to [Loading] with
     * [Loading.thenStartListening] = ([thenStartListeningEventListener] != `null`), and later
     * either sets the state to [Loaded] or calls [startListening] by checking the current state's
     * [Loading.thenStartListening] (which might have changed in the meantime, if the user clicked
     * on the button while loading).
     */
    private fun load(thenStartListeningEventListener: ((InputEvent) -> Unit)?) {
        _state.value = Loading(thenStartListeningEventListener)

        operationsJob = scope.launch {
            val sessions: ParakeetSessions
            try {
                val env = OrtEnvironment.getEnvironment()
                val sessionOptions = OrtSession.SessionOptions().apply {
                    // Use NNAPI on supported devices for hardware acceleration
                    try {
                        addNnapi()
                    } catch (_: Exception) {
                        Log.d(TAG, "NNAPI not available, using CPU")
                    }
                    setIntraOpNumThreads(
                        Runtime.getRuntime().availableProcessors().coerceAtMost(4)
                    )
                }

                val encoder = env.createSession(
                    encoderFile.absolutePath, sessionOptions
                )
                val decoderJoint = env.createSession(
                    decoderJointFile.absolutePath, sessionOptions
                )
                val preprocessor = env.createSession(
                    preprocessorFile.absolutePath, sessionOptions
                )

                // Read vocab.txt: each line is "token id", where U+2581 represents space
                val vocab = loadVocab(vocabFile)

                sessions = ParakeetSessions(
                    encoder, decoderJoint, preprocessor, vocab, env
                )
            } catch (e: Exception) {
                Log.e(TAG, "Can't load Parakeet model", e)
                _state.value = ErrorLoading(e)
                return@launch
            }

            if (!_state.compareAndSet(Loading(null), Loaded(sessions))) {
                val state = _state.value
                if (state is Loading && state.thenStartListening != null) {
                    // "state is Loading" will always be true except when the load() is being
                    // joined by init().
                    // "state.thenStartListening" might be "null" if, in the brief moment between
                    // the compareAndSet() and reading _state.value, the state was changed by
                    // toggleThenStartListening().
                    startListening(sessions, state.thenStartListening)

                } else if (!_state.compareAndSet(Loading(null, true), Loaded(sessions))) {
                    // The current state is not the Loading state, which is unexpected. This means
                    // that load() is being joined by init(), which is reinitializing everything,
                    // so we should drop the sessions.
                    sessions.close()
                }

            } // else, the state was set to Loaded, so no need to do anything
        }
    }

    /**
     * Reads the vocabulary file. Each line contains a token and its ID separated by a space.
     * The Unicode block element U+2581 is replaced with a regular space. The special `<blk>`
     * token is recorded as the blank index.
     */
    private fun loadVocab(file: File): Map<Int, String> {
        val vocab = mutableMapOf<Int, String>()
        BufferedReader(InputStreamReader(file.inputStream(), Charsets.UTF_8)).use { reader ->
            reader.forEachLine { line ->
                val parts = line.trimEnd().split(" ", limit = 2)
                if (parts.size == 2) {
                    val token = parts[0].replace("\u2581", " ")
                    val id = parts[1].toIntOrNull() ?: return@forEachLine
                    vocab[id] = token
                }
            }
        }
        return vocab
    }

    /**
     * Atomically handles toggling the [Loading.thenStartListening] state, making sure that if in
     * the meantime the value is changed by [load], the user click is not wasted, and the state
     * machine does not end up in an inconsistent state.
     *
     * @param eventListener used only if the model has finished loading in the brief moment between
     * when the state is first checked, but if the state was switched to [Loaded] (and not
     * [Listening]), which means that this click should start listening.
     */
    private fun toggleThenStartListening(eventListener: (InputEvent) -> Unit) {
        if (
            !_state.compareAndSet(Loading(null), Loading(eventListener)) &&
            !_state.compareAndSet(Loading(eventListener), Loading(null))
        ) {
            // may happen if load() changes the state in the brief moment between when the state is
            // first checked before calling this function, and when the checks above are performed
            Log.w(TAG, "Cannot toggle thenStartListening")
            when (val newValue = _state.value) {
                is Loaded -> startListening(newValue.sessions, eventListener)
                is Listening -> stopListening(newValue.sessions, newValue.eventListener, true)
                is ErrorLoading -> {} // ignore the user's click
                // the else should never happen, since load() only transitions from Loading(...) to
                // one of Loaded, Listening or ErrorLoading
                else -> Log.e(TAG, "State was none of Loading, Loaded or Listening")
            }
        }
    }

    /**
     * Starts listening for audio input, and changes the state to [Listening].
     */
    private fun startListening(
        sessions: ParakeetSessions,
        eventListener: (InputEvent) -> Unit,
    ) {
        clearTransientUiState()
        activeListener?.stopAndDiscardCurrentAudio()
        _state.value = Listening(sessions, eventListener)
        val listener = ParakeetListener(
            this@ParakeetInputDevice,
            eventListener,
            DEFAULT_SILENCES_BEFORE_STOP,
            sessions,
        )
        activeListener = listener

        val job = scope.launch {
            try {
                listener.startRecording()
            } finally {
                if (activeListener === listener) {
                    activeListener = null
                }
            }
        }
        listeningJob = job
        job.invokeOnCompletion {
            if (listeningJob === job) {
                listeningJob = null
            }
        }
    }

    /**
     * Stops listening for audio input, and changes the state to [Loaded]. This is
     * `internal` because it is used by [ParakeetListener].
     */
    internal fun stopListening(
        sessions: ParakeetSessions,
        eventListener: (InputEvent) -> Unit,
        sendNoneEvent: Boolean,
    ) {
        if (sendNoneEvent) {
            activeListener?.stopAndDiscardCurrentAudio()
            activeListener = null
        }
        clearTransientUiState()
        _state.value = Loaded(sessions)
        if (sendNoneEvent) {
            eventListener(InputEvent.None)
        }
    }

    internal fun setTransientUiState(state: SttState) {
        _transientUiState.value = state
    }

    internal fun clearTransientUiState() {
        _transientUiState.value = null
    }

    override suspend fun destroy() {
        deinit()
        // cancel everything
        scope.cancel()
    }

    companion object {
        private val TAG = ParakeetInputDevice::class.simpleName
        private const val DEFAULT_SILENCES_BEFORE_STOP = 1

        /**
         * Base URL for the pre-quantized ONNX model from
         * [istupakov/parakeet-tdt-0.6b-v3-onnx](https://huggingface.co/istupakov/parakeet-tdt-0.6b-v3-onnx).
         * Parakeet v3 is a single multilingual model that auto-detects the spoken language,
         * so all locale keys point to the same HuggingFace repository. Individual model files
         * (encoder, decoder_joint, preprocessor, vocab) are resolved relative to this URL
         * during download.
         *
         * INT8-quantized files used:
         * - `encoder-model.int8.onnx` (~652 MB)
         * - `decoder_joint-model.int8.onnx` (~18 MB)
         * - `nemo128.onnx` (~140 KB, mel-spectrogram preprocessor)
         * - `vocab.txt` (~94 KB)
         *
         * Supported languages (25 European languages):
         * bg, hr, cs, da, nl, en, et, fi, fr, de, el, hu, it, lv, lt, mt, pl, pt, ro, sk,
         * sl, es, sv, ru, uk
         *
         * @see <a href="https://huggingface.co/nvidia/parakeet-tdt-0.6b-v3">NVIDIA model card</a>
         * @see <a href="https://huggingface.co/istupakov/parakeet-tdt-0.6b-v3-onnx">ONNX conversion</a>
         */
        private const val PARAKEET_MODEL_BASE_URL =
            "https://huggingface.co/istupakov/parakeet-tdt-0.6b-v3-onnx"

        val MODEL_URLS = mapOf(
            "bg" to PARAKEET_MODEL_BASE_URL,
            "hr" to PARAKEET_MODEL_BASE_URL,
            "cs" to PARAKEET_MODEL_BASE_URL,
            "da" to PARAKEET_MODEL_BASE_URL,
            "nl" to PARAKEET_MODEL_BASE_URL,
            "en" to PARAKEET_MODEL_BASE_URL,
            "et" to PARAKEET_MODEL_BASE_URL,
            "fi" to PARAKEET_MODEL_BASE_URL,
            "fr" to PARAKEET_MODEL_BASE_URL,
            "de" to PARAKEET_MODEL_BASE_URL,
            "el" to PARAKEET_MODEL_BASE_URL,
            "hu" to PARAKEET_MODEL_BASE_URL,
            "it" to PARAKEET_MODEL_BASE_URL,
            "lv" to PARAKEET_MODEL_BASE_URL,
            "lt" to PARAKEET_MODEL_BASE_URL,
            "mt" to PARAKEET_MODEL_BASE_URL,
            "pl" to PARAKEET_MODEL_BASE_URL,
            "pt" to PARAKEET_MODEL_BASE_URL,
            "ro" to PARAKEET_MODEL_BASE_URL,
            "sk" to PARAKEET_MODEL_BASE_URL,
            "sl" to PARAKEET_MODEL_BASE_URL,
            "es" to PARAKEET_MODEL_BASE_URL,
            "sv" to PARAKEET_MODEL_BASE_URL,
            "ru" to PARAKEET_MODEL_BASE_URL,
            "uk" to PARAKEET_MODEL_BASE_URL,
        )
    }
}
