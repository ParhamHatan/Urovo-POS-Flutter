import 'dart:convert';
import 'dart:typed_data';

import 'package:urovo_pos/src/printer/urovo_print_enums.dart';
import 'package:urovo_pos/src/printer/urovo_text_style.dart';

/// Builder for composing a Urovo printer job payload.
class UrovoPrintJob {
  final List<Map<String, Object>> _commands = <Map<String, Object>>[];
  int? _gray;

  /// Creates an empty print job builder.
  UrovoPrintJob();

  /// Sets printer gray level in the supported range `0..10`.
  UrovoPrintJob setGray(int level) {
    if (level < 0 || level > 10) {
      throw ArgumentError.value(level, 'level', 'Gray level must be between 0 and 10.');
    }
    _gray = level;
    return this;
  }

  /// Appends a text command.
  UrovoPrintJob text(String text, {UrovoTextStyle? style}) {
    if (text.isEmpty) {
      throw ArgumentError.value(text, 'text', 'Text cannot be empty.');
    }
    _commands.add(<String, Object>{
      'type': 'text',
      'text': text,
      'style': (style ?? const UrovoTextStyle()).toMap(),
    });
    return this;
  }

  /// Appends a black separator line command.
  UrovoPrintJob blackLine() {
    _commands.add(<String, Object>{'type': 'blackLine'});
    return this;
  }

  /// Appends a two-column text command.
  UrovoPrintJob textLeftRight(
    String left,
    String right, {
    UrovoTextStyle? style,
  }) {
    _commands.add(<String, Object>{
      'type': 'textLeftRight',
      'left': left,
      'right': right,
      'style': (style ?? const UrovoTextStyle()).toMap(),
    });
    return this;
  }

  /// Appends a three-column text command.
  UrovoPrintJob textLeftCenterRight(
    String left,
    String center,
    String right, {
    UrovoTextStyle? style,
  }) {
    _commands.add(<String, Object>{
      'type': 'textLeftCenterRight',
      'left': left,
      'center': center,
      'right': right,
      'style': (style ?? const UrovoTextStyle()).toMap(),
    });
    return this;
  }

  /// Appends a barcode command.
  ///
  /// Set [width] to `0` to use the SDK-generated natural barcode width without
  /// additional horizontal scaling (often more scannable on thermal printers).
  UrovoPrintJob barcode(
    String value, {
    int width = 300,
    int height = 100,
    UrovoBarcodeType type = UrovoBarcodeType.code128,
    UrovoAlign align = UrovoAlign.center,
  }) {
    if (value.isEmpty) {
      throw ArgumentError.value(value, 'value', 'Barcode value cannot be empty.');
    }
    if (width < 0) {
      throw ArgumentError.value(width, 'width', 'Barcode width must be >= 0.');
    }
    if (height <= 0) {
      throw ArgumentError.value(height, 'height', 'Barcode height must be > 0.');
    }
    _commands.add(<String, Object>{
      'type': 'barcode',
      'data': value,
      'width': width,
      'height': height,
      'barcodeType': type.wireValue,
      'align': align.wireValue,
    });
    return this;
  }

  /// Appends a QR code command.
  ///
  /// On 58mm (384-dot) thermal printers, longer payloads (for example full URLs)
  /// require larger sizes to remain scannable. As a starting point:
  /// `160` for short IDs/text, `220+` for URLs.
  UrovoPrintJob qr(
    String value, {
    int expectedHeight = 120,
    UrovoAlign align = UrovoAlign.center,
  }) {
    if (value.isEmpty) {
      throw ArgumentError.value(value, 'value', 'QR value cannot be empty.');
    }
    if (expectedHeight <= 0) {
      throw ArgumentError.value(
        expectedHeight,
        'expectedHeight',
        'QR expectedHeight must be > 0.',
      );
    }
    _commands.add(<String, Object>{
      'type': 'qr',
      'data': value,
      'expectedHeight': expectedHeight,
      'align': align.wireValue,
    });
    return this;
  }

  /// Appends an image command using PNG/JPEG bytes.
  UrovoPrintJob imageBytes(
    Uint8List jpegOrPng, {
    int width = 200,
    int height = 80,
    UrovoAlign align = UrovoAlign.center,
  }) {
    if (jpegOrPng.isEmpty) {
      throw ArgumentError.value(jpegOrPng, 'jpegOrPng', 'Image bytes cannot be empty.');
    }
    _commands.add(<String, Object>{
      'type': 'imageBytes',
      'bytes': base64Encode(jpegOrPng),
      'width': width,
      'height': height,
      'align': align.wireValue,
    });
    return this;
  }

  /// Feeds paper by a number of text lines.
  UrovoPrintJob feedLine(int lines) {
    if (lines < 0) {
      throw ArgumentError.value(lines, 'lines', 'feedLine lines must be >= 0.');
    }
    _commands.add(<String, Object>{'type': 'feedLine', 'lines': lines});
    return this;
  }

  /// Feeds paper by absolute dot height.
  UrovoPrintJob paperFeed(int dots) {
    if (dots < 0) {
      throw ArgumentError.value(dots, 'dots', 'paperFeed dots must be >= 0.');
    }
    _commands.add(<String, Object>{'type': 'paperFeed', 'height': dots});
    return this;
  }

  /// Serializes job data for platform-channel invocation.
  Map<String, Object> toMap() {
    final map = <String, Object>{
      'commands': List<Map<String, Object>>.unmodifiable(_commands),
    };
    if (_gray != null) {
      map['gray'] = _gray!;
    }
    return map;
  }
}
