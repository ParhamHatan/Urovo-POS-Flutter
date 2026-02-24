import 'package:urovo_pos/src/scanner/urovo_scan_result.dart';

/// Scanner event types emitted by the Urovo scanner callback bridge.
enum UrovoScannerEventType {
  /// A barcode/QR payload was decoded successfully.
  decoded,

  /// Scanner reported an error callback.
  error,

  /// Scan request timed out.
  timeout,

  /// Scan request was canceled.
  canceled,

  /// Unknown/unmapped scanner callback event.
  unknown,
}

/// Typed scanner callback event parsed from the EventChannel payload.
class UrovoScannerEvent {
  /// Event type.
  final UrovoScannerEventType type;

  /// Decoded payload for [UrovoScannerEventType.decoded] events.
  final UrovoScanResult? result;

  /// Vendor error code for [UrovoScannerEventType.error] events.
  final int? errorCode;

  /// Optional message for error events.
  final String? message;

  /// Event timestamp.
  final DateTime timestamp;

  /// Creates a scanner event.
  const UrovoScannerEvent({
    required this.type,
    required this.timestamp,
    this.result,
    this.errorCode,
    this.message,
  });

  /// Parses a scanner event from the platform event map.
  factory UrovoScannerEvent.fromMap(Map<dynamic, dynamic> map) {
    final typeValue = map['type'] as String?;
    final timestampMs = (map['timestampMs'] as num?)?.toInt();
    final timestamp = timestampMs == null ? DateTime.now() : DateTime.fromMillisecondsSinceEpoch(timestampMs);

    final type = switch (typeValue) {
      'decoded' => UrovoScannerEventType.decoded,
      'error' => UrovoScannerEventType.error,
      'timeout' => UrovoScannerEventType.timeout,
      'cancel' => UrovoScannerEventType.canceled,
      _ => UrovoScannerEventType.unknown,
    };

    return UrovoScannerEvent(
      type: type,
      timestamp: timestamp,
      result: type == UrovoScannerEventType.decoded ? UrovoScanResult.fromMap(map) : null,
      errorCode: (map['errorCode'] as num?)?.toInt(),
      message: map['message'] as String?,
    );
  }

  /// Converts this object to a platform-compatible map.
  Map<String, Object?> toMap() {
    final map = <String, Object?>{
      'type': switch (type) {
        UrovoScannerEventType.decoded => 'decoded',
        UrovoScannerEventType.error => 'error',
        UrovoScannerEventType.timeout => 'timeout',
        UrovoScannerEventType.canceled => 'cancel',
        UrovoScannerEventType.unknown => 'unknown',
      },
      'timestampMs': timestamp.millisecondsSinceEpoch,
      'errorCode': errorCode,
      'message': message,
    };

    if (result != null) {
      map.addAll(result!.toMap());
    }
    return map;
  }
}
