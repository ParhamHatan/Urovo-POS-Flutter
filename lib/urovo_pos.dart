/// Public library exports and facade APIs for the `urovo_pos` plugin.
library;

import 'dart:typed_data';

import 'package:urovo_pos/src/exceptions/urovo_printer_exception.dart';
import 'package:urovo_pos/src/printer/urovo_print_enums.dart';
import 'package:urovo_pos/src/printer/urovo_print_job.dart';
import 'package:urovo_pos/src/printer/urovo_printer_status.dart';
import 'package:urovo_pos/src/printer/urovo_printer_status_detail.dart';
import 'package:urovo_pos/src/printer/urovo_text_style.dart';
import 'package:urovo_pos/urovo_pos_platform_interface.dart';

export 'package:urovo_pos/src/exceptions/urovo_printer_exception.dart';
export 'package:urovo_pos/src/printer/urovo_print_enums.dart';
export 'package:urovo_pos/src/printer/urovo_print_job.dart';
export 'package:urovo_pos/src/printer/urovo_printer_status.dart';
export 'package:urovo_pos/src/printer/urovo_printer_status_detail.dart';
export 'package:urovo_pos/src/printer/urovo_text_style.dart';

/// Entry point for interacting with the Urovo Android POS plugin.
///
/// This API exposes printer lifecycle operations and high-level helpers.
abstract final class UrovoPos {

  /// Returns whether Urovo SDK classes are available at runtime.
  static Future<bool> isUrovoSdkAvailable() {
    return UrovoPosPlatform.instance.isUrovoSdkAvailable();
  }

  /// Opens a printer session.
  ///
  /// Must be called before printer operations such as status checks and print jobs.
  static Future<void> printerInit() {
    return UrovoPosPlatform.instance.printerInit();
  }

  /// Returns the current high-level printer status.
  static Future<UrovoPrinterStatus> printerGetStatus() {
    return UrovoPosPlatform.instance.printerGetStatus();
  }

  /// Returns detailed printer status including raw vendor code and guidance.
  static Future<UrovoPrinterStatusDetail> printerGetStatusDetail() {
    return UrovoPosPlatform.instance.printerGetStatusDetail();
  }

  /// Sets printer gray level in the vendor-supported range `0..10`.
  static Future<void> printerSetGray(int level) {
    return UrovoPosPlatform.instance.printerSetGray(level);
  }

  /// Starts printing for previously queued vendor commands.
  static Future<void> printerStartPrint() {
    return UrovoPosPlatform.instance.printerStartPrint();
  }

  /// Sends and runs a print job in one call.
  static Future<void> printerRunJob(UrovoPrintJob job) {
    return UrovoPosPlatform.instance.printerRunJob(job);
  }

  /// Closes the active printer session.
  static Future<void> printerClose() {
    return UrovoPosPlatform.instance.printerClose();
  }

  /// Builds and prints a sample receipt job.
  ///
  /// This helper does not manage printer lifecycle automatically. Call
  /// [printerInit] before invoking this method and [printerClose] after.
  static Future<void> printSample({
    Uint8List? logoBytes,
    String merchant = 'UROVO POS',
    String qrData = 'UROVO_POS_SAMPLE_01',
    String barcodeData = '123456789012',
  }) async {
    final available = await isUrovoSdkAvailable();
    if (!available) {
      throw const UrovoPrinterException(
        type: UrovoPrinterExceptionType.sdkNotFound,
        message: 'Urovo SDK classes were not found on Android runtime.',
      );
    }

    final job = UrovoPrintJob()
      // Mixed 1D/2D receipts usually need a small bump over 0 for reliable QR scans.
      ..setGray(2)
      ..text(
        merchant,
        style: const UrovoTextStyle(
          align: UrovoAlign.center,
          bold: true,
          font: UrovoFont.large,
          lineHeight: 8,
        ),
      )
      ..text(
        'Sample Receipt',
        style: const UrovoTextStyle(
          align: UrovoAlign.center,
        ),
      )
      ..feedLine(1)
      ..blackLine()
      ..textLeftRight('Date', DateTime.now().toIso8601String())
      ..textLeftCenterRight('Item A', 'x2', '200,000')
      ..textLeftCenterRight('Item B', 'x1', '120,000')
      ..blackLine()
      ..textLeftRight(
        'TOTAL',
        '320,000',
        style: const UrovoTextStyle(
          bold: true,
          font: UrovoFont.large,
        ),
      )
      ..feedLine(1)
      ..barcode(
        barcodeData,
        width: 300,
        height: 100,
        type: UrovoBarcodeType.code128,
        align: UrovoAlign.center,
      )
      ..feedLine(2)
      ..qr(
        qrData,
        expectedHeight: 160,
        align: UrovoAlign.center,
      )
      ..feedLine(5);

    if (logoBytes != null) {
      job
        ..feedLine(1)
        ..imageBytes(
          logoBytes,
          width: 180,
          height: 80,
          align: UrovoAlign.center,
        );
    }

    await printerRunJob(job);
  }
}
