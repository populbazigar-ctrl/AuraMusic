package com.aura.music

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuraSleepTimer(
    private val scope: CoroutineScope,
    private val onTimerFinished: () -> Unit
) {
    private var timerJob: Job? = null
    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    fun startMinutes(minutes: Int) {
        cancel()
        val totalSeconds = minutes * 60L
        _remainingSeconds.value = totalSeconds
        timerJob = scope.launch {
            while (_remainingSeconds.value > 0L) {
                delay(1_000L)
                _remainingSeconds.value -= 1L
            }
            onTimerFinished()
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingSeconds.value = 0L
    }

    fun formatRemaining(seconds: Long): String {
        if (seconds <= 0L) return "Off"
        val m = seconds / 60L
        val s = seconds % 60L
        return "%02d:%02d".format(m, s)
    }
}
