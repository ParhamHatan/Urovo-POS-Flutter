import 'package:flutter_test/flutter_test.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';
import 'package:urovo_pos/urovo_pos.dart';
import 'package:urovo_pos/urovo_pos_platform_interface.dart';

class LifecycleFakeUrovoPosPlatform with MockPlatformInterfaceMixin implements UrovoPosPlatform {
  bool sdkAvailable = true;
  bool initialized = false;

  int initCalls = 0;
  int closeCalls = 0;
  int statusCalls = 0;
  int runJobCalls = 0;
  int startPrintCalls = 0;

  @override
  Future<bool> isUrovoSdkAvailable() async => sdkAvailable;

  @override
  Future<void> printerInit() async {
    _ensureSdk();
    initCalls += 1;
    initialized = true;
  }

  @override
  Future<UrovoPrinterStatus> printerGetStatus() async {
    final detail = await printerGetStatusDetail();
    return detail.status;
  }

  @override
  Future<UrovoPrinterStatusDetail> printerGetStatusDetail() async {
    statusCalls += 1;
    _ensureInitialized('printerGetStatusDetail');
    return const UrovoPrinterStatusDetail(
      status: UrovoPrinterStatus.ok,
      rawCode: 0,
      message: 'Printer is ready.',
      recommendation: 'Continue printing.',
      retryable: true,
    );
  }

  @override
  Future<void> printerSetGray(int level) async {
    _ensureInitialized('printerSetGray');
  }

  @override
  Future<void> printerStartPrint() async {
    startPrintCalls += 1;
    _ensureInitialized('printerStartPrint');
  }

  @override
  Future<void> printerRunJob(UrovoPrintJob job) async {
    runJobCalls += 1;
    _ensureInitialized('printerRunJob');
  }

  @override
  Future<void> printerClose() async {
    _ensureSdk();
    closeCalls += 1;
    initialized = false;
  }

  void _ensureSdk() {
    if (!sdkAvailable) {
      throw const UrovoPrinterException(
        type: UrovoPrinterExceptionType.sdkNotFound,
        message: 'Urovo SDK classes were not found on Android runtime.',
      );
    }
  }

  void _ensureInitialized(String operationName) {
    _ensureSdk();
    if (!initialized) {
      throw UrovoPrinterException(
        type: UrovoPrinterExceptionType.notInitialized,
        message: 'Printer is not initialized. Call printerInit() before $operationName.',
      );
    }
  }
}

void main() {
  final initialPlatform = UrovoPosPlatform.instance;

  tearDown(() {
    UrovoPosPlatform.instance = initialPlatform;
  });

  test('status call without init throws notInitialized', () async {
    final fake = LifecycleFakeUrovoPosPlatform();
    UrovoPosPlatform.instance = fake;

    await expectLater(
      UrovoPos.printerGetStatusDetail(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.notInitialized,
        ),
      ),
    );

    expect(fake.statusCalls, 1);
    expect(fake.initCalls, 0);
  });

  test('startPrint call without init throws notInitialized', () async {
    final fake = LifecycleFakeUrovoPosPlatform();
    UrovoPosPlatform.instance = fake;

    await expectLater(
      UrovoPos.printerStartPrint(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.notInitialized,
        ),
      ),
    );

    expect(fake.startPrintCalls, 1);
    expect(fake.initCalls, 0);
  });

  test('close then runJob throws notInitialized', () async {
    final fake = LifecycleFakeUrovoPosPlatform();
    UrovoPosPlatform.instance = fake;

    await UrovoPos.printerInit();
    await UrovoPos.printerClose();

    await expectLater(
      UrovoPos.printerRunJob(UrovoPrintJob()..text('After close')),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.notInitialized,
        ),
      ),
    );

    expect(fake.initCalls, 1);
    expect(fake.closeCalls, 1);
    expect(fake.runJobCalls, 1);
  });

  test('init then runJob succeeds', () async {
    final fake = LifecycleFakeUrovoPosPlatform();
    UrovoPosPlatform.instance = fake;

    await UrovoPos.printerInit();
    await UrovoPos.printerRunJob(UrovoPrintJob()..text('Ready'));

    expect(fake.initCalls, 1);
    expect(fake.runJobCalls, 1);
  });

  test('sdk missing throws sdkNotFound', () async {
    final fake = LifecycleFakeUrovoPosPlatform()..sdkAvailable = false;
    UrovoPosPlatform.instance = fake;

    await expectLater(
      UrovoPos.printerInit(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.sdkNotFound,
        ),
      ),
    );

    await expectLater(
      UrovoPos.printerGetStatusDetail(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.sdkNotFound,
        ),
      ),
    );
  });
}
