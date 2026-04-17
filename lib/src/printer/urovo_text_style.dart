import 'package:urovo_pos/src/printer/urovo_print_enums.dart';

/// Styling options for text print commands.
class UrovoTextStyle {
  /// Text alignment on paper.
  final UrovoAlign align;

  /// Whether to use bold rendering.
  final bool bold;

  /// Font size preset.
  final UrovoFont font;

  /// Optional line-height hint for vendor SDK.
  final int? lineHeight;

  /// Whether to append a new line after text.
  final bool newline;

  /// Optional Flutter asset path for a TTF/OTF font.
  ///
  /// When provided, native plugin code resolves the asset and provides a
  /// native file path to the Urovo SDK.
  final String? fontAsset;

  /// Creates a text style payload.
  const UrovoTextStyle({
    this.align = UrovoAlign.left,
    this.bold = false,
    this.font = UrovoFont.normal,
    this.lineHeight,
    this.newline = true,
    this.fontAsset,
  });

  /// Serializes this style into method-channel payload format.
  Map<String, Object> toMap() {
    final map = <String, Object>{
      'align': align.wireValue,
      'bold': bold,
      'font': font.wireValue,
      'newline': newline,
    };

    if (lineHeight != null) {
      map['lineHeight'] = lineHeight!;
    }

    if (fontAsset != null && fontAsset!.isNotEmpty) {
      map['fontAsset'] = fontAsset!;
    }

    return map;
  }
}
