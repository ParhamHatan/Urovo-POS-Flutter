package com.urovo.pos.urovo_pos.device

internal interface UrovoDeviceApi {
    fun isSdkAvailable(): Boolean

    fun deviceGetStatus(): Map<String, Any?>
}
