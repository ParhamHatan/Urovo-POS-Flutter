import 'package:urovo_pos/src/printer/urovo_printer_status_detail.dart';

/// Typed error categories emitted by the Urovo printer APIs.
enum UrovoPrinterExceptionType {
  /// Urovo vendor SDK classes were not found on Android runtime.
  sdkNotFound,

  /// A printer operation was called before `printerInit()`.
  notInitialized,

  /// Input payload was malformed or outside accepted value range.
  invalidArgument,

  /// Printer hardware/session is unavailable (paper, voltage, busy, etc.).
  deviceUnavailable,

  /// A print command failed for an unknown or unmapped reason.
  printFailed,

  /// Internal plugin/runtime failure.
  internal,
}

/// Exception raised by the plugin for printer-related failures.
class UrovoPrinterException implements Exception {
  /// Typed category for this exception.
  final UrovoPrinterExceptionType type;

  /// Human-readable error message.
  final String message;

  /// Optional printer status payload attached to the failure.
  final UrovoPrinterStatusDetail? statusDetail;

  /// Creates a typed printer exception.
  const UrovoPrinterException({
    required this.type,
    required this.message,
    this.statusDetail,
  });

  @override
  String toString() => 'UrovoPrinterException(type: $type, message: $message)';
}
