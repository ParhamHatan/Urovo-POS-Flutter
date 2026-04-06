import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/urovo_pos.dart';

void main() {
  test('printer status wire values and reverse mapping cover all enum cases', () {
    const cases = <(UrovoPrinterStatus, String)>[
      (UrovoPrinterStatus.ok, 'ok'),
      (UrovoPrinterStatus.paperEnded, 'paperEnded'),
      (UrovoPrinterStatus.hardError, 'hardError'),
      (UrovoPrinterStatus.overheat, 'overheat'),
      (UrovoPrinterStatus.lowVoltage, 'lowVoltage'),
      (UrovoPrinterStatus.motorError, 'motorError'),
      (UrovoPrinterStatus.busy, 'busy'),
      (UrovoPrinterStatus.unknown, 'unknown'),
    ];

    for (final statusCase in cases) {
      expect(statusCase.$1.wireValue, statusCase.$2);
      expect(
        UrovoPrinterStatusWire.fromWireValue(statusCase.$2),
        statusCase.$1,
      );
    }
  });

  test('print enums wire values cover all enum branches', () {
    expect(UrovoAlign.left.wireValue, 'left');
    expect(UrovoAlign.center.wireValue, 'center');
    expect(UrovoAlign.right.wireValue, 'right');

    expect(UrovoFont.small.wireValue, 'small');
    expect(UrovoFont.normal.wireValue, 'normal');
    expect(UrovoFont.large.wireValue, 'large');

    expect(UrovoBarcodeType.code128.wireValue, 'code128');
    expect(UrovoBarcodeType.code39.wireValue, 'code39');
    expect(UrovoBarcodeType.ean13.wireValue, 'ean13');
    expect(UrovoBarcodeType.ean8.wireValue, 'ean8');
  });

  test('status detail toMap and defaults fromMap work as expected', () {
    const detail = UrovoPrinterStatusDetail(
      status: UrovoPrinterStatus.paperEnded,
      rawCode: 240,
      message: 'out of paper',
      recommendation: 'insert paper',
      retryable: true,
    );

    expect(detail.toMap(), <String, Object>{
      'status': 'paperEnded',
      'rawCode': 240,
      'message': 'out of paper',
      'recommendation': 'insert paper',
      'retryable': true,
    });

    final fallback = UrovoPrinterStatusDetail.fromMap(<String, Object>{});
    expect(fallback.status, UrovoPrinterStatus.unknown);
    expect(fallback.rawCode, -1);
    expect(fallback.message, 'Unknown printer status.');
    expect(
      fallback.recommendation,
      'Verify device state and retry if appropriate.',
    );
    expect(fallback.retryable, isFalse);
  });

  test('text style includes optional fields when configured', () {
    const style = UrovoTextStyle(
      align: UrovoAlign.center,
      bold: true,
      font: UrovoFont.large,
      lineHeight: 10,
      newline: false,
      fontAsset: 'assets/fonts/Vazirmatn-Regular.ttf',
    );

    expect(style.toMap(), <String, Object>{
      'align': 'center',
      'bold': true,
      'font': 'large',
      'newline': false,
      'lineHeight': 10,
      'fontAsset': 'assets/fonts/Vazirmatn-Regular.ttf',
    });
  });

  test('print job input validation rejects invalid text/barcode/qr/feed values', () {
    final job = UrovoPrintJob();
    expect(() => job.text(''), throwsArgumentError);
    expect(() => job.barcode(''), throwsArgumentError);
    expect(() => job.barcode('123', width: -1), throwsArgumentError);
    expect(() => job.barcode('123', height: 0), throwsArgumentError);
    expect(() => job.qr(''), throwsArgumentError);
    expect(() => job.qr('123', expectedHeight: 0), throwsArgumentError);
    expect(() => job.feedLine(-1), throwsArgumentError);
    expect(() => job.paperFeed(-1), throwsArgumentError);
  });
}
