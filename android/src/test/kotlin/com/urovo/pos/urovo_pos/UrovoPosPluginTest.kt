package com.urovo.pos.urovo_pos

import com.urovo.pos.urovo_pos.printer.UrovoPrinterApi
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class UrovoPosPluginTest {
    @Test
    fun onMethodCall_unknownMethod_returnsNotImplemented() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("unknownMethod", null), result)

        assertTrue(result.notImplementedCalled)
        assertEquals(null, result.successValue)
    }

    @Test
    fun onMethodCall_isSdkAvailable_returnsOkResponse() {
        val bridge = FakePrinterBridge().apply { sdkAvailable = true }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("isUrovoSdkAvailable", null), result)

        assertOkResponse(result, true)
    }

    @Test
    fun onMethodCall_printerInit_callsBridgeAndReturnsOk() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerInit", null), result)

        assertEquals(1, bridge.initCalls)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_printerGetStatus_returnsStatusString() {
        val statusDetail = mapOf(
            "status" to "paperEnded",
            "rawCode" to 240,
            "message" to "out of paper",
            "recommendation" to "insert paper",
            "retryable" to true,
        )
        val bridge = FakePrinterBridge().apply { statusDetailResult = statusDetail }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerGetStatus", null), result)

        assertEquals(1, bridge.statusCalls)
        assertOkResponse(result, "paperEnded")
    }

    @Test
    fun onMethodCall_printerGetStatusDetail_returnsDetailMap() {
        val statusDetail = mapOf(
            "status" to "busy",
            "rawCode" to 247,
            "message" to "busy",
            "recommendation" to "wait",
            "retryable" to true,
        )
        val bridge = FakePrinterBridge().apply { statusDetailResult = statusDetail }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerGetStatusDetail", null), result)

        assertEquals(1, bridge.statusCalls)
        assertOkResponse(result, statusDetail)
    }

    @Test
    fun onMethodCall_printerSetGray_withValidArgument_callsBridge() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerSetGray", mapOf("gray" to 8)), result)

        assertEquals(1, bridge.setGrayCalls)
        assertEquals(8, bridge.lastGrayLevel)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_printerSetGray_withInvalidValueType_returnsInvalidArgument() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerSetGray", mapOf("gray" to "x")), result)

        assertErrorResponse(
            result = result,
            code = "invalid_argument",
            message = "gray must be a number.",
            data = null,
        )
    }

    @Test
    fun onMethodCall_printerSetGray_withNonMapArguments_returnsInvalidArgument() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerSetGray", "not-map"), result)

        assertErrorResponse(
            result = result,
            code = "invalid_argument",
            message = "Method arguments must be a map.",
            data = null,
        )
    }

    @Test
    fun onMethodCall_printerStartPrint_returnsStartResult() {
        val startResult = mapOf(
            "status" to "ok",
            "rawCode" to 0,
            "message" to "ready",
            "recommendation" to "continue",
            "retryable" to true,
        )
        val bridge = FakePrinterBridge().apply { startPrintResult = startResult }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerStartPrint", null), result)

        assertEquals(1, bridge.startPrintCalls)
        assertOkResponse(result, startResult)
    }

    @Test
    fun onMethodCall_printerRunJob_withMap_callsBridgeAndReturnsData() {
        val runJobArgs = mapOf<String, Any?>(
            "gray" to 8,
            "commands" to listOf(mapOf("type" to "text", "text" to "Hello")),
        )
        val runJobResult = mapOf(
            "status" to "ok",
            "rawCode" to 0,
            "message" to "ready",
            "recommendation" to "continue",
            "retryable" to true,
        )
        val bridge = FakePrinterBridge().apply { runJobResultValue = runJobResult }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerRunJob", runJobArgs), result)

        assertEquals(1, bridge.runJobCalls)
        assertEquals(runJobArgs, bridge.lastRunJobArguments)
        assertOkResponse(result, runJobResult)
    }

    @Test
    fun onMethodCall_printerRunJob_withNonMapArguments_returnsInvalidArgument() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerRunJob", null), result)

        assertErrorResponse(
            result = result,
            code = "invalid_argument",
            message = "Method arguments must be a map.",
            data = null,
        )
    }

    @Test
    fun onMethodCall_printerClose_callsBridgeAndReturnsOk() {
        val bridge = FakePrinterBridge()
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerClose", null), result)

        assertEquals(1, bridge.closeCalls)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_whenBridgeThrowsPluginException_returnsErrorResponse() {
        val details = mapOf(
            "statusDetail" to mapOf(
                "status" to "paperEnded",
                "rawCode" to 240,
            ),
        )
        val bridge = FakePrinterBridge().apply {
            initError = UrovoPluginException(
                errorCode = "device_unavailable",
                message = "printer init failed",
                details = details,
            )
        }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerInit", null), result)

        assertErrorResponse(
            result = result,
            code = "device_unavailable",
            message = "printer init failed",
            data = details,
        )
    }

    @Test
    fun onMethodCall_whenBridgeThrowsUnexpectedThrowable_returnsInternalError() {
        val bridge = FakePrinterBridge().apply {
            initError = IllegalStateException("boom")
        }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerInit", null), result)

        assertErrorResponse(
            result = result,
            code = "internal",
            message = "boom",
            data = null,
        )
    }

    private fun assertOkResponse(result: CapturingResult, expectedData: Any?) {
        val map = result.requireSuccessMap()
        assertEquals("ok", map["code"])
        assertEquals("OK", map["message"])
        assertEquals(expectedData, map["data"])
        assertFalse(result.notImplementedCalled)
    }

    private fun assertErrorResponse(
        result: CapturingResult,
        code: String,
        message: String,
        data: Any?,
    ) {
        val map = result.requireSuccessMap()
        assertEquals(code, map["code"])
        assertEquals(message, map["message"])
        assertEquals(data, map["data"])
        assertFalse(result.notImplementedCalled)
    }
}

private class FakePrinterBridge : UrovoPrinterApi {
    var sdkAvailable: Boolean = true

    var initCalls: Int = 0
    var closeCalls: Int = 0
    var statusCalls: Int = 0
    var setGrayCalls: Int = 0
    var startPrintCalls: Int = 0
    var runJobCalls: Int = 0

    var lastGrayLevel: Int? = null
    var lastRunJobArguments: Map<String, Any?>? = null

    var statusDetailResult: Map<String, Any> = mapOf(
        "status" to "ok",
        "rawCode" to 0,
        "message" to "ready",
        "recommendation" to "continue",
        "retryable" to true,
    )
    var startPrintResult: Map<String, Any> = mapOf(
        "status" to "ok",
        "rawCode" to 0,
        "message" to "ready",
        "recommendation" to "continue",
        "retryable" to true,
    )
    var runJobResultValue: Map<String, Any> = mapOf(
        "status" to "ok",
        "rawCode" to 0,
        "message" to "ready",
        "recommendation" to "continue",
        "retryable" to true,
    )

    var initError: Throwable? = null
    var closeError: Throwable? = null
    var statusError: Throwable? = null
    var setGrayError: Throwable? = null
    var startPrintError: Throwable? = null
    var runJobError: Throwable? = null

    override fun isSdkAvailable(): Boolean {
        return sdkAvailable
    }

    override fun printerInit() {
        initCalls += 1
        initError?.let { throw it }
    }

    override fun printerClose() {
        closeCalls += 1
        closeError?.let { throw it }
    }

    override fun printerGetStatusDetail(): Map<String, Any> {
        statusCalls += 1
        statusError?.let { throw it }
        return statusDetailResult
    }

    override fun printerSetGray(level: Int) {
        setGrayCalls += 1
        lastGrayLevel = level
        setGrayError?.let { throw it }
    }

    override fun printerStartPrint(): Map<String, Any> {
        startPrintCalls += 1
        startPrintError?.let { throw it }
        return startPrintResult
    }

    override fun printerRunJob(arguments: Map<String, Any?>): Map<String, Any> {
        runJobCalls += 1
        lastRunJobArguments = arguments
        runJobError?.let { throw it }
        return runJobResultValue
    }
}

private class CapturingResult : MethodChannel.Result {
    var successValue: Any? = null
        private set
    var errorValue: Triple<String?, String?, Any?>? = null
        private set
    var notImplementedCalled: Boolean = false
        private set

    override fun success(result: Any?) {
        successValue = result
    }

    override fun error(
        errorCode: String,
        errorMessage: String?,
        errorDetails: Any?,
    ) {
        errorValue = Triple(errorCode, errorMessage, errorDetails)
    }

    override fun notImplemented() {
        notImplementedCalled = true
    }

    fun requireSuccessMap(): Map<String, Any?> {
        assertEquals(null, errorValue)
        val map = successValue as? Map<*, *>
        assertNotNull(map)
        @Suppress("UNCHECKED_CAST")
        return map as Map<String, Any?>
    }
}
