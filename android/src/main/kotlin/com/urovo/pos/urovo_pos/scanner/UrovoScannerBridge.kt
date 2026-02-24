package com.urovo.pos.urovo_pos.scanner

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.urovo.pos.urovo_pos.UrovoPluginException
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class UrovoScannerBridge(
    private val appContext: Context,
) : UrovoScannerApi {
    private var scannerInstance: Any? = null
    private var eventCallback: ((Map<String, Any?>) -> Unit)? = null
    private var scannerListenerProxy: Any? = null
    private var foregroundContext: Context? = null

    override fun scannerStart(cameraId: Int, timeoutMs: Long) {
        if (cameraId < 0) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "cameraId must be >= 0.",
            )
        }
        if (timeoutMs < 0) {
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "timeoutMs must be >= 0.",
            )
        }

        ensureScannerAvailable()
        val scanner = ensureScannerInstance()
        val listener = ensureScannerListenerProxy()
        val startContext = foregroundContext
            ?: throw UrovoPluginException(
                errorCode = "device_unavailable",
                message = "Scanner requires an attached foreground Activity. Ensure the app is visible before scannerStart().",
            )
        val bundle = Bundle()
        val sdkTimeoutSeconds = timeoutMsToSdkTimeoutSeconds(timeoutMs)
        invokeUnit(
            scanner,
            "startScan",
            startContext,
            bundle,
            cameraId,
            sdkTimeoutSeconds,
            listener,
        )
    }

    override fun scannerStop() {
        if (!hasClass(SCANNER_IMPL_CLASS)) {
            return
        }

        val scanner = scannerInstance ?: return
        try {
            invokeUnit(scanner, "stopScan")
        } finally {
            dismissCaptureDialogIfShowing(scanner)
        }
    }

    override fun setEventCallback(callback: ((Map<String, Any?>) -> Unit)?) {
        eventCallback = callback
    }

    override fun setForegroundContext(context: Context?) {
        foregroundContext = context
    }

    private fun ensureScannerAvailable() {
        if (!hasClass(SCANNER_IMPL_CLASS) || !hasClass(SCANNER_LISTENER_CLASS)) {
            throw UrovoPluginException(
                errorCode = "sdk_not_found",
                message = "Urovo scanner SDK classes were not found. Add urovoSDK*.aar to your Android app module.",
            )
        }
    }

    private fun ensureScannerInstance(): Any {
        scannerInstance?.let { return it }

        val clazz = Class.forName(SCANNER_IMPL_CLASS)
        val instance = runCatching {
            val getInstanceMethod = clazz.getMethod("getInstance", Context::class.java)
            getInstanceMethod.invoke(null, appContext)
        }.recoverCatching {
            clazz.getDeclaredConstructor(Context::class.java).newInstance(appContext)
        }.getOrElse { error ->
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Unable to instantiate scanner bridge: ${error.message ?: "Unknown error."}",
            )
        }

        if (instance == null) {
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Scanner instance is null.",
            )
        }

        scannerInstance = instance
        return instance
    }

    private fun ensureScannerListenerProxy(): Any {
        scannerListenerProxy?.let { return it }

        val listenerClass = Class.forName(SCANNER_LISTENER_CLASS)
        val proxy = Proxy.newProxyInstance(
            listenerClass.classLoader,
            arrayOf(listenerClass),
            ScannerListenerInvocationHandler(
                emitEvent = ::emitScannerEvent,
                closeScannerUi = {
                    scannerInstance?.let(::dismissCaptureDialogIfShowing)
                },
            ),
        )
        scannerListenerProxy = proxy
        return proxy
    }

    private fun dismissCaptureDialogIfShowing(scanner: Any) {
        val dismissAction = Runnable {
            runCatching {
                val field = scanner.javaClass.getDeclaredField("captureDialog")
                field.isAccessible = true
                val dialog = field.get(scanner) ?: return@Runnable
                val isShowingMethod = dialog.javaClass.getMethod("isShowing")
                val isShowing = (isShowingMethod.invoke(dialog) as? Boolean) == true
                if (isShowing) {
                    dialog.javaClass.getMethod("dismiss").invoke(dialog)
                }
            }
        }

        if (Looper.myLooper() == Looper.getMainLooper()) {
            dismissAction.run()
        } else {
            Handler(Looper.getMainLooper()).post(dismissAction)
        }
    }

    private fun emitScannerEvent(event: Map<String, Any?>) {
        eventCallback?.invoke(event)
    }

    private fun hasClass(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    private fun invokeUnit(target: Any, methodName: String, vararg args: Any?) {
        invoke(target, methodName, *args)
    }

    private fun invoke(target: Any, methodName: String, vararg args: Any?): Any? {
        val method = findMethod(target.javaClass, methodName, args)
            ?: throw UrovoPluginException(
                errorCode = "internal",
                message = "Method $methodName is not available in Urovo scanner SDK.",
            )

        return try {
            method.isAccessible = true
            method.invoke(target, *args)
        } catch (error: InvocationTargetException) {
            val rootCause = error.targetException ?: error.cause
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Failed to invoke $methodName: ${rootCause?.message ?: "Unknown error."}",
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

    private fun timeoutMsToSdkTimeoutSeconds(timeoutMs: Long): Long {
        if (timeoutMs == 0L) {
            return 0L
        }
        // Urovo SDK expects timeout in seconds; Dart API sends milliseconds.
        return ((timeoutMs + 999L) / 1000L).coerceAtLeast(1L)
    }

    private companion object {
        private const val SCANNER_IMPL_CLASS = "com.urovo.sdk.scanner.InnerScannerImpl"
        private const val SCANNER_LISTENER_CLASS = "com.urovo.sdk.scanner.listener.ScannerListener"
    }
}

private class ScannerListenerInvocationHandler(
    private val emitEvent: (Map<String, Any?>) -> Unit,
    private val closeScannerUi: (() -> Unit)? = null,
) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val safeArgs = args ?: emptyArray()
        val timestampMs = System.currentTimeMillis()

        when (method.name) {
            "onSuccess" -> {
                val text = safeArgs.getOrNull(0) as? String ?: ""
                val bytes = safeArgs.getOrNull(1) as? ByteArray
                runCatching {
                    closeScannerUi?.invoke()
                }
                emitEvent(
                    mapOf(
                        "type" to "decoded",
                        "data" to text,
                        "rawBytesBase64" to bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) },
                        "timestampMs" to timestampMs,
                    ),
                )
            }

            "onError" -> {
                emitEvent(
                    mapOf(
                        "type" to "error",
                        "errorCode" to (safeArgs.getOrNull(0) as? Number)?.toInt(),
                        "message" to (safeArgs.getOrNull(1) as? String ?: "Scanner error."),
                        "timestampMs" to timestampMs,
                    ),
                )
            }

            "onTimeout" -> {
                emitEvent(
                    mapOf(
                        "type" to "timeout",
                        "timestampMs" to timestampMs,
                    ),
                )
            }

            "onCancel" -> {
                emitEvent(
                    mapOf(
                        "type" to "cancel",
                        "timestampMs" to timestampMs,
                    ),
                )
            }
        }

        return null
    }
}
