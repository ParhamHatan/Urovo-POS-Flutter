package com.urovo.pos.urovo_pos.printer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.util.Base64
import com.urovo.pos.urovo_pos.UrovoPluginException
import io.flutter.FlutterInjector
import java.io.File
import java.io.Serializable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

internal class UrovoPrinterBridge(
    private val appContext: Context,
) : UrovoPrinterApi {
    private var printerProvider: Any? = null
    private var isPrinterInitialized = false
    private val fontCacheDir: File by lazy { File(appContext.cacheDir, "urovo_fonts") }

    override fun isSdkAvailable(): Boolean {
        return hasClass(PRINTER_PROVIDER_CLASS)
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
        try {
            val closeCode = invokeInt(provider, "close")
            if (closeCode != STATUS_OK) {
                throw UrovoPluginException(
                    errorCode = "device_unavailable",
                    message = "close failed with status code $closeCode.",
                    details = mapOf("statusDetail" to statusDetailForCode(closeCode)),
                )
            }
        } finally {
            isPrinterInitialized = false
            printerProvider = null
        }
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
                if (styleBundle.containsKey(KEY_FONT_NAME)) {
                    appendTextBitmap(provider, text, styleBundle)
                } else {
                    invokeUnit(provider, "addText", styleBundle, text)
                }
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
                if (styleBundle.containsKey(KEY_FONT_NAME)) {
                    appendTextColumnsBitmap(
                        provider = provider,
                        styleBundle = styleBundle,
                        columns = listOf(
                            BitmapTextColumn(
                                text = left,
                                start = TWO_COLUMN_LEFT_START,
                                end = TWO_COLUMN_LEFT_END,
                                align = Paint.Align.LEFT,
                                alignValue = ALIGN_LEFT,
                            ),
                            BitmapTextColumn(
                                text = right,
                                start = TWO_COLUMN_RIGHT_START,
                                end = TWO_COLUMN_RIGHT_END,
                                align = Paint.Align.RIGHT,
                                alignValue = ALIGN_RIGHT,
                            ),
                        ),
                    )
                } else {
                    invokeUnit(provider, "addTextLeft_Right", styleBundle, left, right)
                }
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
                if (styleBundle.containsKey(KEY_FONT_NAME)) {
                    appendTextColumnsBitmap(
                        provider = provider,
                        styleBundle = styleBundle,
                        columns = listOf(
                            BitmapTextColumn(
                                text = left,
                                start = THREE_COLUMN_LEFT_START,
                                end = THREE_COLUMN_LEFT_END,
                                align = Paint.Align.LEFT,
                                alignValue = ALIGN_LEFT,
                            ),
                            BitmapTextColumn(
                                text = center,
                                start = THREE_COLUMN_CENTER_START,
                                end = THREE_COLUMN_CENTER_END,
                                align = Paint.Align.CENTER,
                                alignValue = ALIGN_CENTER,
                            ),
                            BitmapTextColumn(
                                text = right,
                                start = THREE_COLUMN_RIGHT_START,
                                end = THREE_COLUMN_RIGHT_END,
                                align = Paint.Align.RIGHT,
                                alignValue = ALIGN_RIGHT,
                            ),
                        ),
                    )
                } else {
                    invokeUnit(provider, "addTextLeft_Center_Right", styleBundle, left, center, right)
                }
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
                appendBarcode(
                    provider = provider,
                    data = data,
                    width = width,
                    height = height,
                    align = align,
                    barcodeType = barcodeType,
                )
            }

            "qr" -> {
                val data = command["data"] as? String
                    ?: throw UrovoPluginException(
                        errorCode = "invalid_argument",
                        message = "qr command requires data.",
                    )
                val expectedHeight = (command["expectedHeight"] as? Number)?.toInt() ?: 120
                val align = alignValue(command["align"] as? String)
                appendQrCode(
                    provider = provider,
                    data = data,
                    expectedHeight = expectedHeight,
                    align = align,
                )
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
            val fontAsset = (style?.get("fontAsset") as? String)?.takeIf { it.isNotBlank() }
            if (!fontAsset.isNullOrBlank()) {
                putString(KEY_FONT_NAME, resolveFontAssetToPath(fontAsset))
            }
        }
    }

    private fun resolveFontAssetToPath(assetPath: String): String {
        val lookupKey = FlutterInjector.instance().flutterLoader().getLookupKeyForAsset(assetPath)
        val outputFile = File(fontCacheDir, lookupKey.replace('/', '_'))
        if (outputFile.exists() && outputFile.length() > 0L) {
            return outputFile.absolutePath
        }

        if (!fontCacheDir.exists()) {
            fontCacheDir.mkdirs()
        }

        runCatching {
            appContext.assets.open(lookupKey).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }.getOrElse { error ->
            throw UrovoPluginException(
                errorCode = "invalid_argument",
                message = "Unable to resolve font asset: $assetPath.",
                details = mapOf("error" to (error.message ?: error.javaClass.simpleName)),
            )
        }

        return outputFile.absolutePath
    }

    private fun appendTextBitmap(
        provider: Any,
        text: String,
        styleBundle: Bundle,
    ) {
        var bitmap: Bitmap? = null
        try {
            bitmap = createTextBitmap(text = text, styleBundle = styleBundle)
            val align = styleBundle.getInt(KEY_ALIGN, ALIGN_LEFT)
            invokeInt(provider, "appendBitmap", bitmap, align)
        } finally {
            bitmap?.let { renderedBitmap ->
                if (!renderedBitmap.isRecycled) {
                    renderedBitmap.recycle()
                }
            }
        }
    }

    private fun appendTextColumnsBitmap(
        provider: Any,
        styleBundle: Bundle,
        columns: List<BitmapTextColumn>,
    ) {
        var bitmap: Bitmap? = null
        try {
            bitmap = createTextColumnsBitmap(styleBundle = styleBundle, columns = columns)
            invokeInt(provider, "appendBitmap", bitmap, ALIGN_LEFT)
        } finally {
            bitmap?.let { renderedBitmap ->
                if (!renderedBitmap.isRecycled) {
                    renderedBitmap.recycle()
                }
            }
        }
    }

    private fun createTextBitmap(
        text: String,
        styleBundle: Bundle,
    ): Bitmap {
        val fontSize = resolveTextSize(styleBundle)
        val paint = createTextPaint(styleBundle, fontSize)
        val lineSpacing = resolveLineSpacing(styleBundle)
        val isRtl = containsRtlText(text)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, TEXT_BITMAP_WIDTH)
            .setAlignment(resolveTextLayoutAlignment(styleBundle, isRtl))
            .setIncludePad(false)
            .setLineSpacing(lineSpacing.toFloat(), 1f)
            .setTextDirection(
                if (isRtl) {
                    TextDirectionHeuristics.RTL
                } else {
                    TextDirectionHeuristics.LTR
                },
            )
            .build()
        val height = layout.height.coerceAtLeast(1)
        return Bitmap.createBitmap(TEXT_BITMAP_WIDTH, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            Canvas(this).apply {
                layout.draw(this)
            }
        }
    }

    private fun createTextColumnsBitmap(
        styleBundle: Bundle,
        columns: List<BitmapTextColumn>,
    ): Bitmap {
        val fontSize = resolveTextSize(styleBundle)
        val paint = createTextPaint(styleBundle, fontSize)
        val lineSpacing = resolveLineSpacing(styleBundle)
        val preparedColumns = columns.map { column ->
            val width = (column.end - column.start).coerceAtLeast(1)
            val isRtl = containsRtlText(column.text)
            val layout = StaticLayout.Builder
                .obtain(column.text, 0, column.text.length, paint, width)
                .setAlignment(resolveTextLayoutAlignment(column.alignValue, isRtl))
                .setIncludePad(false)
                .setLineSpacing(lineSpacing.toFloat(), 1f)
                .setMaxLines(1)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setTextDirection(
                    if (isRtl) {
                        TextDirectionHeuristics.RTL
                    } else {
                        TextDirectionHeuristics.LTR
                    },
                )
                .build()
            PreparedBitmapTextColumn(column = column, layout = layout)
        }
        val contentHeight = preparedColumns.maxOfOrNull { it.layout.height } ?: 1
        val height = (contentHeight + lineSpacing + (TEXT_BITMAP_VERTICAL_PADDING * 2)).coerceAtLeast(1)

        return Bitmap.createBitmap(TEXT_BITMAP_WIDTH, height, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
            val canvas = Canvas(this)
            preparedColumns.forEach { preparedColumn ->
                canvas.save()
                canvas.translate(
                    preparedColumn.column.start.toFloat(),
                    TEXT_BITMAP_VERTICAL_PADDING.toFloat(),
                )
                preparedColumn.layout.draw(canvas)
                canvas.restore()
            }
        }
    }

    private fun createTextPaint(
        styleBundle: Bundle,
        fontSize: Int,
    ): TextPaint {
        val fontPath = styleBundle.getString(KEY_FONT_NAME).orEmpty()
        val isBold = styleBundle.getBoolean(KEY_FONT_BOLD, false)

        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = fontSize.toFloat()
            isFakeBoldText = isBold
            typeface = resolveTypeface(fontPath, isBold)
        }
    }

    private fun resolveTextSize(styleBundle: Bundle): Int {
        val overrideSize = styleBundle.getInt(KEY_FONT_SIZE, 0)
        if (overrideSize > 0) {
            return overrideSize
        }

        return when (styleBundle.getInt(KEY_FONT, FONT_NORMAL)) {
            FONT_SMALL -> 16
            FONT_LARGE -> 32
            else -> 24
        }
    }

    private fun resolveTypeface(
        fontPath: String,
        isBold: Boolean,
    ): Typeface {
        if (fontPath.isBlank()) {
            return if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        return runCatching {
            Typeface.createFromFile(fontPath)
        }.getOrElse {
            if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }
    }

    private fun resolveLineSpacing(styleBundle: Bundle): Int {
        return styleBundle.getInt(KEY_LINE_HEIGHT, DEFAULT_TEXT_LINE_HEIGHT)
    }

    private fun resolveTextLayoutAlignment(
        requestedAlign: Int,
        isRtl: Boolean,
    ): Layout.Alignment {
        return when (requestedAlign) {
            ALIGN_CENTER -> Layout.Alignment.ALIGN_CENTER
            ALIGN_RIGHT -> if (isRtl) Layout.Alignment.ALIGN_NORMAL else Layout.Alignment.ALIGN_OPPOSITE
            else -> if (isRtl) Layout.Alignment.ALIGN_OPPOSITE else Layout.Alignment.ALIGN_NORMAL
        }
    }

    private fun resolveTextLayoutAlignment(
        styleBundle: Bundle,
        isRtl: Boolean,
    ): Layout.Alignment {
        return resolveTextLayoutAlignment(
            requestedAlign = styleBundle.getInt(KEY_ALIGN, ALIGN_LEFT),
            isRtl = isRtl,
        )
    }

    private fun containsRtlText(text: String): Boolean {
        return text.any { character ->
            when (Character.getDirectionality(character)) {
                Character.DIRECTIONALITY_RIGHT_TO_LEFT.toByte(),
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC.toByte(),
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING.toByte(),
                Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE.toByte(),
                -> true

                else -> false
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

    private fun appendBarcode(
        provider: Any,
        data: String,
        width: Int,
        height: Int,
        align: Int,
        barcodeType: Any?,
    ) {
        if (tryAppendBarcodeWithNearestNeighborScaling(provider, data, width, height, align, barcodeType)) {
            return
        }

        // Fallback to vendor helper if custom bitmap path is unavailable.
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

    private fun appendQrCode(
        provider: Any,
        data: String,
        expectedHeight: Int,
        align: Int,
    ) {
        if (tryAppendQrCodeWithQuietZone(provider, data, expectedHeight, align)) {
            return
        }

        // Fallback to vendor helper if custom bitmap path is unavailable.
        val bundle = Bundle().apply {
            putInt(KEY_ALIGN, align)
            putInt(KEY_EXPECTED_HEIGHT, expectedHeight)
        }
        invokeUnit(provider, "addQrCode", bundle, data)
    }

    private fun tryAppendBarcodeWithNearestNeighborScaling(
        provider: Any,
        data: String,
        width: Int,
        height: Int,
        align: Int,
        barcodeType: Any?,
    ): Boolean {
        val effectiveFormat = barcodeType ?: resolveBarcodeFormat(type = null) ?: return false
        val rawBitmap = createBarcodeBitmap(data, height, effectiveFormat) ?: return false
        var finalBitmap: Bitmap? = null

        return runCatching {
            finalBitmap = scaleBarcodeBitmapNearestNeighbor(rawBitmap, width, height)
            invokeInt(provider, "appendBitmap", finalBitmap, align)
            true
        }.getOrElse {
            false
        }.also {
            finalBitmap?.let { bitmap ->
                if (bitmap !== rawBitmap && !bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
            if (!rawBitmap.isRecycled) {
                rawBitmap.recycle()
            }
        }
    }

    private fun tryAppendQrCodeWithQuietZone(
        provider: Any,
        data: String,
        expectedHeight: Int,
        align: Int,
    ): Boolean {
        val size = if (expectedHeight > 0) expectedHeight else DEFAULT_QR_SIZE
        val qrBitmap = createQrBitmap(data = data, size = size) ?: return false

        return runCatching {
            invokeInt(provider, "appendBitmap", qrBitmap, align)
            true
        }.getOrElse {
            false
        }.also {
            if (!qrBitmap.isRecycled) {
                qrBitmap.recycle()
            }
        }
    }

    private fun createBarcodeBitmap(
        data: String,
        height: Int,
        barcodeFormat: Any,
    ): Bitmap? {
        return runCatching {
            val encodingHandlerClass = Class.forName(ENCODING_HANDLER_CLASS)
            val barcodeFormatClass = Class.forName(BARCODE_FORMAT_CLASS)
            val method = encodingHandlerClass.getMethod(
                "creatBarcode",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                barcodeFormatClass,
            )
            method.invoke(
                null,
                data,
                1, // Vendor helper uses module width 1 and scales afterward.
                height,
                false, // Do not render human-readable text under the bars.
                1,
                barcodeFormat,
            ) as? Bitmap
        }.getOrNull()
    }

    private fun createQrBitmap(
        data: String,
        size: Int,
    ): Bitmap? {
        // Prefer EncodingHandler.createQRImage(...) because it leaves ZXing defaults intact
        // (notably the quiet zone), while PrinterProviderImpl.addQrCode() forces margin=0.
        val encodingHandlerBitmap = runCatching {
            val encodingHandlerClass = Class.forName(ENCODING_HANDLER_CLASS)
            val method = encodingHandlerClass.getMethod(
                "createQRImage",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            method.invoke(
                null,
                data,
                size,
                size,
            ) as? Bitmap
        }.getOrNull()
        if (encodingHandlerBitmap != null) {
            return encodingHandlerBitmap
        }

        // Fallback to QRCodeUtil with explicit quiet zone if EncodingHandler API changes.
        return runCatching {
            val qrCodeUtilClass = Class.forName(QR_CODE_UTIL_CLASS)
            val method = qrCodeUtilClass.getMethod(
                "createQRCodeBitmap",
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
            )
            method.invoke(
                null,
                data,
                size,
                size,
                "UTF-8",
                "L",
                "4",
                Color.BLACK,
                Color.WHITE,
            ) as? Bitmap
        }.getOrNull()
    }

    private fun scaleBarcodeBitmapNearestNeighbor(
        rawBitmap: Bitmap,
        width: Int,
        height: Int,
    ): Bitmap {
        if (width <= 0 || height <= 0) {
            return rawBitmap
        }
        if (rawBitmap.width == width && rawBitmap.height == height) {
            return rawBitmap
        }
        return Bitmap.createScaledBitmap(rawBitmap, width, height, false)
    }

    private fun ensurePrinterProvider(): Any {
        printerProvider?.let { return it }

        val providerClass = Class.forName(PRINTER_PROVIDER_CLASS)
        val provider = runCatching {
            val getInstanceMethod = providerClass.getMethod("getInstance", Context::class.java)
            getInstanceMethod.invoke(null, appContext)
        }.recoverCatching {
            val getInstanceMethod = providerClass.getMethod("getInstance")
            getInstanceMethod.invoke(null)
        }.recoverCatching {
            providerClass.getDeclaredConstructor(Context::class.java).newInstance(appContext)
        }.recoverCatching {
            providerClass.getDeclaredConstructor().newInstance()
        }.getOrElse { error ->
            throw UrovoPluginException(
                errorCode = "internal",
                message = "Unable to instantiate PrinterProviderImpl: ${error.message ?: "Unknown error."}",
            )
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

    private fun hasClass(className: String): Boolean {
        return try {
            Class.forName(className)
            true
        } catch (_: ClassNotFoundException) {
            false
        }
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
        private data class BitmapTextColumn(
            val text: String,
            val start: Int,
            val end: Int,
            val align: Paint.Align,
            val alignValue: Int,
        )

        private data class PreparedBitmapTextColumn(
            val column: BitmapTextColumn,
            val layout: StaticLayout,
        )

        private const val PRINTER_PROVIDER_CLASS = "com.urovo.sdk.print.PrinterProviderImpl"
        private const val PRINT_STATUS_CLASS = "com.urovo.sdk.print.PrintStatus"
        private const val ENCODING_HANDLER_CLASS = "com.urovo.sdk.print.EncodingHandler"
        private const val QR_CODE_UTIL_CLASS = "com.urovo.sdk.print.QRCodeUtil"
        private const val BARCODE_FORMAT_CLASS = "com.google.zxing.BarcodeFormat"

        private const val KEY_ALIGN = "align"
        private const val KEY_FONT = "font"
        private const val KEY_FONT_SIZE = "fontSize"
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

        private const val DEFAULT_QR_SIZE = 120
        private const val DEFAULT_TEXT_LINE_HEIGHT = 5
        private const val TEXT_BITMAP_WIDTH = 384
        private const val TEXT_BITMAP_VERTICAL_PADDING = 4

        private const val TWO_COLUMN_LEFT_START = 6
        private const val TWO_COLUMN_LEFT_END = 152
        private const val TWO_COLUMN_RIGHT_START = 164
        private const val TWO_COLUMN_RIGHT_END = 378

        private const val THREE_COLUMN_LEFT_START = 6
        private const val THREE_COLUMN_LEFT_END = 134
        private const val THREE_COLUMN_CENTER_START = 142
        private const val THREE_COLUMN_CENTER_END = 198
        private const val THREE_COLUMN_RIGHT_START = 206
        private const val THREE_COLUMN_RIGHT_END = 378
    }
}
