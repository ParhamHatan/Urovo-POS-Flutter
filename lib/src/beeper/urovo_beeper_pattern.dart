/// Named beep patterns supported by the plugin.
enum UrovoBeeperPattern {
  /// A single short acknowledgement beep.
  short,

  /// A positive confirmation tone.
  success,

  /// An attention tone.
  warning,

  /// A negative/error tone.
  error,
}

/// Conversion helpers for beeper pattern wire values.
extension UrovoBeeperPatternWire on UrovoBeeperPattern {
  /// Returns the method-channel wire value.
  String get wireValue {
    return switch (this) {
      UrovoBeeperPattern.short => 'short',
      UrovoBeeperPattern.success => 'success',
      UrovoBeeperPattern.warning => 'warning',
      UrovoBeeperPattern.error => 'error',
    };
  }

  /// Parses a beeper pattern from method-channel wire value.
  static UrovoBeeperPattern fromWireValue(String? wireValue) {
    return switch (wireValue) {
      'success' => UrovoBeeperPattern.success,
      'warning' => UrovoBeeperPattern.warning,
      'error' => UrovoBeeperPattern.error,
      _ => UrovoBeeperPattern.short,
    };
  }
}
