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

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.TensorInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.stypox.dicio.io.input.InputEvent
import org.stypox.dicio.io.input.SttState
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

/**
 * Handles audio recording and speech-to-text inference for the Parakeet TDT model using
 * ONNX Runtime. The inference pipeline mirrors the reference implementation from
 * [onnx-asr](https://github.com/istupakov/onnx-asr):
 *
 * 1. Record 16 kHz mono PCM audio from the microphone.
 * 2. Run the NeMo 128-dim mel-spectrogram preprocessor (`nemo128.onnx`).
 * 3. Run the FastConformer encoder (`encoder-model.int8.onnx`).
 * 4. Run TDT (Token-and-Duration Transducer) greedy decoding with the joint decoder
 *    (`decoder_joint-model.int8.onnx`).
 * 5. Map token IDs to text via `vocab.txt`.
 *
 * @param parakeetInputDevice the parent input device for state management
 * @param eventListener callback to receive transcription events
 * @param silencesBeforeStop how many consecutive silence chunks before auto-stopping; must be >= 1
 * @param sessions the loaded ONNX Runtime sessions and vocabulary
 */
internal class ParakeetListener(
    private val parakeetInputDevice: ParakeetInputDevice,
    private val eventListener: (InputEvent) -> Unit,
    private var silencesBeforeStop: Int,
    private val sessions: ParakeetSessions,
) {

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var isRecording = false
    @Volatile
    private var shouldProcessFinalAudio = true
    @Volatile
    private var stopRequestedByUser = false

    /**
     * Starts recording audio and processing it with the Parakeet model.
     *
     * Parakeet does not support true streaming: we record until speech-end (silence) and then run
     * a single final inference. To keep the user informed, the UI transitions through transient
     * phases: [SttState.Listening] -> [SttState.SilenceDetected] -> [SttState.Thinking].
     */
    suspend fun startRecording() = withContext(Dispatchers.IO) {
        try {
            shouldProcessFinalAudio = true
            stopRequestedByUser = false

            val bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid buffer size: $bufferSize")
                eventListener(InputEvent.Error(Exception("Invalid audio buffer size")))
                return@withContext
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed")
                eventListener(InputEvent.Error(Exception("AudioRecord initialization failed")))
                return@withContext
            }

            audioRecord?.startRecording()
            isRecording = true
            Log.i(TAG, "Recording started, bufferSize=$bufferSize")

            // Thread-safe audio accumulator: the recording coroutine appends, while the
            // partial-inference coroutine takes snapshots.
            val audioData = mutableListOf<Short>()
            val audioLock = Any()
            var hasHeardSpeech = false
            var silenceDetected = false
            var speechSamples = 0

            // Recording loop in the current (IO) coroutine
            val audioBuffer = ShortArray(bufferSize / 2)
            var consecutiveSilentReads = 0

            while (isRecording && isActive) {
                val readSize = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0

                if (readSize > 0) {
                    synchronized(audioLock) {
                        for (i in 0 until readSize) {
                            audioData.add(audioBuffer[i])
                        }
                    }

                    // Compute RMS amplitude of this chunk for silence detection
                    val rms = kotlin.math.sqrt(
                        audioBuffer.take(readSize)
                            .sumOf { it.toLong() * it.toLong() }
                            .toDouble() / readSize
                    )
                    val isSilent = rms < SILENCE_RMS_THRESHOLD

                    if (!isSilent) {
                        speechSamples += readSize
                        hasHeardSpeech =
                            speechSamples >= MIN_SPEECH_SAMPLES_BEFORE_AUTO_STOP
                        consecutiveSilentReads = 0
                    } else {
                        consecutiveSilentReads++
                    }

                    // Only consider silence-based stop after speech has been heard
                    val samplesPerSilenceUnit = (SILENCE_DURATION_MS * SAMPLE_RATE / 1000)
                    val readsPerSilenceUnit =
                        (samplesPerSilenceUnit / readSize).coerceAtLeast(1)
                    val requiredSilentReads = readsPerSilenceUnit * silencesBeforeStop

                    if (hasHeardSpeech && consecutiveSilentReads >= requiredSilentReads) {
                        val totalSamples = synchronized(audioLock) { audioData.size }
                        Log.i(TAG, "Silence detected after speech, " +
                            "$totalSamples samples " +
                            "(${totalSamples / SAMPLE_RATE.toFloat()}s)")
                        silenceDetected = true
                        break
                    }

                    // Safety cap: stop after MAX_RECORDING_SECONDS
                    val totalSamples = synchronized(audioLock) { audioData.size }
                    if (totalSamples >= SAMPLE_RATE * MAX_RECORDING_SECONDS) {
                        Log.i(TAG, "Max recording duration reached")
                        break
                    }
                }
            }

            // Stop recording hardware
            stopRecording()
            val finalAudio: ShortArray
            synchronized(audioLock) {
                finalAudio = audioData.toShortArray()
            }
            Log.i(TAG, "Recording stopped, total samples: ${finalAudio.size} " +
                "(${finalAudio.size / SAMPLE_RATE.toFloat()}s), hasHeardSpeech=$hasHeardSpeech")

            if (!shouldProcessFinalAudio) {
                Log.i(TAG, "Recording stopped by user, skipping final inference")
                return@withContext
            }

            if (silenceDetected) {
                parakeetInputDevice.setTransientUiState(SttState.SilenceDetected)
                delay(SILENCE_DETECTED_FEEDBACK_MS)
            }
            processAudio(finalAudio)

        } catch (e: Exception) {
            if (stopRequestedByUser) {
                Log.i(TAG, "Recording stopped by user while reading audio")
                return@withContext
            }
            Log.e(TAG, "Error during recording", e)
            eventListener(InputEvent.Error(e))
            stopRecording()
        }
    }

    /**
     * Processes the final audio data and generates the final transcription result.
     * After emitting the result, transitions the input device state back to [ParakeetState.Loaded]
     * so the UI shows the mic button again (matching Vosk's behavior in onResult).
     */
    private fun processAudio(audioData: ShortArray) {
        // Transition state from Listening → Loaded *before* emitting the event, so the UI
        // updates promptly. Pass sendNoneEvent = false because we emit our own event below.
        parakeetInputDevice.stopListening(sessions, eventListener, false)
        parakeetInputDevice.setTransientUiState(SttState.Thinking)

        try {
            if (audioData.isEmpty()) {
                Log.w(TAG, "processAudio: empty audio data")
                eventListener(InputEvent.None)
                return
            }

            Log.i(TAG, "processAudio: running inference on ${audioData.size} samples " +
                "(${audioData.size / SAMPLE_RATE.toFloat()}s)")
            val startTime = System.currentTimeMillis()
            val result = runInference(audioData)
            val elapsed = System.currentTimeMillis() - startTime
            Log.i(TAG, "processAudio: inference took ${elapsed}ms, result=\"$result\"")

            if (result.isBlank()) {
                eventListener(InputEvent.None)
            } else {
                eventListener(InputEvent.Final(listOf(Pair(result, 1.0f))))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing final audio", e)
            eventListener(InputEvent.Error(e))
        } finally {
            parakeetInputDevice.clearTransientUiState()
        }
    }

    /**
     * Runs the full Parakeet inference pipeline on the given PCM audio data:
     * preprocessor -> encoder -> TDT greedy decode -> vocab lookup.
     */
    private fun runInference(audioData: ShortArray): String {
        // 1. Convert PCM int16 to float32 normalized to [-1.0, 1.0]
        val floatAudio = FloatArray(audioData.size) { i ->
            audioData[i].toFloat() / Short.MAX_VALUE
        }

        // 2. Run the nemo128 mel-spectrogram preprocessor
        var stepStart = System.currentTimeMillis()
        val (features, featuresLens) = runPreprocessor(floatAudio)
        Log.i(TAG, "Preprocessor: ${System.currentTimeMillis() - stepStart}ms, " +
            "features size=${features.size}")

        // 3. Run the FastConformer encoder
        stepStart = System.currentTimeMillis()
        val (encoderOut, encoderOutLens) = runEncoder(features, featuresLens)
        Log.i(TAG, "Encoder: ${System.currentTimeMillis() - stepStart}ms, " +
            "T=${encoderOut.size}, D=${encoderOut.firstOrNull()?.size ?: 0}, len=$encoderOutLens")

        // 4. Run TDT greedy decoding
        stepStart = System.currentTimeMillis()
        val tokenIds = tdtGreedyDecode(encoderOut, encoderOutLens)
        Log.i(TAG, "TDT decode: ${System.currentTimeMillis() - stepStart}ms, " +
            "tokens=${tokenIds.size}")

        // 5. Map token IDs to text
        return decodeTokens(tokenIds)
    }

    /**
     * Runs the NeMo 128-dim mel-spectrogram preprocessor.
     *
     * Input:  `waveforms` [1, N] float32, `waveforms_lens` [1] int64
     * Output: `features` [1, 128, T] float32, `features_lens` [1] int64
     */
    private fun runPreprocessor(
        waveform: FloatArray
    ): Pair<FloatArray, LongArray> {
        val env = sessions.env
        val waveformTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(waveform),
            longArrayOf(1, waveform.size.toLong()),
        )
        val waveformLensTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(longArrayOf(waveform.size.toLong())),
            longArrayOf(1),
        )

        val result = sessions.preprocessor.run(
            mapOf("waveforms" to waveformTensor, "waveforms_lens" to waveformLensTensor)
        )

        val featuresTensor = result["features"].get() as OnnxTensor
        val featuresLensTensor = result["features_lens"].get() as OnnxTensor

        val featuresShape = featuresTensor.info.shape // [1, 128, T]
        val featuresFlat = featuresTensor.floatBuffer.let { buf ->
            FloatArray(buf.remaining()).also { buf.get(it) }
        }
        val featuresLens = featuresLensTensor.longBuffer.let { buf ->
            LongArray(buf.remaining()).also { buf.get(it) }
        }

        waveformTensor.close()
        waveformLensTensor.close()
        result.close()

        return Pair(featuresFlat, featuresLens)
    }

    /**
     * Runs the FastConformer encoder.
     *
     * Input:  `audio_signal` [1, 128, T] float32, `length` [1] int64
     * Output: `outputs` [1, D, T'] float32, `encoded_lengths` [1] int64
     *
     * The encoder output is transposed from [1, D, T'] to [T', D] for the decoder.
     */
    private fun runEncoder(
        features: FloatArray,
        featuresLens: LongArray
    ): Pair<Array<FloatArray>, Long> {
        val env = sessions.env

        // Reconstruct shape: the preprocessor output is [1, 128, T]
        val totalElements = features.size
        val featureDim = 128L
        val timeSteps = totalElements / featureDim

        val featuresTensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(features),
            longArrayOf(1, featureDim, timeSteps),
        )
        val lengthTensor = OnnxTensor.createTensor(
            env,
            LongBuffer.wrap(featuresLens),
            longArrayOf(1),
        )

        val result = sessions.encoder.run(
            mapOf("audio_signal" to featuresTensor, "length" to lengthTensor)
        )

        val outputsTensor = result["outputs"].get() as OnnxTensor
        val encodedLengthsTensor = result["encoded_lengths"].get() as OnnxTensor

        // outputs shape: [1, D, T'] — need to transpose to [T', D]
        val outShape = outputsTensor.info.shape // [1, D, T']
        val outD = outShape[1].toInt()
        val outT = outShape[2].toInt()
        val outFlat = outputsTensor.floatBuffer.let { buf ->
            FloatArray(buf.remaining()).also { buf.get(it) }
        }
        val encodedLength = encodedLengthsTensor.longBuffer.get(0)

        // Transpose from [1, D, T'] (row-major) to Array<FloatArray> [T'][D]
        val transposed = Array(outT) { t ->
            FloatArray(outD) { d -> outFlat[d * outT + t] }
        }

        featuresTensor.close()
        lengthTensor.close()
        result.close()

        return Pair(transposed, encodedLength)
    }

    /**
     * TDT (Token-and-Duration Transducer) greedy decoding. At each encoder time step, the
     * decoder+joint network produces logits for both the token vocabulary and the duration
     * (how many encoder frames to skip). This follows the reference implementation from
     * `onnx-asr` (`NemoConformerTdt._decode` and `_AsrWithTransducerDecoding._decoding`).
     *
     * @param encoderOut encoder output, shape [T, D] (already transposed)
     * @param encodedLength number of valid encoder time steps
     * @return list of decoded token IDs (excluding blanks)
     */
    private fun tdtGreedyDecode(
        encoderOut: Array<FloatArray>,
        encodedLength: Long,
    ): List<Int> {
        val env = sessions.env
        val blankIdx = sessions.blankIdx
        val vocabSize = sessions.vocabSize

        // Initialize decoder LSTM states to zero, following the reference implementation
        // (NemoConformerRnnt._create_state): use dim[0] and dim[2] from the model's
        // input metadata (which are static), and hardcode dim[1]=1 (the batch/sequence
        // dimension, which is dynamic=-1 in the ONNX model and would cause
        // NegativeArraySizeException if multiplied).
        val decoderInputs = sessions.decoderJoint.inputInfo
        val state1ModelShape = (decoderInputs["input_states_1"]?.info as? TensorInfo)?.shape
        val state2ModelShape = (decoderInputs["input_states_2"]?.info as? TensorInfo)?.shape

        val state1Shape = if (state1ModelShape != null && state1ModelShape.size == 3) {
            longArrayOf(state1ModelShape[0], 1, state1ModelShape[2])
        } else {
            longArrayOf(2, 1, 640)
        }
        val state2Shape = if (state2ModelShape != null && state2ModelShape.size == 3) {
            longArrayOf(state2ModelShape[0], 1, state2ModelShape[2])
        } else {
            longArrayOf(2, 1, 640)
        }

        var state1 = FloatArray((state1Shape[0] * state1Shape[1] * state1Shape[2]).toInt())
        var state2 = FloatArray((state2Shape[0] * state2Shape[1] * state2Shape[2]).toInt())

        val tokens = mutableListOf<Int>()
        var t = 0
        var emittedTokens = 0
        val maxT = encodedLength.toInt().coerceAtMost(encoderOut.size)

        while (t < maxT) {
            // Prepare the encoder output for this time step: [1, D, 1]
            val encoderFrame = encoderOut[t]
            val d = encoderFrame.size

            val encoderOutputsTensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(encoderFrame),
                longArrayOf(1, d.toLong(), 1),
            )

            // Target: the last emitted token, or blank if none emitted yet
            val targetToken = if (tokens.isNotEmpty()) tokens.last() else blankIdx
            val targetsTensor = OnnxTensor.createTensor(
                env,
                IntBuffer.wrap(intArrayOf(targetToken)),
                longArrayOf(1, 1),
            )
            val targetLengthTensor = OnnxTensor.createTensor(
                env,
                IntBuffer.wrap(intArrayOf(1)),
                longArrayOf(1),
            )
            val states1Tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(state1),
                state1Shape,
            )
            val states2Tensor = OnnxTensor.createTensor(
                env,
                FloatBuffer.wrap(state2),
                state2Shape,
            )

            val result = sessions.decoderJoint.run(
                mapOf(
                    "encoder_outputs" to encoderOutputsTensor,
                    "targets" to targetsTensor,
                    "target_length" to targetLengthTensor,
                    "input_states_1" to states1Tensor,
                    "input_states_2" to states2Tensor,
                )
            )

            val outputsTensor = result["outputs"].get() as OnnxTensor
            val outState1Tensor = result["output_states_1"].get() as OnnxTensor
            val outState2Tensor = result["output_states_2"].get() as OnnxTensor

            val outputs = outputsTensor.floatBuffer.let { buf ->
                FloatArray(buf.remaining()).also { buf.get(it) }
            }
            val newState1 = outState1Tensor.floatBuffer.let { buf ->
                FloatArray(buf.remaining()).also { buf.get(it) }
            }
            val newState2 = outState2Tensor.floatBuffer.let { buf ->
                FloatArray(buf.remaining()).also { buf.get(it) }
            }

            // TDT: first vocabSize elements are token logits, rest are duration logits
            val tokenLogits = outputs.copyOfRange(0, vocabSize)
            val durationLogits = outputs.copyOfRange(vocabSize, outputs.size)

            // Greedy: pick the token with the highest logit
            val token = tokenLogits.indices.maxByOrNull { tokenLogits[it] } ?: blankIdx

            // Greedy: pick the duration with the highest logit
            val step = if (durationLogits.isNotEmpty()) {
                durationLogits.indices.maxByOrNull { durationLogits[it] } ?: 0
            } else {
                -1 // fallback for plain RNN-T (no duration head)
            }

            if (token != blankIdx) {
                // Non-blank emission: update decoder states and record the token
                state1 = newState1
                state2 = newState2
                tokens.add(token)
                emittedTokens++
            }

            // Advance the time step based on the duration prediction
            if (step > 0) {
                t += step
                emittedTokens = 0
            } else if (token == blankIdx || emittedTokens >= MAX_TOKENS_PER_STEP) {
                t += 1
                emittedTokens = 0
            }

            // Clean up ORT tensors for this iteration
            encoderOutputsTensor.close()
            targetsTensor.close()
            targetLengthTensor.close()
            states1Tensor.close()
            states2Tensor.close()
            result.close()
        }

        return tokens
    }

    /**
     * Maps a list of token IDs to their string representations using the vocabulary,
     * then joins and trims the result.
     */
    private fun decodeTokens(tokenIds: List<Int>): String {
        return tokenIds.mapNotNull { sessions.vocab[it] }
            .joinToString("")
            .trim()
    }

    /**
     * Stops the audio recording.
     */
    fun stopRecording() {
        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
        }
    }

    fun stopAndDiscardCurrentAudio() {
        stopRequestedByUser = true
        shouldProcessFinalAudio = false
        stopRecording()
    }

    companion object {
        private val TAG = ParakeetListener::class.simpleName
        /** Parakeet models use 16 kHz sample rate. */
        private const val SAMPLE_RATE = 16000
        /** RMS amplitude threshold for silence detection (on int16 PCM samples). */
        private const val SILENCE_RMS_THRESHOLD = 300.0
        /** Duration in milliseconds that one "silence unit" represents. */
        private const val SILENCE_DURATION_MS = 1000
        /** Maximum recording duration in seconds (safety cap). */
        private const val MAX_RECORDING_SECONDS = 30
        /** Maximum tokens the TDT decoder may emit per encoder time step. */
        private const val MAX_TOKENS_PER_STEP = 10
        /** How long to show "Silence detected" before switching to "Thinking". */
        private const val SILENCE_DETECTED_FEEDBACK_MS = 800L
        /**
         * Minimum amount of detected speech before auto-stopping on silence.
         * Helps avoid false early stop from brief noise spikes.
         */
        private const val MIN_SPEECH_SAMPLES_BEFORE_AUTO_STOP = SAMPLE_RATE * 2 / 5
    }
}
