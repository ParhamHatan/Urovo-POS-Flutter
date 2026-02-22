package com.urovo.pos.urovo_pos.printer

internal interface UrovoPrinterApi {
    fun isSdkAvailable(): Boolean

    fun printerInit()

    fun printerClose()

    fun printerGetStatusDetail(): Map<String, Any>

    fun printerSetGray(level: Int)

    fun printerStartPrint(): Map<String, Any>

    fun printerRunJob(arguments: Map<String, Any?>): Map<String, Any>
}
