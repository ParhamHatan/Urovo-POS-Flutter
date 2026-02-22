package com.urovo.pos.urovo_pos

internal data class UrovoPluginResponse(
    val code: String,
    val message: String,
    val data: Any? = null,
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "code" to code,
            "message" to message,
            "data" to data,
        )
    }

    companion object {
        private const val OK_CODE = "ok"

        fun ok(data: Any? = null, message: String = "OK"): UrovoPluginResponse {
            return UrovoPluginResponse(code = OK_CODE, message = message, data = data)
        }

        fun error(code: String, message: String, data: Any? = null): UrovoPluginResponse {
            return UrovoPluginResponse(code = code, message = message, data = data)
        }
    }
}

internal class UrovoPluginException(
    val errorCode: String,
    override val message: String,
    val details: Any? = null,
) : RuntimeException(message)
