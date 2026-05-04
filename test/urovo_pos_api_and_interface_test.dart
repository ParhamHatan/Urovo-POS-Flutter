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
  int beeperBeepCalls = 0;
  int beeperStopCalls = 0;
  int deviceStatusCalls = 0;
  int? grayLevel;
  int? scannerCameraId;
  int? scannerTimeoutMs;
  UrovoBeeperPattern? beeperPattern;
  int? beeperRepeat;
  int? beeperDurationMs;
  int? beeperIntervalMs;
  double? beeperVolume;

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
  final UrovoDeviceStatus deviceStatus = UrovoDeviceStatus(
    deviceManagerAvailable: true,
    manufacturer: 'Urovo',
    brand: 'Urovo',
    model: 'i9000S',
    device: 'i9000s',
    androidVersion: '11',
    androidSdkInt: 30,
    serialNumber: 'SN123',
    tidSerialNumber: 'TID123',
    docked: false,
    timestamp: DateTime.fromMillisecondsSinceEpoch(1710000000000),
  );

  @override
  Future<bool> isUrovoSdkAvailable() async => true;

  @override
  Future<UrovoDeviceStatus> deviceGetStatus() async {
    deviceStatusCalls += 1;
    return deviceStatus;
  }

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

  @override
  Future<void> beeperBeep({
    required UrovoBeeperPattern pattern,
    required int repeat,
    required int durationMs,
    required int intervalMs,
    required double volume,
  }) async {
    beeperBeepCalls += 1;
    beeperPattern = pattern;
    beeperRepeat = repeat;
    beeperDurationMs = durationMs;
    beeperIntervalMs = intervalMs;
    beeperVolume = volume;
  }

  @override
  Future<void> beeperStop() async {
    beeperStopCalls += 1;
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
    final deviceStatus = await UrovoPos.deviceGetStatus();

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
    await UrovoPos.beeperBeep(
      pattern: UrovoBeeperPattern.warning,
      repeat: 2,
      duration: const Duration(milliseconds: 200),
      interval: const Duration(milliseconds: 40),
      volume: 0.5,
    );
    await UrovoPos.beeperStop();

    expect(deviceStatus.model, 'i9000S');
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
    expect(fake.deviceStatusCalls, 1);
    expect(fake.beeperBeepCalls, 1);
    expect(fake.beeperStopCalls, 1);
    expect(fake.beeperPattern, UrovoBeeperPattern.warning);
    expect(fake.beeperRepeat, 2);
    expect(fake.beeperDurationMs, 200);
    expect(fake.beeperIntervalMs, 40);
    expect(fake.beeperVolume, 0.5);
  });

  test('base platform methods throw unimplemented errors', () {
    final platform = _BarePlatform();

    expect(platform.isUrovoSdkAvailable, throwsUnimplementedError);
    expect(platform.deviceGetStatus, throwsUnimplementedError);
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
    expect(
      () => platform.beeperBeep(
        pattern: UrovoBeeperPattern.short,
        repeat: 1,
        durationMs: 120,
        intervalMs: 80,
        volume: 1,
      ),
      throwsUnimplementedError,
    );
    expect(platform.beeperStop, throwsUnimplementedError);
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
