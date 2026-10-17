package com.aura.music

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import androidx.media3.common.C

data class FrequencyBandState(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val currentLevelMb: Short,
    val minLevelMb: Short,
    val maxLevelMb: Short
)

data class EqualizerState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val bands: List<FrequencyBandState> = emptyList(),
    val bassBoostSupported: Boolean = false,
    val bassStrength: Short = 0,
    val message: String = "Start playing a track to activate audio effects."
)

class AuraEqualizerManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var boundSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var enabledFlag: Boolean = true

    fun bindToAudioSession(sessionId: Int): EqualizerState {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) {
            return EqualizerState(
                isSupported = false,
                isEnabled = false,
                message = "Play a song first to initialize the hardware audio session."
            )
        }
        if (sessionId == boundSessionId && equalizer != null) {
            return snapshotState()
        }

        releaseEffects()
        boundSessionId = sessionId

        try {
            val eq = Equalizer(0, sessionId)
            eq.enabled = enabledFlag
            equalizer = eq
        } catch (e: Exception) {
            equalizer = null
        }

        try {
            val bass = BassBoost(0, sessionId)
            if (bass.strengthSupported) {
                bass.enabled = enabledFlag
                bassBoost = bass
            } else {
                bass.release()
                bassBoost = null
            }
        } catch (e: Exception) {
            bassBoost = null
        }

        return snapshotState()
    }

    fun setEffectsEnabled(enabled: Boolean): EqualizerState {
        enabledFlag = enabled
        runCatching { equalizer?.enabled = enabled }
        runCatching { bassBoost?.enabled = enabled }
        return snapshotState()
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short): EqualizerState {
        runCatching { equalizer?.setBandLevel(bandIndex, levelMb) }
        return snapshotState()
    }

    fun setBassStrength(strength: Short): EqualizerState {
        val coerced = strength.coerceIn(0, 1000)
        runCatching { bassBoost?.setStrength(coerced) }
        return snapshotState()
    }

    fun snapshotState(): EqualizerState {
        val eq = equalizer
            ?: return EqualizerState(
                isSupported = false,
                isEnabled = false,
                message = "Hardware Equalizer is not available on this device or session."
            )

        return try {
            val range = eq.bandLevelRange
            val minMb = range[0]
            val maxMb = range[1]
            val count = eq.numberOfBands.toInt()
            val bandList = (0 until count).map { i ->
                val idx = i.toShort()
                FrequencyBandState(
                    bandIndex = idx,
                    centerFreqHz = eq.getCenterFreq(idx) / 1000,
                    currentLevelMb = eq.getBandLevel(idx),
                    minLevelMb = minMb,
                    maxLevelMb = maxMb
                )
            }
            val bass = bassBoost
            val bassOk = bass != null && runCatching { bass.strengthSupported }.getOrDefault(false)
            val bassVal = if (bassOk) runCatching { bass?.roundedStrength ?: 0 }.getOrDefault(0) else 0

            EqualizerState(
                isSupported = true,
                isEnabled = eq.enabled,
                bands = bandList,
                bassBoostSupported = bassOk,
                bassStrength = bassVal,
                message = if (bassOk) "Equalizer & Bass Boost ready" else "Equalizer active (Bass Boost unsupported on device)"
            )
        } catch (e: Exception) {
            EqualizerState(
                isSupported = false,
                isEnabled = false,
                message = "Audio effect error: ${e.localizedMessage ?: "Unsupported hardware"}"
            )
        }
    }

    fun releaseEffects() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        equalizer = null
        bassBoost = null
        boundSessionId = C.AUDIO_SESSION_ID_UNSET
    }
}
