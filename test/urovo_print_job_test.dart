import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/urovo_pos.dart';

void main() {
  test('serializes print commands into a method channel payload', () {
    final job = UrovoPrintJob()
      ..setGray(5)
      ..text(
        'Hello',
        style: const UrovoTextStyle(align: UrovoAlign.center, bold: true),
      )
      ..textLeftRight('A', 'B')
      ..textLeftCenterRight('L', 'C', 'R')
      ..blackLine()
      ..imageBytes(Uint8List.fromList(<int>[1, 2, 3]), width: 120, height: 30)
      ..barcode('123456789012')
      ..qr('https://example.com')
      ..feedLine(2)
      ..paperFeed(10);

    final map = job.toMap();
    final commandsObject = map['commands'];
    expect(commandsObject, isA<List<Map<String, Object>>>());
    final commands = commandsObject! as List<Map<String, Object>>;

    expect(map['gray'], 5);
    expect(commands.length, 9);
    expect(commands[0]['type'], 'text');
    expect(commands[1]['type'], 'textLeftRight');
    expect(commands[2]['type'], 'textLeftCenterRight');
    expect(commands[3]['type'], 'blackLine');
    expect(commands[4]['type'], 'imageBytes');
    expect(commands[5]['type'], 'barcode');
    expect(commands[6]['type'], 'qr');
    expect(commands[7]['type'], 'feedLine');
    expect(commands[8]['type'], 'paperFeed');
  });

  test('rejects gray level out of 0..10 range', () {
    final job = UrovoPrintJob();
    expect(() => job.setGray(-1), throwsArgumentError);
    expect(() => job.setGray(11), throwsArgumentError);
  });

  test('throws when image bytes are empty', () {
    final job = UrovoPrintJob();
    expect(
      () => job.imageBytes(Uint8List(0), width: 10, height: 10),
      throwsArgumentError,
    );
  });
}
