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

import org.stypox.dicio.io.input.InputEvent
import org.stypox.dicio.io.input.SttState
import org.stypox.dicio.ui.util.Progress

/**
 * The internal state for [ParakeetInputDevice]. This is an enum with different fields depending on
 * the current state, to avoid having nullable objects all over the place in [ParakeetInputDevice].
 * [SttState] is symmetrical to this enum, except that it does not expose implementation-defined
 * fields to the UI.
 */
sealed interface ParakeetState {

    /**
     * The ParakeetInputDevice has not been initialized yet, or has just been deinitialized
     */
    data object NotInitialized : ParakeetState

    /**
     * The model is not available for the current locale
     */
    data object NotAvailable : ParakeetState

    /**
     * The model is not present on disk.
     */
    data class NotDownloaded(
        val modelUrl: String
    ) : ParakeetState

    data class Downloading(
        val progress: Progress,
    ) : ParakeetState

    data class ErrorDownloading(
        val modelUrl: String,
        val throwable: Throwable
    ) : ParakeetState

    data object Downloaded : ParakeetState

    /**
     * The model is present on disk, but was not loaded in RAM yet.
     */
    data object NotLoaded : ParakeetState

    /**
     * The model is being loaded, and the nullity of [thenStartListening] indicates whether once
     * loading is finished, the STT should start listening right away.
     * [shouldEqualAnyLoading] is used just to create a [Loading] object with compares equal to any
     * other [Loading], but [Loading] with [shouldEqualAnyLoading]` = true` will never appear as a
     * state.
     */
    data class Loading(
        val thenStartListening: ((InputEvent) -> Unit)?,
        val shouldEqualAnyLoading: Boolean = false,
    ) : ParakeetState {
        override fun equals(other: Any?): Boolean {
            if (other !is Loading)
                return false
            if (shouldEqualAnyLoading || other.shouldEqualAnyLoading)
                return true
            return (this.thenStartListening == null) == (other.thenStartListening == null)
        }

        override fun hashCode(): Int {
            return if (thenStartListening == null) 0 else 1;
        }
    }

    data class ErrorLoading(
        val throwable: Throwable
    ) : ParakeetState

    /**
     * The model is ready in RAM, and can start listening at any time.
     */
    data class Loaded(
        internal val sessions: ParakeetSessions
    ) : ParakeetState

    /**
     * The model is listening.
     */
    data class Listening(
        internal val sessions: ParakeetSessions,
        internal val eventListener: (InputEvent) -> Unit,
    ) : ParakeetState

    /**
     * Converts this [ParakeetState] to a [SttState], which is basically the same, except that
     * implementation-defined fields (e.g. [ParakeetSessions]) are stripped away.
     */
    fun toUiState(): SttState {
        return when (this) {
            NotInitialized -> SttState.NotInitialized
            NotAvailable -> SttState.NotAvailable
            is NotDownloaded -> SttState.NotDownloaded
            is Downloading -> SttState.Downloading(progress)
            is ErrorDownloading -> SttState.ErrorDownloading(throwable)
            Downloaded -> SttState.Downloaded
            NotLoaded -> SttState.NotLoaded
            is Loading -> SttState.Loading(thenStartListening != null)
            is ErrorLoading -> SttState.ErrorLoading(throwable)
            is Loaded -> SttState.Loaded
            is Listening -> SttState.Listening
        }
    }
}

/**
 * Holds all ONNX Runtime inference sessions needed for Parakeet TDT inference:
 * the NeMo mel-spectrogram preprocessor, the FastConformer encoder, and the
 * RNN-T/TDT joint decoder, plus the decoded vocabulary map.
 *
 * @param encoder the FastConformer encoder session (`encoder-model.int8.onnx`)
 * @param decoderJoint the joint decoder session (`decoder_joint-model.int8.onnx`)
 * @param preprocessor the mel-spectrogram preprocessor session (`nemo128.onnx`)
 * @param vocab mapping from token ID to decoded string (from `vocab.txt`)
 * @param env the shared ONNX Runtime environment
 */
data class ParakeetSessions(
    internal val encoder: ai.onnxruntime.OrtSession,
    internal val decoderJoint: ai.onnxruntime.OrtSession,
    internal val preprocessor: ai.onnxruntime.OrtSession,
    internal val vocab: Map<Int, String>,
    internal val env: ai.onnxruntime.OrtEnvironment,
) {
    /** The blank token ID, used as the initial decoder target and to detect blank emissions. */
    val blankIdx: Int = vocab.entries.firstOrNull { it.value == "<blk>" }?.key
        ?: (vocab.size - 1)

    /** Total vocabulary size (including the blank token). */
    val vocabSize: Int = vocab.size

    fun close() {
        preprocessor.close()
        encoder.close()
        decoderJoint.close()
        env.close()
    }
}
