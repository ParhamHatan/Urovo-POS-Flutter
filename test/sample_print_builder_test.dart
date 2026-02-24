import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:urovo_pos/urovo_pos.dart';
import 'package:urovo_pos/urovo_pos_platform_interface.dart';

class FakeUrovoPosPlatform with MockPlatformInterfaceMixin implements UrovoPosPlatform {
  bool sdkAvailable = true;
  bool throwOnRunJob = false;
  int initCalls = 0;
  int closeCalls = 0;
  int runJobCalls = 0;
  Map<String, Object>? lastJob;

  @override
  Future<bool> isUrovoSdkAvailable() async => sdkAvailable;

  @override
  Future<void> printerInit() async {
    initCalls += 1;
    if (!sdkAvailable) {
      throw const UrovoPrinterException(
        type: UrovoPrinterExceptionType.sdkNotFound,
        message: 'Urovo SDK classes were not found on Android runtime.',
      );
    }
  }

  @override
  Future<UrovoPrinterStatus> printerGetStatus() async => UrovoPrinterStatus.ok;

  @override
  Future<UrovoPrinterStatusDetail> printerGetStatusDetail() async {
    return const UrovoPrinterStatusDetail(
      status: UrovoPrinterStatus.ok,
      rawCode: 0,
      message: 'ready',
      recommendation: 'continue',
      retryable: true,
    );
  }

  @override
  Future<void> printerSetGray(int level) async {}

  @override
  Future<void> printerStartPrint() async {}

  @override
  Future<void> printerRunJob(UrovoPrintJob job) async {
    runJobCalls += 1;
    if (throwOnRunJob) {
      throw const UrovoPrinterException(
        type: UrovoPrinterExceptionType.printFailed,
        message: 'Simulated print failure.',
      );
    }
    lastJob = job.toMap();
  }

  @override
  Future<void> printerClose() async {
    closeCalls += 1;
  }

  @override
  Future<void> scannerStart({
    required int cameraId,
    required int timeoutMs,
  }) async {}

  @override
  Future<void> scannerStop() async {}

  @override
  Stream<UrovoScannerEvent> get scannerEvents {
    return const Stream<UrovoScannerEvent>.empty();
  }

  @override
  Stream<UrovoScanResult> get scannerDecodedStream {
    return const Stream<UrovoScanResult>.empty();
  }
}

void main() {
  final initialPlatform = UrovoPosPlatform.instance;

  tearDown(() {
    UrovoPosPlatform.instance = initialPlatform;
  });

  test('printSample builds standard payload without logo', () async {
    final fake = FakeUrovoPosPlatform();
    UrovoPosPlatform.instance = fake;

    await UrovoPos.printSample();

    final job = fake.lastJob;
    expect(job, isNotNull);
    final commandsObject = job!['commands'];
    expect(commandsObject, isA<List<Map<String, Object>>>());
    final commands = commandsObject! as List<Map<String, Object>>;

    expect(job['gray'], 2);
    expect(commands.any((command) => command['type'] == 'imageBytes'), isFalse);
    expect(commands.any((command) => command['type'] == 'barcode'), isTrue);
    expect(commands.any((command) => command['type'] == 'qr'), isTrue);
    expect(fake.initCalls, 0);
    expect(fake.closeCalls, 0);
    expect(fake.runJobCalls, 1);
  });

  test('printSample includes logo image command when bytes are provided', () async {
    final fake = FakeUrovoPosPlatform();
    UrovoPosPlatform.instance = fake;

    await UrovoPos.printSample(logoBytes: Uint8List.fromList(<int>[1, 2, 3]));

    final commandsObject = fake.lastJob?['commands'];
    expect(commandsObject, isA<List<Map<String, Object>>>());
    final commands = commandsObject! as List<Map<String, Object>>;
    expect(commands.any((command) => command['type'] == 'imageBytes'), isTrue);
    expect(fake.initCalls, 0);
    expect(fake.closeCalls, 0);
  });

  test('printSample throws sdkNotFound when SDK is unavailable', () async {
    final fake = FakeUrovoPosPlatform()..sdkAvailable = false;
    UrovoPosPlatform.instance = fake;

    await expectLater(
      UrovoPos.printSample(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.sdkNotFound,
        ),
      ),
    );

    expect(fake.runJobCalls, 0);
    expect(fake.initCalls, 0);
    expect(fake.closeCalls, 0);
  });

  test('printSample does not auto-manage lifecycle when print job fails', () async {
    final fake = FakeUrovoPosPlatform()..throwOnRunJob = true;
    UrovoPosPlatform.instance = fake;

    await expectLater(
      UrovoPos.printSample(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.printFailed,
        ),
      ),
    );

    expect(fake.initCalls, 0);
    expect(fake.runJobCalls, 1);
    expect(fake.closeCalls, 0);
  });
}
