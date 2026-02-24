import 'dart:convert';
import 'dart:typed_data';

/// Decoded payload emitted by the Urovo scanner callback.
class UrovoScanResult {
  /// Decoded string payload.
  final String data;

  /// Raw bytes reported by the scanner callback when available.
  final Uint8List? rawBytes;

  /// Event timestamp.
  final DateTime timestamp;

  /// Creates a decoded scanner result.
  const UrovoScanResult({
    required this.data,
    required this.timestamp,
    this.rawBytes,
  });

  /// Parses a decoded scanner result from a platform event map.
  factory UrovoScanResult.fromMap(Map<dynamic, dynamic> map) {
    final rawBytesBase64 = map['rawBytesBase64'] as String?;
    final timestampMs = (map['timestampMs'] as num?)?.toInt();
    return UrovoScanResult(
      data: map['data'] as String? ?? '',
      rawBytes: rawBytesBase64 == null ? null : base64Decode(rawBytesBase64),
      timestamp: timestampMs == null ? DateTime.now() : DateTime.fromMillisecondsSinceEpoch(timestampMs),
    );
  }

  /// Converts this object to a platform-compatible map.
  Map<String, Object?> toMap() {
    return <String, Object?>{
      'data': data,
      'rawBytesBase64': rawBytes == null ? null : base64Encode(rawBytes!),
      'timestampMs': timestamp.millisecondsSinceEpoch,
    };
  }
}
