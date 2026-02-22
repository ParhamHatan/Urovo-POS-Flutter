import 'package:urovo_pos/src/printer/urovo_printer_status.dart';

/// Detailed printer status payload with vendor raw code and guidance.
class UrovoPrinterStatusDetail {
  /// Normalized status value.
  final UrovoPrinterStatus status;

  /// Raw vendor code returned by Urovo SDK.
  final int rawCode;

  /// Human-readable status message.
  final String message;

  /// Recommended action for current status.
  final String recommendation;

  /// Whether retrying is usually safe.
  final bool retryable;

  /// Creates a status detail model.
  const UrovoPrinterStatusDetail({
    required this.status,
    required this.rawCode,
    required this.message,
    required this.recommendation,
    required this.retryable,
  });

  /// Parses status detail from method-channel map.
  factory UrovoPrinterStatusDetail.fromMap(Map<dynamic, dynamic> map) {
    final rawCode = (map['rawCode'] as num?)?.toInt() ?? -1;
    final status = UrovoPrinterStatusWire.fromWireValue(map['status'] as String?);
    return UrovoPrinterStatusDetail(
      status: status == UrovoPrinterStatus.unknown ? UrovoPrinterStatusWire.fromRawCode(rawCode) : status,
      rawCode: rawCode,
      message: map['message'] as String? ?? 'Unknown printer status.',
      recommendation: map['recommendation'] as String? ?? 'Verify device state and retry if appropriate.',
      retryable: map['retryable'] as bool? ?? false,
    );
  }

  /// Converts this object back to method-channel payload map.
  Map<String, Object> toMap() {
    return <String, Object>{
      'status': status.wireValue,
      'rawCode': rawCode,
      'message': message,
      'recommendation': recommendation,
      'retryable': retryable,
    };
  }
}
