/// Snapshot of shared Urovo/device runtime status.
class UrovoDeviceStatus {
  /// Whether `android.device.DeviceManager` is available.
  final bool deviceManagerAvailable;

  /// Android manufacturer value.
  final String manufacturer;

  /// Android brand value.
  final String brand;

  /// Android model value.
  final String model;

  /// Android device codename.
  final String device;

  /// Android release version string.
  final String androidVersion;

  /// Android SDK integer.
  final int androidSdkInt;

  /// Urovo product serial number, when available from DeviceManager.
  final String? serialNumber;

  /// Urovo TUSN/TID serial number, when available from DeviceManager.
  final String? tidSerialNumber;

  /// Docking station state when supported by the device.
  final bool? docked;

  /// Time when the native snapshot was produced.
  final DateTime timestamp;

  /// Creates a device status snapshot.
  const UrovoDeviceStatus({
    required this.deviceManagerAvailable,
    required this.manufacturer,
    required this.brand,
    required this.model,
    required this.device,
    required this.androidVersion,
    required this.androidSdkInt,
    required this.timestamp,
    this.serialNumber,
    this.tidSerialNumber,
    this.docked,
  });

  /// Whether the status includes any Urovo-specific identifier.
  bool get hasDeviceIdentifiers {
    return !_isBlank(serialNumber) || !_isBlank(tidSerialNumber);
  }

  /// Best-effort Urovo device hint based on Android build fields and identifiers.
  ///
  /// Use `UrovoPos.isUrovoSdkAvailable()` for the SDK availability check.
  bool get isLikelyUrovoDevice {
    final normalizedManufacturer = manufacturer.toLowerCase();
    final normalizedBrand = brand.toLowerCase();
    return normalizedManufacturer.contains('urovo') || normalizedBrand.contains('urovo') || hasDeviceIdentifiers;
  }

  /// Parses device status from method-channel map.
  factory UrovoDeviceStatus.fromMap(Map<dynamic, dynamic> map) {
    return UrovoDeviceStatus(
      deviceManagerAvailable: map['deviceManagerAvailable'] as bool? ?? false,
      manufacturer: map['manufacturer'] as String? ?? '',
      brand: map['brand'] as String? ?? '',
      model: map['model'] as String? ?? '',
      device: map['device'] as String? ?? '',
      androidVersion: map['androidVersion'] as String? ?? '',
      androidSdkInt: (map['androidSdkInt'] as num?)?.toInt() ?? 0,
      serialNumber: _nullIfBlank(map['serialNumber'] as String?),
      tidSerialNumber: _nullIfBlank(map['tidSerialNumber'] as String?),
      docked: map['docked'] as bool?,
      timestamp: DateTime.fromMillisecondsSinceEpoch(
        (map['timestampMs'] as num?)?.toInt() ?? DateTime.now().millisecondsSinceEpoch,
      ),
    );
  }

  /// Converts this object back to method-channel payload map.
  Map<String, Object?> toMap() {
    return <String, Object?>{
      'deviceManagerAvailable': deviceManagerAvailable,
      'manufacturer': manufacturer,
      'brand': brand,
      'model': model,
      'device': device,
      'androidVersion': androidVersion,
      'androidSdkInt': androidSdkInt,
      'serialNumber': serialNumber,
      'tidSerialNumber': tidSerialNumber,
      'docked': docked,
      'timestampMs': timestamp.millisecondsSinceEpoch,
    };
  }

  static bool _isBlank(String? value) {
    return value == null || value.trim().isEmpty;
  }

  static String? _nullIfBlank(String? value) {
    final trimmed = value?.trim();
    if (trimmed == null || trimmed.isEmpty) {
      return null;
    }
    return trimmed;
  }
}
