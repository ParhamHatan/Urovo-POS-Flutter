/// Horizontal alignment for printable content.
enum UrovoAlign { left, center, right }

/// Wire mapping helpers for [UrovoAlign].
extension UrovoAlignWire on UrovoAlign {
  /// Returns vendor wire value for this alignment.
  String get wireValue {
    return switch (this) {
      UrovoAlign.left => 'left',
      UrovoAlign.center => 'center',
      UrovoAlign.right => 'right',
    };
  }
}

/// Printer font size presets.
enum UrovoFont { small, normal, large }

/// Wire mapping helpers for [UrovoFont].
extension UrovoFontWire on UrovoFont {
  /// Returns vendor wire value for this font.
  String get wireValue {
    return switch (this) {
      UrovoFont.small => 'small',
      UrovoFont.normal => 'normal',
      UrovoFont.large => 'large',
    };
  }
}

/// Supported barcode formats.
enum UrovoBarcodeType { code128, code39, ean13, ean8 }

/// Wire mapping helpers for [UrovoBarcodeType].
extension UrovoBarcodeTypeWire on UrovoBarcodeType {
  /// Returns vendor wire value for this barcode type.
  String get wireValue {
    return switch (this) {
      UrovoBarcodeType.code128 => 'code128',
      UrovoBarcodeType.code39 => 'code39',
      UrovoBarcodeType.ean13 => 'ean13',
      UrovoBarcodeType.ean8 => 'ean8',
    };
  }
}
