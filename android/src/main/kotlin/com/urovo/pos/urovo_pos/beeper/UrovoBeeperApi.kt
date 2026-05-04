package com.urovo.pos.urovo_pos.beeper

internal interface UrovoBeeperApi {
    fun beeperBeep(
        pattern: String,
        repeat: Int,
        durationMs: Int,
        intervalMs: Int,
        volume: Double,
    )

    fun beeperStop()
}
