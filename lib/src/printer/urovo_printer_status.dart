/// Normalized printer status values exposed by the plugin.
enum UrovoPrinterStatus {
  /// Printer is ready.
  ok,

  /// Paper roll is missing or ended.
  paperEnded,

  /// Printer hardware/driver error.
  hardError,

  /// Printer is overheated.
  overheat,

  /// Device battery voltage is too low for printing.
  lowVoltage,

  /// Printer motor error.
  motorError,

  /// Printer is busy.
  busy,

  /// Unrecognized status.
  unknown,
}

/// Conversion helpers for printer status wire values and raw codes.
extension UrovoPrinterStatusWire on UrovoPrinterStatus {
  /// Returns the wire status string.
  String get wireValue {
    return switch (this) {
      UrovoPrinterStatus.ok => 'ok',
      UrovoPrinterStatus.paperEnded => 'paperEnded',
      UrovoPrinterStatus.hardError => 'hardError',
      UrovoPrinterStatus.overheat => 'overheat',
      UrovoPrinterStatus.lowVoltage => 'lowVoltage',
      UrovoPrinterStatus.motorError => 'motorError',
      UrovoPrinterStatus.busy => 'busy',
      UrovoPrinterStatus.unknown => 'unknown',
    };
  }

  /// Parses a status from wire string value.
  static UrovoPrinterStatus fromWireValue(String? wireValue) {
    return switch (wireValue) {
      'ok' => UrovoPrinterStatus.ok,
      'paperEnded' => UrovoPrinterStatus.paperEnded,
      'hardError' => UrovoPrinterStatus.hardError,
      'overheat' => UrovoPrinterStatus.overheat,
      'lowVoltage' => UrovoPrinterStatus.lowVoltage,
      'motorError' => UrovoPrinterStatus.motorError,
      'busy' => UrovoPrinterStatus.busy,
      _ => UrovoPrinterStatus.unknown,
    };
  }

  /// Parses a status from raw vendor status code.
  static UrovoPrinterStatus fromRawCode(int rawCode) {
    return switch (rawCode) {
      0 => UrovoPrinterStatus.ok,
      240 => UrovoPrinterStatus.paperEnded,
      242 => UrovoPrinterStatus.hardError,
      243 => UrovoPrinterStatus.overheat,
      225 => UrovoPrinterStatus.lowVoltage,
      251 => UrovoPrinterStatus.motorError,
      247 => UrovoPrinterStatus.busy,
      _ => UrovoPrinterStatus.unknown,
    };
  }
}
