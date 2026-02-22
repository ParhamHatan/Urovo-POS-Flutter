import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/src/printer/printer_channel_contract.dart';
import 'package:urovo_pos/urovo_pos.dart';
import 'package:urovo_pos/urovo_pos_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('urovo_pos/methods');
  final platform = MethodChannelUrovoPos();

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
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
