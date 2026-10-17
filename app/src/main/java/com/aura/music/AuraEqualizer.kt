package com.aura.music

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerBandState(
    val bandIndex: Short,
    val centerFreqHz: Int,
    val minLevelMb: Short,
    val maxLevelMb: Short,
    val currentLevelMb: Short
)

data class EqualizerState(
    val isSupported: Boolean = false,
    val isEnabled: Boolean = false,
    val bands: List<EqualizerBandState> = emptyList(),
    val bassBoostSupported: Boolean = false,
    val bassStrength: Short = 0,
    val message: String = "Equalizer ready"
)

class AuraEqualizerController {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachSession(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) {
            _state.value = EqualizerState(message = "Invalid audio session ID")
            return
        }
        try {
            val eq = Equalizer(0, audioSessionId).apply { enabled = true }
            val bb = BassBoost(0, audioSessionId).apply { enabled = true }

            equalizer = eq
            bassBoost = bb

            val numBands = eq.numberOfBands
            val bandList = mutableListOf<EqualizerBandState>()
            for (i in 0 until numBands) {
                val center = eq.getCenterFreq(i.toShort()) / 1000
                val range = eq.bandLevelRange
                val current = eq.getBandLevel(i.toShort())
                bandList.add(
                    EqualizerBandState(
                        bandIndex = i.toShort(),
                        centerFreqHz = center,
                        minLevelMb = range[0],
                        maxLevelMb = range[1],
                        currentLevelMb = current
                    )
                )
            }

            val bassSupported = bb.roundedSupported
            val bassStr = if (bassSupported) bb.roundedStrength else 0

            _state.value = EqualizerState(
                isSupported = true,
                isEnabled = true,
                bands = bandList,
                bassBoostSupported = bassSupported,
                bassStrength = bassStr,
                message = "Equalizer active"
            )
        } catch (e: Exception) {
            _state.value = EqualizerState(
                isSupported = false,
                message = "Equalizer not supported on this device/session"
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        runCatching {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            _state.value = _state.value.copy(isEnabled = enabled)
        }
    }

    fun setBandLevel(bandIndex: Short, levelMb: Short) {
        runCatching {
            equalizer?.setBandLevel(bandIndex, levelMb)
            val currentBands = _state.value.bands.map {
                if (it.bandIndex == bandIndex) it.copy(currentLevelMb = levelMb) else it
            }
            _state.value = _state.value.copy(bands = currentBands)
        }
    }

    fun setBassStrength(strength: Short) {
        runCatching {
            if (bassBoost?.roundedSupported == true) {
                bassBoost?.setStrength(strength)
                _state.value = _state.value.copy(bassStrength = strength)
            }
        }
    }

    fun release() {
        runCatching {
            equalizer?.release()
            bassBoost?.release()
        }
        equalizer = null
        bassBoost = null
    }
}
