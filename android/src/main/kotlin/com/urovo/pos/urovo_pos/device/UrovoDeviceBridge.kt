package com.urovo.pos.urovo_pos.device

import android.os.Build
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

internal class UrovoDeviceBridge : UrovoDeviceApi {
    override fun isSdkAvailable(): Boolean {
        return hasClass(PRINTER_PROVIDER_CLASS) ||
            hasClass(SCANNER_IMPL_CLASS) ||
            hasClass(DEVICE_MANAGER_CLASS)
    }

    override fun deviceGetStatus(): Map<String, Any?> {
        val deviceManagerAvailable = hasClass(DEVICE_MANAGER_CLASS)
        val deviceManager = if (deviceManagerAvailable) {
            instantiateDeviceManager()
        } else {
            null
        }

        return mapOf(
            "deviceManagerAvailable" to deviceManagerAvailable,
            "manufacturer" to Build.MANUFACTURER.orEmpty(),
            "brand" to Build.BRAND.orEmpty(),
            "model" to Build.MODEL.orEmpty(),
            "device" to Build.DEVICE.orEmpty(),
            "androidVersion" to Build.VERSION.RELEASE.orEmpty(),
            "androidSdkInt" to Build.VERSION.SDK_INT,
            "serialNumber" to deviceManager?.let { invokeStringOrNull(it, "getDeviceId") },
            "tidSerialNumber" to deviceManager?.let { invokeStringOrNull(it, "getTIDSN") },
            "docked" to deviceManager?.let { invokeBooleanOrNull(it, "getDockerState") },
            "timestampMs" to System.currentTimeMillis(),
        )
    }

    private fun instantiateDeviceManager(): Any? {
        return runCatching {
            Class.forName(DEVICE_MANAGER_CLASS).getDeclaredConstructor().newInstance()
        }.getOrNull()
    }

    private fun invokeStringOrNull(target: Any, methodName: String): String? {
        return runCatching {
            invoke(target, methodName) as? String
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun invokeBooleanOrNull(target: Any, methodName: String): Boolean? {
        return runCatching {
            invoke(target, methodName) as? Boolean
        }.getOrNull()
    }

    private fun invoke(target: Any, methodName: String): Any? {
        val method = findMethod(target.javaClass, methodName) ?: return null
        return try {
            method.isAccessible = true
            method.invoke(target)
        } catch (error: InvocationTargetException) {
            null
        } catch (error: Throwable) {
            null
        }
    }

    private fun findMethod(clazz: Class<*>, methodName: String): Method? {
        return (clazz.methods + clazz.declaredMethods).firstOrNull { method ->
            method.name == methodName && method.parameterTypes.isEmpty()
        }
    }

    private fun hasClass(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    private companion object {
        private const val PRINTER_PROVIDER_CLASS = "com.urovo.sdk.print.PrinterProviderImpl"
        private const val SCANNER_IMPL_CLASS = "com.urovo.sdk.scanner.InnerScannerImpl"
        private const val DEVICE_MANAGER_CLASS = "android.device.DeviceManager"
    }
}
