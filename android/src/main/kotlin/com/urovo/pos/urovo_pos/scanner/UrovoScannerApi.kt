package com.urovo.pos.urovo_pos.scanner

import android.content.Context

internal interface UrovoScannerApi {
    fun scannerStart(cameraId: Int, timeoutMs: Long)

    fun scannerStop()

    fun setEventCallback(callback: ((Map<String, Any?>) -> Unit)?)

    fun setForegroundContext(context: Context?)
}
