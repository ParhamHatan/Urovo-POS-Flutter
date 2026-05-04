package com.urovo.pos.urovo_pos

import android.content.Context
import com.urovo.pos.urovo_pos.beeper.UrovoBeeperApi
import com.urovo.pos.urovo_pos.device.UrovoDeviceApi
import com.urovo.pos.urovo_pos.printer.UrovoPrinterApi
import com.urovo.pos.urovo_pos.scanner.UrovoScannerApi
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
        val deviceBridge = FakeDeviceBridge().apply { sdkAvailable = true }
        val plugin = UrovoPosPlugin(
            FakePrinterBridge(),
            FakeScannerBridge(),
            FakeBeeperBridge(),
            deviceBridge,
        )
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("isUrovoSdkAvailable", null), result)

        assertOkResponse(result, true)
    }

    @Test
    fun onMethodCall_deviceGetStatus_returnsDeviceStatusMap() {
        val deviceStatus = mapOf<String, Any?>(
            "deviceManagerAvailable" to true,
            "manufacturer" to "Urovo",
            "brand" to "Urovo",
            "model" to "i9000S",
            "device" to "i9000s",
            "androidVersion" to "11",
            "androidSdkInt" to 30,
            "serialNumber" to "SN123",
            "tidSerialNumber" to "TID123",
            "docked" to false,
            "timestampMs" to 1710000000000L,
        )
        val deviceBridge = FakeDeviceBridge().apply { statusResult = deviceStatus }
        val plugin = UrovoPosPlugin(
            FakePrinterBridge(),
            FakeScannerBridge(),
            FakeBeeperBridge(),
            deviceBridge,
        )
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("deviceGetStatus", null), result)

        assertEquals(1, deviceBridge.statusCalls)
        assertOkResponse(result, deviceStatus)
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
    fun onMethodCall_printerClose_whenBridgeThrowsPluginException_returnsErrorResponse() {
        val details = mapOf(
            "statusDetail" to mapOf(
                "status" to "paperEnded",
                "rawCode" to 240,
            ),
        )
        val bridge = FakePrinterBridge().apply {
            closeError = UrovoPluginException(
                errorCode = "device_unavailable",
                message = "close failed with status code 240.",
                details = details,
            )
        }
        val plugin = UrovoPosPlugin(bridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("printerClose", null), result)

        assertEquals(1, bridge.closeCalls)
        assertErrorResponse(
            result = result,
            code = "device_unavailable",
            message = "close failed with status code 240.",
            data = details,
        )
    }

    @Test
    fun onMethodCall_scannerStart_withDefaults_callsScannerBridge() {
        val printerBridge = FakePrinterBridge()
        val scannerBridge = FakeScannerBridge()
        val plugin = UrovoPosPlugin(printerBridge, scannerBridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("scannerStart", null), result)

        assertEquals(1, scannerBridge.startCalls)
        assertEquals(0, scannerBridge.lastCameraId)
        assertEquals(10_000L, scannerBridge.lastTimeoutMs)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_scannerStart_withArgs_callsScannerBridge() {
        val printerBridge = FakePrinterBridge()
        val scannerBridge = FakeScannerBridge()
        val plugin = UrovoPosPlugin(printerBridge, scannerBridge)
        val result = CapturingResult()

        plugin.onMethodCall(
            MethodCall(
                "scannerStart",
                mapOf("cameraId" to 1, "timeoutMs" to 5500),
            ),
            result,
        )

        assertEquals(1, scannerBridge.startCalls)
        assertEquals(1, scannerBridge.lastCameraId)
        assertEquals(5500L, scannerBridge.lastTimeoutMs)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_scannerStart_withInvalidArgs_returnsInvalidArgument() {
        val printerBridge = FakePrinterBridge()
        val scannerBridge = FakeScannerBridge()
        val plugin = UrovoPosPlugin(printerBridge, scannerBridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("scannerStart", mapOf("timeoutMs" to "x")), result)

        assertErrorResponse(
            result = result,
            code = "invalid_argument",
            message = "timeoutMs must be a number.",
            data = null,
        )
    }

    @Test
    fun onMethodCall_scannerStop_callsScannerBridgeAndReturnsOk() {
        val printerBridge = FakePrinterBridge()
        val scannerBridge = FakeScannerBridge()
        val plugin = UrovoPosPlugin(printerBridge, scannerBridge)
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("scannerStop", null), result)

        assertEquals(1, scannerBridge.stopCalls)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_beeperBeep_withDefaults_callsBeeperBridge() {
        val beeperBridge = FakeBeeperBridge()
        val plugin = UrovoPosPlugin(
            FakePrinterBridge(),
            FakeScannerBridge(),
            beeperBridge,
            FakeDeviceBridge(),
        )
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("beeperBeep", null), result)

        assertEquals(1, beeperBridge.beepCalls)
        assertEquals("short", beeperBridge.lastPattern)
        assertEquals(1, beeperBridge.lastRepeat)
        assertEquals(120, beeperBridge.lastDurationMs)
        assertEquals(80, beeperBridge.lastIntervalMs)
        assertEquals(1.0, beeperBridge.lastVolume)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_beeperBeep_withArgs_callsBeeperBridge() {
        val beeperBridge = FakeBeeperBridge()
        val plugin = UrovoPosPlugin(
            FakePrinterBridge(),
            FakeScannerBridge(),
            beeperBridge,
            FakeDeviceBridge(),
        )
        val result = CapturingResult()

        plugin.onMethodCall(
            MethodCall(
                "beeperBeep",
                mapOf(
                    "pattern" to "warning",
                    "repeat" to 2,
                    "durationMs" to 200,
                    "intervalMs" to 50,
                    "volume" to 0.5,
                ),
            ),
            result,
        )

        assertEquals(1, beeperBridge.beepCalls)
        assertEquals("warning", beeperBridge.lastPattern)
        assertEquals(2, beeperBridge.lastRepeat)
        assertEquals(200, beeperBridge.lastDurationMs)
        assertEquals(50, beeperBridge.lastIntervalMs)
        assertEquals(0.5, beeperBridge.lastVolume)
        assertOkResponse(result, null)
    }

    @Test
    fun onMethodCall_beeperBeep_withInvalidArgs_returnsInvalidArgument() {
        val plugin = UrovoPosPlugin(
            FakePrinterBridge(),
            FakeScannerBridge(),
            FakeBeeperBridge(),
            FakeDeviceBridge(),
        )
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("beeperBeep", mapOf("volume" to "loud")), result)

        assertErrorResponse(
            result = result,
            code = "invalid_argument",
            message = "volume must be a number.",
            data = null,
        )
    }

    @Test
    fun onMethodCall_beeperStop_callsBeeperBridgeAndReturnsOk() {
        val beeperBridge = FakeBeeperBridge()
        val plugin = UrovoPosPlugin(
            FakePrinterBridge(),
            FakeScannerBridge(),
            beeperBridge,
            FakeDeviceBridge(),
        )
        val result = CapturingResult()

        plugin.onMethodCall(MethodCall("beeperStop", null), result)

        assertEquals(1, beeperBridge.stopCalls)
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

private class FakeScannerBridge : UrovoScannerApi {
    var startCalls: Int = 0
    var stopCalls: Int = 0
    var lastCameraId: Int? = null
    var lastTimeoutMs: Long? = null
    var startError: Throwable? = null
    var stopError: Throwable? = null

    override fun scannerStart(cameraId: Int, timeoutMs: Long) {
        startCalls += 1
        lastCameraId = cameraId
        lastTimeoutMs = timeoutMs
        startError?.let { throw it }
    }

    override fun scannerStop() {
        stopCalls += 1
        stopError?.let { throw it }
    }

    override fun setEventCallback(callback: ((Map<String, Any?>) -> Unit)?) {}

    override fun setForegroundContext(context: Context?) {}
}

private class FakeBeeperBridge : UrovoBeeperApi {
    var beepCalls: Int = 0
    var stopCalls: Int = 0
    var lastPattern: String? = null
    var lastRepeat: Int? = null
    var lastDurationMs: Int? = null
    var lastIntervalMs: Int? = null
    var lastVolume: Double? = null
    var beepError: Throwable? = null
    var stopError: Throwable? = null

    override fun beeperBeep(
        pattern: String,
        repeat: Int,
        durationMs: Int,
        intervalMs: Int,
        volume: Double,
    ) {
        beepCalls += 1
        lastPattern = pattern
        lastRepeat = repeat
        lastDurationMs = durationMs
        lastIntervalMs = intervalMs
        lastVolume = volume
        beepError?.let { throw it }
    }

    override fun beeperStop() {
        stopCalls += 1
        stopError?.let { throw it }
    }
}

private class FakeDeviceBridge : UrovoDeviceApi {
    var sdkAvailable: Boolean = true
    var statusCalls: Int = 0
    var statusResult: Map<String, Any?> = mapOf(
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
        "timestampMs" to 1710000000000L,
    )

    override fun isSdkAvailable(): Boolean {
        return sdkAvailable
    }

    override fun deviceGetStatus(): Map<String, Any?> {
        statusCalls += 1
        return statusResult
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
