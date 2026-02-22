package com.urovo.pos.urovo_pos

import android.content.Context
import com.urovo.pos.urovo_pos.printer.UrovoPrinterApi
import com.urovo.pos.urovo_pos.printer.UrovoPrinterBridge
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class UrovoPosPlugin() : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var appContext: Context
    private lateinit var printerBridge: UrovoPrinterApi

    internal constructor(testPrinterBridge: UrovoPrinterApi) : this() {
        printerBridge = testPrinterBridge
    }

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        appContext = binding.applicationContext
        printerBridge = UrovoPrinterBridge(appContext)
        channel = MethodChannel(binding.binaryMessenger, CHANNEL_NAME)
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val response = try {
            when (call.method) {
                METHOD_IS_SDK_AVAILABLE -> {
                    UrovoPluginResponse.ok(data = printerBridge.isSdkAvailable())
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

    private fun requireArgumentsMap(arguments: Any?): Map<String, Any?> {
        val map = arguments as? Map<*, *>
            ?: throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "Method arguments must be a map.",
            )

        return map.entries.associate { (key, value) -> key.toString() to value }
    }

    private companion object {
        private const val CHANNEL_NAME = "urovo_pos/methods"

        private const val METHOD_IS_SDK_AVAILABLE = "isUrovoSdkAvailable"
        private const val METHOD_PRINTER_INIT = "printerInit"
        private const val METHOD_PRINTER_GET_STATUS = "printerGetStatus"
        private const val METHOD_PRINTER_GET_STATUS_DETAIL = "printerGetStatusDetail"
        private const val METHOD_PRINTER_SET_GRAY = "printerSetGray"
        private const val METHOD_PRINTER_START_PRINT = "printerStartPrint"
        private const val METHOD_PRINTER_RUN_JOB = "printerRunJob"
        private const val METHOD_PRINTER_CLOSE = "printerClose"
    }
}
