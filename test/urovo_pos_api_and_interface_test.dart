import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:urovo_pos/urovo_pos.dart';
import 'package:urovo_pos/urovo_pos_platform_interface.dart';

class _DelegatingFakePlatform with MockPlatformInterfaceMixin implements UrovoPosPlatform {
  int initCalls = 0;
  int statusCalls = 0;
  int statusDetailCalls = 0;
  int setGrayCalls = 0;
  int startPrintCalls = 0;
  int runJobCalls = 0;
  int closeCalls = 0;
  int scannerStartCalls = 0;
  int scannerStopCalls = 0;
  int? grayLevel;
  int? scannerCameraId;
  int? scannerTimeoutMs;

  final UrovoPrinterStatusDetail detail = const UrovoPrinterStatusDetail(
    status: UrovoPrinterStatus.busy,
    rawCode: 247,
    message: 'Printer is busy.',
    recommendation: 'Wait a moment, then retry.',
    retryable: true,
  );
  final UrovoScannerEvent scannerEvent = UrovoScannerEvent(
    type: UrovoScannerEventType.decoded,
    timestamp: DateTime.fromMillisecondsSinceEpoch(1710000000000),
    result: UrovoScanResult(
      data: '123456',
      timestamp: DateTime.fromMillisecondsSinceEpoch(1710000000000),
    ),
  );

  @override
  Future<bool> isUrovoSdkAvailable() async => true;

  @override
  Future<void> printerInit() async {
    initCalls += 1;
  }

  @override
  Future<UrovoPrinterStatus> printerGetStatus() async {
    statusCalls += 1;
    return UrovoPrinterStatus.busy;
  }

  @override
  Future<UrovoPrinterStatusDetail> printerGetStatusDetail() async {
    statusDetailCalls += 1;
    return detail;
  }

  @override
  Future<void> printerSetGray(int level) async {
    setGrayCalls += 1;
    grayLevel = level;
  }

  @override
  Future<void> printerStartPrint() async {
    startPrintCalls += 1;
  }

  @override
  Future<void> printerRunJob(UrovoPrintJob job) async {
    runJobCalls += 1;
  }

  @override
  Future<void> printerClose() async {
    closeCalls += 1;
  }

  @override
  Future<void> scannerStart({
    required int cameraId,
    required int timeoutMs,
  }) async {
    scannerStartCalls += 1;
    scannerCameraId = cameraId;
    scannerTimeoutMs = timeoutMs;
  }

  @override
  Future<void> scannerStop() async {
    scannerStopCalls += 1;
  }

  @override
  Stream<UrovoScannerEvent> get scannerEvents {
    return Stream<UrovoScannerEvent>.value(scannerEvent);
  }

  @override
  Stream<UrovoScanResult> get scannerDecodedStream {
    return Stream<UrovoScanResult>.value(scannerEvent.result!);
  }
}

class _BarePlatform extends UrovoPosPlatform {}

void main() {
  final initialPlatform = UrovoPosPlatform.instance;

  tearDown(() {
    UrovoPosPlatform.instance = initialPlatform;
  });

  test('UrovoPos API delegates to platform instance methods', () async {
    final fake = _DelegatingFakePlatform();
    UrovoPosPlatform.instance = fake;

    expect(await UrovoPos.isUrovoSdkAvailable(), isTrue);

    await UrovoPos.printerInit();
    expect(await UrovoPos.printerGetStatus(), UrovoPrinterStatus.busy);

    final detail = await UrovoPos.printerGetStatusDetail();
    expect(detail.rawCode, 247);

    await UrovoPos.printerSetGray(6);
    await UrovoPos.printerStartPrint();
    await UrovoPos.printerRunJob(UrovoPrintJob()..text('Hello'));
    await UrovoPos.printerClose();
    await UrovoPos.scannerStart(
      cameraId: 1,
      timeout: const Duration(seconds: 5),
    );
    final scannerEvent = await UrovoPos.scannerEvents.first;
    final scannerDecode = await UrovoPos.scannerDecodedStream.first;
    await UrovoPos.scannerStop();

    expect(fake.initCalls, 1);
    expect(fake.statusCalls, 1);
    expect(fake.statusDetailCalls, 1);
    expect(fake.setGrayCalls, 1);
    expect(fake.grayLevel, 6);
    expect(fake.startPrintCalls, 1);
    expect(fake.runJobCalls, 1);
    expect(fake.closeCalls, 1);
    expect(fake.scannerStartCalls, 1);
    expect(fake.scannerStopCalls, 1);
    expect(fake.scannerCameraId, 1);
    expect(fake.scannerTimeoutMs, 5000);
    expect(scannerEvent.type, UrovoScannerEventType.decoded);
    expect(scannerDecode.data, '123456');
  });

  test('base platform methods throw unimplemented errors', () {
    final platform = _BarePlatform();

    expect(platform.isUrovoSdkAvailable, throwsUnimplementedError);
    expect(platform.printerInit, throwsUnimplementedError);
    expect(platform.printerGetStatus, throwsUnimplementedError);
    expect(platform.printerGetStatusDetail, throwsUnimplementedError);
    expect(() => platform.printerSetGray(5), throwsUnimplementedError);
    expect(platform.printerStartPrint, throwsUnimplementedError);
    expect(
      () => platform.printerRunJob(UrovoPrintJob()),
      throwsUnimplementedError,
    );
    expect(platform.printerClose, throwsUnimplementedError);
    expect(
      () => platform.scannerStart(cameraId: 0, timeoutMs: 1000),
      throwsUnimplementedError,
    );
    expect(platform.scannerStop, throwsUnimplementedError);
    expect(() => platform.scannerEvents, throwsUnimplementedError);
    expect(() => platform.scannerDecodedStream, throwsUnimplementedError);
  });

  test('printer exception toString includes type and message', () {
    const error = UrovoPrinterException(
      type: UrovoPrinterExceptionType.internal,
      message: 'boom',
    );

    expect(
      error.toString(),
      'UrovoPrinterException(type: UrovoPrinterExceptionType.internal, message: boom)',
    );
  });
}
