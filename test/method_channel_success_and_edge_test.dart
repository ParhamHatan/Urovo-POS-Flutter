import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/src/printer/printer_channel_contract.dart';
import 'package:urovo_pos/src/scanner/scanner_channel_contract.dart';
import 'package:urovo_pos/urovo_pos.dart';
import 'package:urovo_pos/urovo_pos_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('urovo_pos/methods');
  const scannerEventChannel = EventChannel(ScannerChannelContract.eventChannelName);
  final platform = MethodChannelUrovoPos();
  final codec = const StandardMethodCodec();

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMessageHandler(
      scannerEventChannel.name,
      null,
    );
  });

  test('isUrovoSdkAvailable handles raw non-map payloads', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async => true);
    expect(await platform.isUrovoSdkAvailable(), isTrue);

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (call) async => 'true');
    expect(await platform.isUrovoSdkAvailable(), isFalse);
  });

  test('status/setGray/close success paths parse and send expected payloads', () async {
    final methods = <String>[];
    Object? receivedGrayArg;

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      methods.add(call.method);

      switch (call.method) {
        case PrinterChannelContract.printerGetStatusDetail:
          return <String, Object?>{
            'code': 'ok',
            'message': 'OK',
            'data': <String, Object>{
              'status': 'busy',
              'rawCode': 247,
              'message': 'Printer is busy.',
              'recommendation': 'Wait a moment, then retry.',
              'retryable': true,
            },
          };
        case PrinterChannelContract.printerSetGray:
          receivedGrayArg = call.arguments;
          return <String, Object?>{'code': 'ok', 'message': 'OK', 'data': null};
        case PrinterChannelContract.printerClose:
          return <String, Object?>{'code': 'ok', 'message': 'OK', 'data': null};
        default:
          return <String, Object?>{'code': 'ok', 'message': 'OK', 'data': null};
      }
    });

    final status = await platform.printerGetStatus();
    final detail = await platform.printerGetStatusDetail();
    await platform.printerSetGray(7);
    await platform.printerClose();

    expect(status, UrovoPrinterStatus.busy);
    expect(detail.status, UrovoPrinterStatus.busy);
    expect(detail.rawCode, 247);
    expect(receivedGrayArg, <String, Object>{'gray': 7});
    expect(methods.contains(PrinterChannelContract.printerClose), isTrue);
  });

  test('scannerStart/scannerStop success paths send expected payloads', () async {
    final methods = <String>[];
    Object? receivedStartArgs;

    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      methods.add(call.method);
      if (call.method == ScannerChannelContract.scannerStart) {
        receivedStartArgs = call.arguments;
      }
      return <String, Object?>{'code': 'ok', 'message': 'OK', 'data': null};
    });

    await platform.scannerStart(cameraId: 1, timeoutMs: 7000);
    await platform.scannerStop();

    expect(
      receivedStartArgs,
      <String, Object>{
        'cameraId': 1,
        'timeoutMs': 7000,
      },
    );
    expect(methods, contains(ScannerChannelContract.scannerStart));
    expect(methods, contains(ScannerChannelContract.scannerStop));
  });

  test('scanner event streams parse and filter broadcast events', () async {
    final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;
    var listenCalls = 0;

    messenger.setMockMessageHandler(scannerEventChannel.name, (ByteData? message) async {
      final call = codec.decodeMethodCall(message);
      if (call.method == 'listen') {
        listenCalls += 1;
        Future<void>.microtask(() {
          messenger.handlePlatformMessage(
            scannerEventChannel.name,
            codec.encodeSuccessEnvelope(<String, Object?>{
              'type': 'timeout',
              'timestampMs': 1710000000000,
            }),
            (_) {},
          );
          messenger.handlePlatformMessage(
            scannerEventChannel.name,
            codec.encodeSuccessEnvelope(<String, Object?>{
              'type': 'decoded',
              'data': 'SCANNED123',
              'timestampMs': 1710000000100,
            }),
            (_) {},
          );
        });
        return codec.encodeSuccessEnvelope(null);
      }
      if (call.method == 'cancel') {
        return codec.encodeSuccessEnvelope(null);
      }
      return null;
    });

    final scannerEventsA = platform.scannerEvents;
    final scannerEventsB = platform.scannerEvents;
    expect(identical(scannerEventsA, scannerEventsB), isTrue);

    final timeoutFuture = scannerEventsA.firstWhere(
      (event) => event.type == UrovoScannerEventType.timeout,
    );
    final decodedFuture = platform.scannerDecodedStream.first;

    final timeoutEvent = await timeoutFuture;
    final decoded = await decodedFuture;

    expect(timeoutEvent.type, UrovoScannerEventType.timeout);
    expect(decoded.data, 'SCANNED123');
    expect(listenCalls, 1);
  });

  test('scannerEvents throws typed error when event payload is not a map', () async {
    final messenger = TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger;

    messenger.setMockMessageHandler(scannerEventChannel.name, (ByteData? message) async {
      final call = codec.decodeMethodCall(message);
      if (call.method == 'listen') {
        Future<void>.microtask(() {
          messenger.handlePlatformMessage(
            scannerEventChannel.name,
            codec.encodeSuccessEnvelope('invalid'),
            (_) {},
          );
        });
        return codec.encodeSuccessEnvelope(null);
      }
      if (call.method == 'cancel') {
        return codec.encodeSuccessEnvelope(null);
      }
      return null;
    });

    await expectLater(
      platform.scannerEvents.first,
      throwsA(
        isA<UrovoPrinterException>()
            .having(
              (error) => error.type,
              'type',
              UrovoPrinterExceptionType.internal,
            )
            .having(
              (error) => error.message,
              'message',
              contains('Invalid scanner event payload'),
            ),
      ),
    );
  });

  test('throws internal exception when map payload is required but missing', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      return <String, Object?>{
        'code': 'ok',
        'message': 'OK',
        'data': 'not a map',
      };
    });

    await expectLater(
      platform.printerGetStatusDetail(),
      throwsA(
        isA<UrovoPrinterException>()
            .having(
              (error) => error.type,
              'type',
              UrovoPrinterExceptionType.internal,
            )
            .having(
              (error) => error.message,
              'message',
              contains('Invalid response payload'),
            ),
      ),
    );
  });

  test('maps platform exception with status details to typed exception', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      throw PlatformException(
        code: 'device_unavailable',
        details: <String, Object>{
          'statusDetail': <String, Object>{
            'status': 'paperEnded',
            'rawCode': 240,
            'message': 'out of paper',
            'recommendation': 'insert paper',
            'retryable': true,
          },
        },
      );
    });

    await expectLater(
      platform.printerStartPrint(),
      throwsA(
        isA<UrovoPrinterException>()
            .having(
              (error) => error.type,
              'type',
              UrovoPrinterExceptionType.deviceUnavailable,
            )
            .having(
              (error) => error.message,
              'message',
              'Platform exception on method channel.',
            )
            .having(
              (error) => error.statusDetail?.rawCode,
              'statusDetail.rawCode',
              240,
            ),
      ),
    );
  });

  test('maps error when status detail exists at root data map', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      return <String, Object?>{
        'code': 'print_failed',
        'message': 'failed',
        'data': <String, Object>{
          'status': 'paperEnded',
          'rawCode': 240,
          'message': 'out of paper',
          'recommendation': 'insert paper',
          'retryable': true,
        },
      };
    });

    await expectLater(
      platform.printerRunJob(UrovoPrintJob()..text('x')),
      throwsA(
        isA<UrovoPrinterException>()
            .having(
              (error) => error.type,
              'type',
              UrovoPrinterExceptionType.deviceUnavailable,
            )
            .having(
              (error) => error.statusDetail?.rawCode,
              'statusDetail.rawCode',
              240,
            ),
      ),
    );
  });
}
