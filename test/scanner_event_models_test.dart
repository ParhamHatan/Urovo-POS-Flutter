import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/urovo_pos.dart';

void main() {
  test('parses decoded scanner event with raw bytes', () {
    final bytes = Uint8List.fromList(<int>[1, 2, 3, 4]);
    final event = UrovoScannerEvent.fromMap(<String, Object?>{
      'type': 'decoded',
      'data': 'ABC123',
      'rawBytesBase64': base64Encode(bytes),
      'timestampMs': 1710000000000,
    });

    expect(event.type, UrovoScannerEventType.decoded);
    expect(event.result?.data, 'ABC123');
    expect(event.result?.rawBytes, bytes);
    expect(event.errorCode, isNull);
    expect(event.message, isNull);
  });

  test('parses timeout and error scanner events', () {
    final timeout = UrovoScannerEvent.fromMap(<String, Object?>{
      'type': 'timeout',
      'timestampMs': 1710000000100,
    });
    final error = UrovoScannerEvent.fromMap(<String, Object?>{
      'type': 'error',
      'errorCode': 12,
      'message': 'Scanner busy',
      'timestampMs': 1710000000200,
    });

    expect(timeout.type, UrovoScannerEventType.timeout);
    expect(timeout.result, isNull);
    expect(error.type, UrovoScannerEventType.error);
    expect(error.errorCode, 12);
    expect(error.message, 'Scanner busy');
  });

  test('parses cancel/unknown scanner events and missing timestamps', () {
    final before = DateTime.now();
    final canceled = UrovoScannerEvent.fromMap(<String, Object?>{
      'type': 'cancel',
    });
    final unknown = UrovoScannerEvent.fromMap(<String, Object?>{
      'type': 'something_else',
    });
    final resultWithoutTimestamp = UrovoScanResult.fromMap(<String, Object?>{
      'data': 'NO_TS',
    });
    final after = DateTime.now();

    expect(canceled.type, UrovoScannerEventType.canceled);
    expect(canceled.result, isNull);
    expect(canceled.timestamp.isBefore(before), isFalse);
    expect(canceled.timestamp.isAfter(after), isFalse);

    expect(unknown.type, UrovoScannerEventType.unknown);
    expect(unknown.timestamp.isBefore(before), isFalse);
    expect(unknown.timestamp.isAfter(after), isFalse);

    expect(resultWithoutTimestamp.data, 'NO_TS');
    expect(resultWithoutTimestamp.rawBytes, isNull);
    expect(resultWithoutTimestamp.timestamp.isBefore(before), isFalse);
    expect(resultWithoutTimestamp.timestamp.isAfter(after), isFalse);
  });

  test('scanner event toMap keeps decoded payload data', () {
    final event = UrovoScannerEvent(
      type: UrovoScannerEventType.decoded,
      timestamp: DateTime.fromMillisecondsSinceEpoch(1710000000300),
      result: UrovoScanResult(
        data: 'HELLO',
        rawBytes: Uint8List.fromList(<int>[9, 8]),
        timestamp: DateTime.fromMillisecondsSinceEpoch(1710000000300),
      ),
    );

    final map = event.toMap();

    expect(map['type'], 'decoded');
    expect(map['data'], 'HELLO');
    expect(map['rawBytesBase64'], base64Encode(<int>[9, 8]));
  });

  test('scanner event toMap maps non-decoded event types', () {
    final ts = DateTime.fromMillisecondsSinceEpoch(1710000000400);

    expect(
      UrovoScannerEvent(type: UrovoScannerEventType.error, timestamp: ts).toMap()['type'],
      'error',
    );
    expect(
      UrovoScannerEvent(type: UrovoScannerEventType.timeout, timestamp: ts).toMap()['type'],
      'timeout',
    );
    expect(
      UrovoScannerEvent(type: UrovoScannerEventType.canceled, timestamp: ts).toMap()['type'],
      'cancel',
    );
    expect(
      UrovoScannerEvent(type: UrovoScannerEventType.unknown, timestamp: ts).toMap()['type'],
      'unknown',
    );
  });
}
