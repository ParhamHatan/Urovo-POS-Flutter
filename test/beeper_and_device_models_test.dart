import 'package:flutter_test/flutter_test.dart';
import 'package:urovo_pos/urovo_pos.dart';

void main() {
  test('beeper pattern wire values round trip', () {
    expect(UrovoBeeperPattern.short.wireValue, 'short');
    expect(UrovoBeeperPattern.success.wireValue, 'success');
    expect(UrovoBeeperPattern.warning.wireValue, 'warning');
    expect(UrovoBeeperPattern.error.wireValue, 'error');

    expect(
      UrovoBeeperPatternWire.fromWireValue('success'),
      UrovoBeeperPattern.success,
    );
    expect(
      UrovoBeeperPatternWire.fromWireValue('warning'),
      UrovoBeeperPattern.warning,
    );
    expect(
      UrovoBeeperPatternWire.fromWireValue('error'),
      UrovoBeeperPattern.error,
    );
    expect(
      UrovoBeeperPatternWire.fromWireValue('unknown'),
      UrovoBeeperPattern.short,
    );
  });

  test('device status parses identifiers and derived Urovo hint', () {
    final status = UrovoDeviceStatus.fromMap(<String, Object?>{
      'deviceManagerAvailable': true,
      'manufacturer': 'Generic',
      'brand': 'Brand',
      'model': 'Model',
      'device': 'device',
      'androidVersion': '13',
      'androidSdkInt': 33,
      'serialNumber': '  SN123  ',
      'tidSerialNumber': ' ',
      'docked': false,
      'timestampMs': 1710000000000,
    });

    expect(status.deviceManagerAvailable, isTrue);
    expect(status.serialNumber, 'SN123');
    expect(status.tidSerialNumber, isNull);
    expect(status.docked, isFalse);
    expect(status.hasDeviceIdentifiers, isTrue);
    expect(status.isLikelyUrovoDevice, isTrue);
    expect(status.toMap()['serialNumber'], 'SN123');
    expect(status.toMap()['timestampMs'], 1710000000000);
  });

  test('device status uses build hints when identifiers are missing', () {
    final status = UrovoDeviceStatus.fromMap(<String, Object?>{
      'deviceManagerAvailable': false,
      'manufacturer': 'Urovo',
      'brand': 'Unknown',
      'model': 'Model',
      'device': 'device',
      'androidVersion': '13',
      'androidSdkInt': 33,
    });

    expect(status.hasDeviceIdentifiers, isFalse);
    expect(status.isLikelyUrovoDevice, isTrue);
  });
}
