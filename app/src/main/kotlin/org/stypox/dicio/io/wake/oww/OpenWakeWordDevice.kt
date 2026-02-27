package org.stypox.dicio.io.wake.oww

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.stypox.dicio.io.wake.WakeDevice
import org.stypox.dicio.io.wake.WakeState
import org.stypox.dicio.ui.util.Progress
import org.stypox.dicio.util.FileToDownload
import org.stypox.dicio.util.downloadBinaryFilesWithPartial
import java.io.File
import java.io.IOException

class OpenWakeWordDevice(
    @param:ApplicationContext private val appContext: Context,
    private val okHttpClient: OkHttpClient,
) : WakeDevice {
    private val _state: MutableStateFlow<WakeState>
    override val state: StateFlow<WakeState>

    private val cacheDir: File = appContext.cacheDir
    private val owwFolder = File(appContext.filesDir, "openWakeWord")
    private val melFile = FileToDownload(MEL_URL, File(owwFolder, "melspectrogram.tflite"))
    private val embFile = FileToDownload(EMB_URL, File(owwFolder, "embedding.tflite"))
    private val wakeFile = FileToDownload(WAKE_URL, File(owwFolder, "wake.tflite"))
    private val userWakeFile = userWakeFile(appContext)
    private val userWakeFileExists = userWakeFile.exists()
    private val allModelFiles =
        // wakeFile is not needed if we want to use the userWakeFile instead
        if (userWakeFileExists) listOf(melFile, embFile)
        else listOf(melFile, embFile, wakeFile)

    private val audio = FloatArray(OwwModel.MEL_INPUT_COUNT)
    private var model: OwwModel? = null

    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        _state = if (allModelFiles.any(FileToDownload::needsToBeDownloaded)) {
            MutableStateFlow(WakeState.NotDownloaded)
        } else {
            MutableStateFlow(WakeState.NotLoaded)
        }
        state = _state
    }

    override fun download() {
        _state.value = WakeState.Downloading(Progress.UNKNOWN)

        scope.launch {
            try {
                owwFolder.mkdirs()
                downloadBinaryFilesWithPartial(
                    urlsFiles = allModelFiles,
                    httpClient = okHttpClient,
                    cacheDir = cacheDir,
                ) { progress ->
                    _state.value = WakeState.Downloading(progress)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Can't download OpenWakeWord model", e)
                _state.value = WakeState.ErrorDownloading(e)
                return@launch
            }

            _state.value = WakeState.NotLoaded
        }
    }

    override fun processFrame(audio16bitPcm: ShortArray): Boolean {
        if (audio16bitPcm.size != OwwModel.MEL_INPUT_COUNT) {
            throw IllegalArgumentException(
                "OwwModel can only process audio frames of ${OwwModel.MEL_INPUT_COUNT} samples"
            )
        }

        if (model == null) {
            if (_state.value.let { it != WakeState.NotLoaded && it !is WakeState.ErrorLoading }) {
                throw IOException("Model has not been downloaded yet")
            }

            try {
                _state.value = WakeState.Loading
                model = OwwModel(
                    melFile.file,
                    embFile.file,
                    if (userWakeFileExists) userWakeFile else wakeFile.file,
                )
                _state.value = WakeState.Loaded
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to load model", t)
                _state.value = WakeState.ErrorLoading(t)
                throw t
            }
        }

        for (i in 0..<OwwModel.MEL_INPUT_COUNT) {
            audio[i] = audio16bitPcm[i].toFloat() / 32768.0f
        }

        return model!!.processFrame(audio) > 0.8f
    }

    override fun frameSize(): Int {
        return OwwModel.MEL_INPUT_COUNT
    }

    override fun isOccupyingResources(): Boolean = model != null

    override fun destroy() {
        model?.close()
        model = null
        scope.cancel()
    }

    override fun isHeyDicio(): Boolean = !userWakeFileExists

    companion object {
        val TAG = OpenWakeWordDevice::class.simpleName
        const val MEL_URL = "https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/melspectrogram.tflite"
        const val EMB_URL = "https://github.com/dscripka/openWakeWord/releases/download/v0.5.1/embedding_model.tflite"
        const val WAKE_URL = "https://github.com/Stypox/dicio-android/releases/download/v2.0/hey_dicio_v6.0.tflite"

        private fun userWakeFile(context: Context) =
            File(context.filesDir, "openWakeWord/userwake.tflite")

        suspend fun addUserWakeFile(context: Context, source: Uri) {
            // Use a partial file to ensure atomicity
            val userWakeFile = userWakeFile(context)
            withContext(Dispatchers.IO) {
                val partialFile = File.createTempFile(userWakeFile.name, ".part", context.cacheDir)
                val inputStream = context.contentResolver.openInputStream(source)
                if (inputStream != null) {
                    inputStream.use { source ->
                        partialFile.outputStream().use {
                            source.copyTo(it)
                        }
                    }

                    // Remove the previous file if it already exists
                    userWakeFile.delete()
                    userWakeFile.parentFile?.mkdirs()
                    val renameOk = partialFile.renameTo(userWakeFile)
                    if (!renameOk) {
                        throw IOException("Cannot rename partial file $partialFile to actual file $userWakeFile")
                    }
                }
            }
        }

        suspend fun removeUserWakeFile(context: Context) {
            withContext(Dispatchers.IO) {
                userWakeFile(context).delete()
            }
        }
    }
}
