import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/urovo_pos.dart';
import 'package:urovo_pos/urovo_pos_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  const channel = MethodChannel('urovo_pos/methods');
  final platform = MethodChannelUrovoPos();

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('maps sdk_not_found structured response', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      return <String, Object?>{
        'code': 'sdk_not_found',
        'message': 'missing sdk',
        'data': null,
      };
    });

    await expectLater(
      platform.printerInit(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.sdkNotFound,
        ),
      ),
    );
  });

  const knownStatusCases = <({
    String status,
    int rawCode,
    String message,
  })>[
    (status: 'paperEnded', rawCode: 240, message: 'out of paper'),
    (status: 'hardError', rawCode: 242, message: 'hardware error'),
    (status: 'overheat', rawCode: 243, message: 'printer too hot'),
    (status: 'lowVoltage', rawCode: 225, message: 'low battery'),
    (status: 'motorError', rawCode: 251, message: 'motor error'),
    (status: 'busy', rawCode: 247, message: 'printer busy'),
  ];

  for (final statusCase in knownStatusCases) {
    test(
      'maps print_failed with ${statusCase.status}(${statusCase.rawCode}) to deviceUnavailable',
      () async {
        TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel,
            (call) async {
          return <String, Object?>{
            'code': 'print_failed',
            'message': 'print failed',
            'data': <String, Object>{
              'statusDetail': <String, Object>{
                'status': statusCase.status,
                'rawCode': statusCase.rawCode,
                'message': statusCase.message,
                'recommendation': 'retry later',
                'retryable': true,
              },
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
                  'rawCode',
                  statusCase.rawCode,
                ),
          ),
        );
      },
    );
  }

  test('keeps unknown print_failed status as printFailed', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      return <String, Object?>{
        'code': 'print_failed',
        'message': 'print failed',
        'data': <String, Object>{
          'statusDetail': <String, Object>{
            'status': 'unknown',
            'rawCode': 999,
            'message': 'unknown issue',
            'recommendation': 'retry later',
            'retryable': false,
          },
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
              UrovoPrinterExceptionType.printFailed,
            )
            .having(
              (error) => error.statusDetail?.rawCode,
              'rawCode',
              999,
            ),
      ),
    );
  });

  test('maps not_initialized response', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, (call) async {
      return <String, Object?>{
        'code': 'not_initialized',
        'message': 'Call printerInit() first.',
        'data': null,
      };
    });

    await expectLater(
      platform.printerStartPrint(),
      throwsA(
        isA<UrovoPrinterException>().having(
          (error) => error.type,
          'type',
          UrovoPrinterExceptionType.notInitialized,
        ),
      ),
    );
  });
}
