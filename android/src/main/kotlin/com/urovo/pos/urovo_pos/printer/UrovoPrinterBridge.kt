package com.urovo.pos.urovo_pos.printer

import android.content.Context
import android.os.Bundle
import android.util.Base64
import com.urovo.pos.urovo_pos.UrovoPluginException
import java.io.Serializable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

internal class UrovoPrinterBridge(
    private val appContext: Context,
) : UrovoPrinterApi {
    private var printerProvider: Any? = null
    private var isPrinterInitialized = false

    override fun isSdkAvailable(): Boolean {
        return try {
            Class.forName(PRINTER_PROVIDER_CLASS)
            Class.forName(PRINT_STATUS_CLASS)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    override fun printerInit() {
        ensureSdkAvailable()
        val provider = ensurePrinterProvider()
        val initCode = invokeInt(provider, "initPrint")
        if (initCode != STATUS_OK) {
            isPrinterInitialized = false
            throw UrovoPluginException(
                errorCode = "device_unavailable",
                message = "initPrint failed with status code $initCode.",
                details = mapOf("statusDetail" to statusDetailForCode(initCode)),
            )
        }
        isPrinterInitialized = true
    }

    override fun printerClose() {
        ensureSdkAvailable()
        val provider = printerProvider ?: run {
            isPrinterInitialized = false
            return
        }
        runCatching {
            invokeInt(provider, "close")
        }
        isPrinterInitialized = false
        printerProvider = null
    }

    override fun printerGetStatusDetail(): Map<String, Any> {
        val provider = requireInitializedPrinterProvider(operationName = "printerGetStatusDetail")
        val statusCode = invokeInt(provider, "getStatus")
        return statusDetailForCode(statusCode)
    }

    override fun printerSetGray(level: Int) {
        if (level < 0 || level > 10) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "gray level must be between 0 and 10.",
            )
        }

        val provider = requireInitializedPrinterProvider(operationName = "printerSetGray")
        invokeUnit(provider, "setGray", level)
    }

    override fun printerStartPrint(): Map<String, Any> {
        val provider = requireInitializedPrinterProvider(operationName = "printerStartPrint")
        val printCode = invokeInt(provider, "startPrint")
        val detail = statusDetailForCode(printCode)
        if (printCode != STATUS_OK) {
            val errorCode = startPrintErrorCodeForStatus(printCode)
            throw UrovoPluginException(
                errorCode = errorCode,
                message = "startPrint failed with status code $printCode.",
                details = mapOf("statusDetail" to detail),
            )
        }
        return detail
    }

    override fun printerRunJob(arguments: Map<String, Any?>): Map<String, Any> {
        val provider = requireInitializedPrinterProvider(operationName = "printerRunJob")
        val gray = (arguments["gray"] as? Number)?.toInt()
        if (gray != null) {
            printerSetGray(gray)
        }

        val commands = arguments["commands"] as? List<*>
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "commands must be a list.",
            )

        commands.forEachIndexed { index, rawCommand ->
            val command = rawCommand as? Map<*, *>
                ?: throw UrovoPluginException(
                    errorCode = "invalid_argument",
                    message = "Command at index $index must be a map.",
                )
            applyCommand(provider, command)
        }

        return printerStartPrint()
    }

    private fun applyCommand(provider: Any, command: Map<*, *>) {
        val type = command["type"] as? String
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "Command type is required.",
            )

        when (type) {
            "text" -> {
                val text = command["text"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "text command requires text.",
                    )
                val styleBundle = textStyleBundle(command["style"] as? Map<*, *>)
                invokeUnit(provider, "addText", styleBundle, text)
            }

            "blackLine" -> invokeUnit(provider, "addBlackLine")

            "textLeftRight" -> {
                val left = command["left"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "textLeftRight requires left text.",
                    )
                val right = command["right"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "textLeftRight requires right text.",
                    )
                val styleBundle = textStyleBundle(command["style"] as? Map<*, *>)
                invokeUnit(provider, "addTextLeft_Right", styleBundle, left, right)
            }

            "textLeftCenterRight" -> {
                val left = command["left"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "textLeftCenterRight requires left text.",
                    )
                val center = command["center"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "textLeftCenterRight requires center text.",
                    )
                val right = command["right"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "textLeftCenterRight requires right text.",
                    )
                val styleBundle = textStyleBundle(command["style"] as? Map<*, *>)
                invokeUnit(provider, "addTextLeft_Center_Right", styleBundle, left, center, right)
            }

            "barcode" -> {
                val data = command["data"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "barcode command requires data.",
                    )
                val width = (command["width"] as? Number)?.toInt() ?: 300
                val height = (command["height"] as? Number)?.toInt() ?: 100
                val align = alignValue(command["align"] as? String)
                val barcodeType = resolveBarcodeFormat(command["barcodeType"] as? String)
                val bundle = Bundle().apply {
                    putInt(KEY_ALIGN, align)
                    putInt(KEY_WIDTH, width)
                    putInt(KEY_HEIGHT, height)
                    if (barcodeType is Serializable) {
                        putSerializable(KEY_BARCODE_TYPE, barcodeType)
                    }
                }
                invokeUnit(provider, "addBarCode", bundle, data)
            }

            "qr" -> {
                val data = command["data"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "qr command requires data.",
                    )
                val expectedHeight = (command["expectedHeight"] as? Number)?.toInt() ?: 120
                val align = alignValue(command["align"] as? String)
                val bundle = Bundle().apply {
                    putInt(KEY_ALIGN, align)
                    putInt(KEY_EXPECTED_HEIGHT, expectedHeight)
                }
                invokeUnit(provider, "addQrCode", bundle, data)
            }

            "imageBytes" -> {
                val base64Bytes = command["bytes"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "imageBytes command requires bytes.",
                    )
                val bytes = try {
                    Base64.decode(base64Bytes, Base64.DEFAULT)
                } catch (_: IllegalArgumentException) {
                    throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "image bytes are not valid base64.",
                    )
                }
                val width = (command["width"] as? Number)?.toInt() ?: 200
                val height = (command["height"] as? Number)?.toInt() ?: 80
                val align = alignValue(command["align"] as? String)
                val bundle = Bundle().apply {
                    putInt(KEY_ALIGN, align)
                    putInt(KEY_WIDTH, width)
                    putInt(KEY_HEIGHT, height)
                }
                invokeUnit(provider, "addImage", bundle, bytes)
            }

            "feedLine" -> {
                val lines = (command["lines"] as? Number)?.toInt() ?: 0
                invokeUnit(provider, "feedLine", lines)
            }

            "paperFeed" -> {
                val dots = (command["height"] as? Number)?.toInt() ?: 0
                invokeUnit(provider, "paperFeed", dots)
            }

            else -> throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "Unsupported print command type: $type.",
            )
        }
    }

    private fun textStyleBundle(style: Map<*, *>?): Bundle {
        val align = alignValue(style?.get("align") as? String)
        val font = fontValue(style?.get("font") as? String)
        val bold = style?.get("bold") as? Boolean ?: false
        val newline = style?.get("newline") as? Boolean ?: true

        return Bundle().apply {
            putInt(KEY_ALIGN, align)
            putInt(KEY_FONT, font)
            putBoolean(KEY_FONT_BOLD, bold)
            putBoolean(KEY_NEWLINE, newline)
            val lineHeight = (style?.get("lineHeight") as? Number)?.toInt()
            if (lineHeight != null) {
                putInt(KEY_LINE_HEIGHT, lineHeight)
            }
            val fontName = style?.get("fontName") as? String
            if (!fontName.isNullOrBlank()) {
                putString(KEY_FONT_NAME, fontName)
            }
        }
    }

    private fun ensureSdkAvailable() {
        if (!isSdkAvailable()) {
            throw UrovoPluginException(
                errorCode = "sdk_not_found",
                message = "Urovo SDK classes were not found. Add urovoSDK*.aar to your Android app module.",
            )
        }
    }

    private fun ensurePrinterProvider(): Any {
        printerProvider?.let { return it }

        val providerClass = Class.forName(PRINTER_PROVIDER_CLASS)
        val provider = runCatching {
            val getInstanceMethod = providerClass.getMethod("getInstance", Context::class.java)
            getInstanceMethod.invoke(null, appContext)
        }.getOrElse {
            providerClass.getDeclaredConstructor().newInstance()
        }

        if (provider == null) {
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Unable to instantiate PrinterProviderImpl.",
            )
        }

        printerProvider = provider
        return provider
    }

    private fun requireInitializedPrinterProvider(operationName: String): Any {
        ensureSdkAvailable()
        if (!isPrinterInitialized) {
            throw UrovoPluginException(
                errorCode = "not_initialized",
                message = "Printer is not initialized. Call printerInit() before $operationName.",
            )
        }

        return printerProvider ?: throw UrovoPluginException(
            errorCode = "internal",
            message = "Printer provider is unavailable. Call printerInit() again.",
        )
    }

    private fun invokeUnit(target: Any, methodName: String, vararg args: Any?) {
        invoke(target, methodName, *args)
    }

    private fun invokeInt(target: Any, methodName: String, vararg args: Any?): Int {
        val value = invoke(target, methodName, *args)
        return when (value) {
            is Int -> value
            is Number -> value.toInt()
            null -> STATUS_OK
            else -> throw UrovoPluginException(
                errorCode = "internal",
                message = "Method $methodName returned a non-numeric value.",
            )
        }
    }

    private fun invoke(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = findMethod(target.javaClass, methodName, args)
            ?: throw UrovoPluginException(
                errorCode = "internal",
                message = "Method $methodName is not available in Urovo SDK.",
            )

        return try {
            method.isAccessible = true
            method.invoke(target, *args)
        } catch (error: InvocationTargetException) {
            val rootCause = error.targetException ?: error.cause
            val detail = rootCause?.message ?: rootCause?.javaClass?.simpleName ?: "Unknown error."
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Failed to invoke $methodName: $detail",
            )
        } catch (error: Throwable) {
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Failed to invoke $methodName: ${error.message ?: "Unknown error."}",
            )
        }
    }

    private fun findMethod(
        clazz: Class<*>,
        methodName: String,
        args: Array<out Any?>,
    ): Method? {
        return (clazz.methods + clazz.declaredMethods).firstOrNull { method ->
            method.name == methodName &&
                method.parameterTypes.size == args.size &&
                method.parameterTypes.indices.all { index ->
                    isCompatible(method.parameterTypes[index], args[index])
                }
        }
    }

    private fun isCompatible(parameterType: Class<*>, arg: Any?): Boolean {
        if (arg == null) {
            return !parameterType.isPrimitive
        }

        val normalizedType = if (parameterType.isPrimitive) {
            primitiveWrapper(parameterType)
        } else {
            parameterType
        }

        return normalizedType.isAssignableFrom(arg.javaClass)
    }

    private fun primitiveWrapper(primitiveType: Class<*>): Class<*> {
        return when (primitiveType) {
            java.lang.Integer.TYPE -> java.lang.Integer::class.java
            java.lang.Long.TYPE -> java.lang.Long::class.java
            java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
            java.lang.Float.TYPE -> java.lang.Float::class.java
            java.lang.Double.TYPE -> java.lang.Double::class.java
            java.lang.Short.TYPE -> java.lang.Short::class.java
            java.lang.Byte.TYPE -> java.lang.Byte::class.java
            java.lang.Character.TYPE -> java.lang.Character::class.java
            else -> primitiveType
        }
    }

    private fun resolveBarcodeFormat(type: String?): Any? {
        val valueName = when (type) {
            "code39" -> "CODE_39"
            "ean13" -> "EAN_13"
            "ean8" -> "EAN_8"
            else -> "CODE_128"
        }

        return runCatching {
            val formatClass = Class.forName(BARCODE_FORMAT_CLASS)
            val valueOfMethod = formatClass.getMethod("valueOf", String::class.java)
            valueOfMethod.invoke(null, valueName)
        }.getOrNull()
    }

    private fun alignValue(align: String?): Int {
        return when (align) {
            "center" -> ALIGN_CENTER
            "right" -> ALIGN_RIGHT
            else -> ALIGN_LEFT
        }
    }

    private fun fontValue(font: String?): Int {
        return when (font) {
            "small" -> FONT_SMALL
            "large" -> FONT_LARGE
            else -> FONT_NORMAL
        }
    }

    private fun statusDetailForCode(code: Int): Map<String, Any> {
        val status = statusForCode(code)
        return mapOf(
            "status" to status,
            "rawCode" to code,
            "message" to statusMessageForCode(code),
            "recommendation" to recommendationForStatus(status),
            "retryable" to retryableForStatus(status),
        )
    }

    private fun statusForCode(code: Int): String {
        return when (code) {
            STATUS_OK -> "ok"
            STATUS_PAPER_ENDED -> "paperEnded"
            STATUS_HARD_ERROR -> "hardError"
            STATUS_OVERHEAT -> "overheat"
            STATUS_LOW_VOLTAGE -> "lowVoltage"
            STATUS_MOTOR_ERROR -> "motorError"
            STATUS_BUSY -> "busy"
            else -> "unknown"
        }
    }

    private fun statusMessageForCode(code: Int): String {
        if (code == STATUS_OK) {
            // Vendor SDK maps code 0 to a default "failed" string; keep success status explicit.
            return "Printer is ready."
        }

        val messageFromSdk = runCatching {
            val statusClass = Class.forName(PRINT_STATUS_CLASS)
            val getDescriptionMethod = statusClass.getMethod(
                "getPrinterStatusDes",
                Context::class.java,
                Int::class.javaPrimitiveType,
            )
            getDescriptionMethod.invoke(null, appContext, code) as? String
        }.getOrNull()

        if (!messageFromSdk.isNullOrBlank()) {
            return messageFromSdk
        }

        return when (code) {
            STATUS_OK -> "Printer is ready."
            STATUS_PAPER_ENDED -> "Printer is out of paper."
            STATUS_HARD_ERROR -> "Printer hardware error."
            STATUS_OVERHEAT -> "Printer is overheated."
            STATUS_LOW_VOLTAGE -> "Low battery voltage."
            STATUS_MOTOR_ERROR -> "Printer motor error."
            STATUS_BUSY -> "Printer is busy."
            else -> "Unknown printer status."
        }
    }

    private fun recommendationForStatus(status: String): String {
        return when (status) {
            "ok" -> "Continue printing."
            "paperEnded" -> "Insert paper and retry."
            "hardError" -> "Stop job and check hardware/driver."
            "overheat" -> "Wait for cooldown, then retry."
            "lowVoltage" -> "Charge device before retrying."
            "motorError" -> "Stop job and service device."
            "busy" -> "Wait a moment, then retry."
            else -> "Verify device state and retry if appropriate."
        }
    }

    private fun startPrintErrorCodeForStatus(code: Int): String {
        return when (code) {
            STATUS_PAPER_ENDED,
            STATUS_HARD_ERROR,
            STATUS_OVERHEAT,
            STATUS_LOW_VOLTAGE,
            STATUS_MOTOR_ERROR,
            STATUS_BUSY,
            -> "device_unavailable"
            else -> "print_failed"
        }
    }

    private fun retryableForStatus(status: String): Boolean {
        return when (status) {
            "ok", "paperEnded", "overheat", "busy" -> true
            else -> false
        }
    }

    private companion object {
        private const val PRINTER_PROVIDER_CLASS = "com.urovo.sdk.print.PrinterProviderImpl"
        private const val PRINT_STATUS_CLASS = "com.urovo.sdk.print.PrintStatus"
        private const val BARCODE_FORMAT_CLASS = "com.google.zxing.BarcodeFormat"

        private const val KEY_ALIGN = "align"
        private const val KEY_FONT = "font"
        private const val KEY_FONT_BOLD = "fontBold"
        private const val KEY_NEWLINE = "newline"
        private const val KEY_LINE_HEIGHT = "lineHeight"
        private const val KEY_FONT_NAME = "fontName"
        private const val KEY_WIDTH = "width"
        private const val KEY_HEIGHT = "height"
        private const val KEY_EXPECTED_HEIGHT = "expectedHeight"
        private const val KEY_BARCODE_TYPE = "barcode_type"

        private const val ALIGN_LEFT = 0
        private const val ALIGN_CENTER = 1
        private const val ALIGN_RIGHT = 2

        private const val FONT_SMALL = 0
        private const val FONT_NORMAL = 1
        private const val FONT_LARGE = 2

        private const val STATUS_OK = 0
        private const val STATUS_LOW_VOLTAGE = 225
        private const val STATUS_PAPER_ENDED = 240
        private const val STATUS_HARD_ERROR = 242
        private const val STATUS_OVERHEAT = 243
        private const val STATUS_BUSY = 247
        private const val STATUS_MOTOR_ERROR = 251
    }
}
