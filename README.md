# Urovo POS

[![pub package](https://img.shields.io/pub/v/urovo_pos.svg)](https://pub.dev/packages/urovo_pos)
[![Coverage Status](https://coveralls.io/repos/github/ParhamHatan/Urovo-POS-Flutter/badge.svg)](https://coveralls.io/github/ParhamHatan/Urovo-POS-Flutter)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![GitHub Issues](https://img.shields.io/github/issues/ParhamHatan/Urovo-POS-Flutter)](https://github.com/ParhamHatan/Urovo-POS-Flutter/issues)
[![GitHub Stars](https://img.shields.io/github/stars/ParhamHatan/Urovo-POS-Flutter?style=social)](https://github.com/ParhamHatan/Urovo-POS-Flutter/stargazers)
[![Platform](https://img.shields.io/badge/Platform-Android-blue.svg)](https://flutter.dev)

Standalone Flutter plugin for Urovo POS devices, designed for incremental feature delivery.

`v0.1.0` implements **printing** only and is structured for future additions (scanner, beeper, pinpad) in the same package.

Tested on Urovo SDK version `v1.0.13`.

## v0.1.0 scope (printer)

- Runtime SDK availability check (`isUrovoSdkAvailable`)
- Printer lifecycle
  - `printerInit`
  - `printerClose`
  - `printerGetStatus`
  - `printerGetStatusDetail`
  - `printerSetGray`
  - `printerStartPrint`
- Print job pipeline
  - `printerRunJob(UrovoPrintJob)`
- Print primitives exposed via `UrovoPrintJob`
  - `text`
  - `blackLine`
  - `textLeftRight`
  - `textLeftCenterRight`
  - `barcode`
  - `qr`
  - `imageBytes`
  - `feedLine`
  - `paperFeed`
- Built-in demo helper: `printSample()`
  - `printSample()` only builds/runs the sample job; it does not auto-call `printerInit`/`printerClose`.

## Upcoming features (roadmap)

- `v0.2.0`: scanner APIs (scan start/stop + decoded payload stream)
- `v0.3.0`: beeper APIs + shared device status utilities
- `v0.4.0`: pinpad wrappers (non-sensitive operations only)
- `v0.5.0`: capability registry (`isPrinterAvailable`, `isScannerAvailable`, `isPinpadAvailable`)
- `v1.0.0`: stabilized printing + scanning contract

## Legal and licensing

This package does not include Urovo proprietary SDK binaries.
You must obtain Urovo SDK files under your own license agreement and add them to your Android app module.
You are responsible for complying with Urovo licensing and distribution terms.

## Android setup (add Urovo SDK AAR)

1. Copy Urovo AAR into your app module:

   `android/app/libs/urovoSDK-v1.0.13.aar`

2. In `android/app/build.gradle`:

```gradle
dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar", "*.aar"])
    // optional explicit:
    // implementation(name: "urovoSDK-v1.0.13", ext: "aar")
}
```

3. In `android/app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
    // optional explicit:
    // implementation(files("libs/urovoSDK-v1.0.13.aar"))
}
```

4. Optional (if your Gradle setup needs it):

```gradle
repositories {
    flatDir {
        dirs "libs"
    }
}
```

## Quick usage

```dart
import 'package:urovo_pos/urovo_pos.dart';

Future<void> printReceipt() async {
  final available = await UrovoPos.isUrovoSdkAvailable();
  if (!available) {
    throw Exception('Urovo SDK not found.');
  }

  final job = UrovoPrintJob()
    ..setGray(8)
    ..text(
      'UROVO POS',
      style: const UrovoTextStyle(
        align: UrovoAlign.center,
        bold: true,
        font: UrovoFont.large,
      ),
    )
    ..blackLine()
    ..textLeftCenterRight('Item A', 'x1', '100,000')
    ..barcode('123456789012')
    ..qr('https://urovo.example/receipt/1')
    ..feedLine(2);

  await UrovoPos.printerInit();
  try {
    await UrovoPos.printerRunJob(job);
  } finally {
    await UrovoPos.printerClose();
  }
}
```

## Lifecycle contract (manual)

This plugin uses manual lifecycle control. It does not auto-open or auto-close printer sessions.

Required sequence for printer transactions:

1. `printerInit()`
2. One or more operations:
   - `printerGetStatusDetail()`
   - `printerSetGray(...)`
   - `printerRunJob(...)`
   - `printerStartPrint()`
3. `printerClose()`

`printSample()` also requires an opened session:

```dart
await UrovoPos.printerInit();
try {
  await UrovoPos.printSample();
} finally {
  await UrovoPos.printerClose();
}
```

## Status mapping

Public Dart API does not expose raw Urovo status integers.

Mapped printer status enum:

- `ok`
- `paperEnded`
- `hardError`
- `overheat`
- `lowVoltage`
- `motorError`
- `busy`
- `unknown`

## Troubleshooting

- `sdk_not_found`: AAR is missing from app module `android/app/libs`.
- `not_initialized`: call `printerInit` before `printerGetStatusDetail`, `printerSetGray`, `printerStartPrint`, `printerRunJob`, or `printSample`.
- `device_unavailable`: printer init/status failed (paper, voltage, hardware, busy).
- `print_failed`: `startPrint` failed. Check status detail message and recommendation.
- `invalid_argument`: malformed job payload or invalid gray/input values.
