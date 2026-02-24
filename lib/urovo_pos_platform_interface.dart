/// Platform-interface definitions for the `urovo_pos` plugin.
library;

import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:urovo_pos/src/printer/urovo_print_job.dart';
import 'package:urovo_pos/src/printer/urovo_printer_status.dart';
import 'package:urovo_pos/src/printer/urovo_printer_status_detail.dart';
import 'package:urovo_pos/src/scanner/urovo_scan_result.dart';
import 'package:urovo_pos/src/scanner/urovo_scanner_event.dart';
import 'package:urovo_pos/urovo_pos_method_channel.dart';

/// Platform interface contract for `urovo_pos`.
abstract class UrovoPosPlatform extends PlatformInterface {
  /// Creates a platform interface instance.
  UrovoPosPlatform() : super(token: _token);

  static final Object _token = Object();

  static UrovoPosPlatform _instance = MethodChannelUrovoPos();

  /// Active platform implementation.
  static UrovoPosPlatform get instance => _instance;

  /// Replaces active platform implementation (mostly for tests/fakes).
  static set instance(UrovoPosPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  /// Returns whether the Urovo SDK classes exist on current runtime.
  Future<bool> isUrovoSdkAvailable() {
    throw UnimplementedError('isUrovoSdkAvailable() has not been implemented.');
  }

  /// Opens printer session.
  Future<void> printerInit() {
    throw UnimplementedError('printerInit() has not been implemented.');
  }

  /// Returns normalized printer status.
  Future<UrovoPrinterStatus> printerGetStatus() {
    throw UnimplementedError('printerGetStatus() has not been implemented.');
  }

  /// Returns detailed printer status.
  Future<UrovoPrinterStatusDetail> printerGetStatusDetail() {
    throw UnimplementedError('printerGetStatusDetail() has not been implemented.');
  }

  /// Sets printer gray level.
  Future<void> printerSetGray(int level) {
    throw UnimplementedError('printerSetGray() has not been implemented.');
  }

  /// Starts print for queued vendor commands.
  Future<void> printerStartPrint() {
    throw UnimplementedError('printerStartPrint() has not been implemented.');
  }

  /// Runs a print job.
  Future<void> printerRunJob(UrovoPrintJob job) {
    throw UnimplementedError('printerRunJob() has not been implemented.');
  }

  /// Closes printer session.
  Future<void> printerClose() {
    throw UnimplementedError('printerClose() has not been implemented.');
  }

  /// Starts a scanner session with the provided timeout (milliseconds).
  Future<void> scannerStart({
    required int cameraId,
    required int timeoutMs,
  }) {
    throw UnimplementedError('scannerStart() has not been implemented.');
  }

  /// Stops an active scanner session.
  Future<void> scannerStop() {
    throw UnimplementedError('scannerStop() has not been implemented.');
  }

  /// Broadcast stream of scanner lifecycle and decode events.
  Stream<UrovoScannerEvent> get scannerEvents {
    throw UnimplementedError('scannerEvents has not been implemented.');
  }

  /// Convenience broadcast stream that only emits decoded payloads.
  Stream<UrovoScanResult> get scannerDecodedStream {
    throw UnimplementedError('scannerDecodedStream has not been implemented.');
  }
}
