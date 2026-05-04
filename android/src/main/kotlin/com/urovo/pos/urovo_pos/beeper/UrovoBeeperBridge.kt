package com.urovo.pos.urovo_pos.beeper

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import com.urovo.pos.urovo_pos.UrovoPluginException
import kotlin.math.roundToInt

internal class UrovoBeeperBridge : UrovoBeeperApi {
    private val handler = Handler(Looper.getMainLooper())
    private val scheduledCallbacks = mutableListOf<Runnable>()
    private var activeToneGenerator: ToneGenerator? = null

    override fun beeperBeep(
        pattern: String,
        repeat: Int,
        durationMs: Int,
        intervalMs: Int,
        volume: Double,
    ) {
        validateArguments(
            pattern = pattern,
            repeat = repeat,
            durationMs = durationMs,
            intervalMs = intervalMs,
            volume = volume,
        )

        beeperStop()

        val volumePercent = (volume * 100).roundToInt().coerceIn(0, 100)
        val toneType = toneTypeForPattern(pattern)
        val toneGenerator = runCatching {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, volumePercent)
        }.getOrElse { error ->
            throw UrovoPluginException(
                errorCode = "device_unavailable",
                message = "Unable to initialize Android tone generator: ${error.message ?: "Unknown error."}",
            )
        }
        activeToneGenerator = toneGenerator

        repeat(repeat) { index ->
            val startDelayMs = index.toLong() * (durationMs + intervalMs).toLong()
            val callback = Runnable {
                toneGenerator.startTone(toneType, durationMs)
            }
            scheduledCallbacks.add(callback)
            handler.postDelayed(callback, startDelayMs)
        }

        val releaseCallback = Runnable {
            releaseToneGenerator(toneGenerator)
            scheduledCallbacks.clear()
        }
        scheduledCallbacks.add(releaseCallback)
        handler.postDelayed(
            releaseCallback,
            (repeat.toLong() * durationMs) + ((repeat - 1).toLong() * intervalMs) + RELEASE_GRACE_MS,
        )
    }

    override fun beeperStop() {
        scheduledCallbacks.forEach(handler::removeCallbacks)
        scheduledCallbacks.clear()
        activeToneGenerator?.let(::releaseToneGenerator)
        activeToneGenerator = null
    }

    private fun releaseToneGenerator(toneGenerator: ToneGenerator) {
        runCatching {
            toneGenerator.stopTone()
        }
        runCatching {
            toneGenerator.release()
        }
        if (activeToneGenerator === toneGenerator) {
            activeToneGenerator = null
        }
    }

    private fun validateArguments(
        pattern: String,
        repeat: Int,
        durationMs: Int,
        intervalMs: Int,
        volume: Double,
    ) {
        if (pattern !in SUPPORTED_PATTERNS) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "Unsupported beeper pattern: $pattern.",
            )
        }
        if (repeat !in 1..10) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "repeat must be between 1 and 10.",
            )
        }
        if (durationMs !in 1..5000) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "durationMs must be between 1 and 5000.",
            )
        }
        if (intervalMs !in 0..5000) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "intervalMs must be between 0 and 5000.",
            )
        }
        if (volume.isNaN() || volume < 0.0 || volume > 1.0) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "volume must be between 0.0 and 1.0.",
            )
        }
    }

    private fun toneTypeForPattern(pattern: String): Int {
        return when (pattern) {
            "success" -> ToneGenerator.TONE_PROP_ACK
            "warning" -> ToneGenerator.TONE_PROP_PROMPT
            "error" -> ToneGenerator.TONE_PROP_NACK
            else -> ToneGenerator.TONE_PROP_BEEP
        }
    }

    private companion object {
        private const val RELEASE_GRACE_MS = 80L
        private val SUPPORTED_PATTERNS = setOf("short", "success", "warning", "error")
    }
}
