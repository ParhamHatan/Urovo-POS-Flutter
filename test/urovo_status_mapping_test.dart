import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/urovo_pos.dart';

void main() {
  test('maps known vendor codes to printer status enum', () {
    expect(UrovoPrinterStatusWire.fromRawCode(0), UrovoPrinterStatus.ok);
    expect(
      UrovoPrinterStatusWire.fromRawCode(225),
      UrovoPrinterStatus.lowVoltage,
    );
    expect(
      UrovoPrinterStatusWire.fromRawCode(240),
      UrovoPrinterStatus.paperEnded,
    );
    expect(
      UrovoPrinterStatusWire.fromRawCode(242),
      UrovoPrinterStatus.hardError,
    );
    expect(
      UrovoPrinterStatusWire.fromRawCode(243),
      UrovoPrinterStatus.overheat,
    );
    expect(UrovoPrinterStatusWire.fromRawCode(247), UrovoPrinterStatus.busy);
    expect(
      UrovoPrinterStatusWire.fromRawCode(251),
      UrovoPrinterStatus.motorError,
    );
  });

  test('status detail parser falls back from raw code', () {
    final detail = UrovoPrinterStatusDetail.fromMap(<String, Object>{
      'rawCode': 240,
      'message': 'out of paper',
      'recommendation': 'insert paper',
      'retryable': true,
    });

    expect(detail.status, UrovoPrinterStatus.paperEnded);
    expect(detail.rawCode, 240);
    expect(detail.retryable, isTrue);
  });

  test('unknown code maps to unknown enum', () {
    expect(UrovoPrinterStatusWire.fromRawCode(999), UrovoPrinterStatus.unknown);
  });
}
