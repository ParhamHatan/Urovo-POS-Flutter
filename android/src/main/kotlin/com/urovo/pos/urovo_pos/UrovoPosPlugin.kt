package com.urovo.pos.urovo_pos

import android.content.Context
import com.urovo.pos.urovo_pos.beeper.UrovoBeeperApi
import com.urovo.pos.urovo_pos.beeper.UrovoBeeperBridge
import com.urovo.pos.urovo_pos.device.UrovoDeviceApi
import com.urovo.pos.urovo_pos.device.UrovoDeviceBridge
import com.urovo.pos.urovo_pos.printer.UrovoPrinterApi
import com.urovo.pos.urovo_pos.printer.UrovoPrinterBridge
import com.urovo.pos.urovo_pos.scanner.UrovoScannerApi
import com.urovo.pos.urovo_pos.scanner.UrovoScannerBridge
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class UrovoPosPlugin() : FlutterPlugin, MethodCallHandler, EventChannel.StreamHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private lateinit var scannerEventChannel: EventChannel
    private lateinit var appContext: Context
    private lateinit var printerBridge: UrovoPrinterApi
    private lateinit var scannerBridge: UrovoScannerApi
    private lateinit var beeperBridge: UrovoBeeperApi
    private lateinit var deviceBridge: UrovoDeviceApi
    private var foregroundContext: Context? = null
    private var scannerEventSink: EventChannel.EventSink? = null

    internal constructor(testPrinterBridge: UrovoPrinterApi) : this() {
        printerBridge = testPrinterBridge
        scannerBridge = NoopScannerBridge()
        beeperBridge = NoopBeeperBridge()
        deviceBridge = NoopDeviceBridge(sdkAvailable = true)
    }

    internal constructor(
        testPrinterBridge: UrovoPrinterApi,
        testScannerBridge: UrovoScannerApi,
    ) : this() {
        printerBridge = testPrinterBridge
        scannerBridge = testScannerBridge
        beeperBridge = NoopBeeperBridge()
        deviceBridge = NoopDeviceBridge(sdkAvailable = true)
    }

    internal constructor(
        testPrinterBridge: UrovoPrinterApi,
        testScannerBridge: UrovoScannerApi,
        testBeeperBridge: UrovoBeeperApi,
        testDeviceBridge: UrovoDeviceApi,
    ) : this() {
        printerBridge = testPrinterBridge
        scannerBridge = testScannerBridge
        beeperBridge = testBeeperBridge
        deviceBridge = testDeviceBridge
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        printerBridge = UrovoPrinterBridge(appContext)
        scannerBridge = UrovoScannerBridge(appContext)
        beeperBridge = UrovoBeeperBridge()
        deviceBridge = UrovoDeviceBridge()
        scannerBridge.setEventCallback(::emitScannerEvent)
        scannerBridge.setForegroundContext(foregroundContext)
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
        scannerEventChannel = EventChannel(binding.binaryMessenger, SCANNER_EVENT_CHANNEL_NAME)
        scannerEventChannel.setStreamHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        scannerEventChannel.setStreamHandler(null)
        scannerBridge.setEventCallback(null)
        scannerBridge.setForegroundContext(null)
        beeperBridge.beeperStop()
        scannerEventSink = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val response = try {
            when (call.method) {
                METHOD_IS_SDK_AVAILABLE -> {
                    UrovoPluginResponse.ok(data = deviceBridge.isSdkAvailable())
                }

                METHOD_DEVICE_GET_STATUS -> {
                    UrovoPluginResponse.ok(data = deviceBridge.deviceGetStatus())
                }

                METHOD_PRINTER_INIT -> {
                    printerBridge.printerInit()
                    UrovoPluginResponse.ok()
                }

                METHOD_PRINTER_GET_STATUS -> {
                    val statusDetail = printerBridge.printerGetStatusDetail()
                    UrovoPluginResponse.ok(data = statusDetail["status"])
                }

                METHOD_PRINTER_GET_STATUS_DETAIL -> {
                    UrovoPluginResponse.ok(data = printerBridge.printerGetStatusDetail())
                }

                METHOD_PRINTER_SET_GRAY -> {
                    val args = requireArgumentsMap(call.arguments)
                    val level = (args["gray"] as? Number)?.toInt()
                        ?: throw UrovoPluginException(
                            errorCode = "invalid_argument",
                            message = "gray must be a number.",
                        )
                    printerBridge.printerSetGray(level)
                    UrovoPluginResponse.ok()
                }

                METHOD_PRINTER_START_PRINT -> {
                    UrovoPluginResponse.ok(data = printerBridge.printerStartPrint())
                }

                METHOD_PRINTER_RUN_JOB -> {
                    val args = requireArgumentsMap(call.arguments)
                    UrovoPluginResponse.ok(data = printerBridge.printerRunJob(args))
                }

                METHOD_PRINTER_CLOSE -> {
                    printerBridge.printerClose()
                    UrovoPluginResponse.ok()
                }

                METHOD_SCANNER_START -> {
                    val args = optionalArgumentsMap(call.arguments)
                    val cameraId = parseOptionalInt(args["cameraId"], "cameraId") ?: 0
                    val timeoutMs = parseOptionalLong(args["timeoutMs"], "timeoutMs") ?: 10_000L
                    scannerBridge.scannerStart(cameraId = cameraId, timeoutMs = timeoutMs)
                    UrovoPluginResponse.ok()
                }

                METHOD_SCANNER_STOP -> {
                    scannerBridge.scannerStop()
                    UrovoPluginResponse.ok()
                }

                METHOD_BEEPER_BEEP -> {
                    val args = optionalArgumentsMap(call.arguments)
                    val pattern = (args["pattern"] as? String) ?: "short"
                    val repeat = parseOptionalInt(args["repeat"], "repeat") ?: 1
                    val durationMs = parseOptionalInt(args["durationMs"], "durationMs") ?: 120
                    val intervalMs = parseOptionalInt(args["intervalMs"], "intervalMs") ?: 80
                    val volume = parseOptionalDouble(args["volume"], "volume") ?: 1.0
                    beeperBridge.beeperBeep(
                        pattern = pattern,
                        repeat = repeat,
                        durationMs = durationMs,
                        intervalMs = intervalMs,
                        volume = volume,
                    )
                    UrovoPluginResponse.ok()
                }

                METHOD_BEEPER_STOP -> {
                    beeperBridge.beeperStop()
                    UrovoPluginResponse.ok()
                }

                else -> {
                    result.notImplemented()
                    return
                }
            }
        } catch (error: UrovoPluginException) {
            UrovoPluginResponse.error(
                code = error.errorCode,
                message = error.message,
                data = error.details,
            )
        } catch (error: Throwable) {
            UrovoPluginResponse.error(
                code = "internal",
                message = error.message ?: "Unexpected plugin failure.",
            )
        }

        result.success(response.toMap())
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        scannerEventSink = events
    }

    override fun onCancel(arguments: Any?) {
        scannerEventSink = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        foregroundContext = binding.activity
        scannerBridge.setForegroundContext(foregroundContext)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        foregroundContext = null
        scannerBridge.setForegroundContext(null)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        foregroundContext = binding.activity
        scannerBridge.setForegroundContext(foregroundContext)
    }

    override fun onDetachedFromActivity() {
        foregroundContext = null
        scannerBridge.setForegroundContext(null)
    }

    private fun requireArgumentsMap(arguments: Any?): Map<String, Any?> {
        val map = arguments as? Map<*, *>
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "Method arguments must be a map.",
            )

        return map.entries.associate { (key, value) -> key.toString() to value }
    }

    private fun optionalArgumentsMap(arguments: Any?): Map<String, Any?> {
        if (arguments == null) {
            return emptyMap()
        }
        return requireArgumentsMap(arguments)
    }

    private fun parseOptionalInt(value: Any?, fieldName: String): Int? {
        if (value == null) {
            return null
        }
        return (value as? Number)?.toInt()
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "$fieldName must be a number.",
            )
    }

    private fun parseOptionalLong(value: Any?, fieldName: String): Long? {
        if (value == null) {
            return null
        }
        return (value as? Number)?.toLong()
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "$fieldName must be a number.",
            )
    }

    private fun parseOptionalDouble(value: Any?, fieldName: String): Double? {
        if (value == null) {
            return null
        }
        return (value as? Number)?.toDouble()
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "$fieldName must be a number.",
            )
    }

    private fun emitScannerEvent(event: Map<String, Any?>) {
        scannerEventSink?.success(event)
    }

    private companion object {
        private const val CHANNEL_NAME = "urovo_pos/methods"
        private const val SCANNER_EVENT_CHANNEL_NAME = "urovo_pos/scanner_events"

        private const val METHOD_IS_SDK_AVAILABLE = "isUrovoSdkAvailable"
        private const val METHOD_DEVICE_GET_STATUS = "deviceGetStatus"
        private const val METHOD_PRINTER_INIT = "printerInit"
        private const val METHOD_PRINTER_GET_STATUS = "printerGetStatus"
        private const val METHOD_PRINTER_GET_STATUS_DETAIL = "printerGetStatusDetail"
        private const val METHOD_PRINTER_SET_GRAY = "printerSetGray"
        private const val METHOD_PRINTER_START_PRINT = "printerStartPrint"
        private const val METHOD_PRINTER_RUN_JOB = "printerRunJob"
        private const val METHOD_PRINTER_CLOSE = "printerClose"
        private const val METHOD_SCANNER_START = "scannerStart"
        private const val METHOD_SCANNER_STOP = "scannerStop"
        private const val METHOD_BEEPER_BEEP = "beeperBeep"
        private const val METHOD_BEEPER_STOP = "beeperStop"
    }
}

private class NoopScannerBridge : UrovoScannerApi {
    override fun scannerStart(cameraId: Int, timeoutMs: Long) {}

    override fun scannerStop() {}

    override fun setEventCallback(callback: ((Map<String, Any?>) -> Unit)?) {}

    override fun setForegroundContext(context: Context?) {}
}

private class NoopBeeperBridge : UrovoBeeperApi {
    override fun beeperBeep(
        pattern: String,
        repeat: Int,
        durationMs: Int,
        intervalMs: Int,
        volume: Double,
    ) {}

    override fun beeperStop() {}
}

private class NoopDeviceBridge(
    private val sdkAvailable: Boolean,
) : UrovoDeviceApi {
    override fun isSdkAvailable(): Boolean {
        return sdkAvailable
    }

    override fun deviceGetStatus(): Map<String, Any?> {
        return mapOf(
            "deviceManagerAvailable" to false,
            "manufacturer" to "",
            "brand" to "",
            "model" to "",
            "device" to "",
            "androidVersion" to "",
            "androidSdkInt" to 0,
            "serialNumber" to null,
            "tidSerialNumber" to null,
            "docked" to null,
            "timestampMs" to System.currentTimeMillis(),
        )
    }
}
