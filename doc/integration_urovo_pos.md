# Urovo POS Integration

## 1. Add dependency

```yaml
dependencies:
  urovo_pos: ^0.2.0
```

For local workspace integration:

```yaml
dependencies:
  urovo_pos:
    path: ../urovo_pos
```

## 2. Add vendor SDK to Android app

Copy AAR into:

- `android/app/libs/urovoSDK-v1.0.13.aar`

In `android/app/build.gradle`:

```gradle
dependencies {
  implementation fileTree(dir: "libs", include: ["*.jar", "*.aar"])
}
```

In `android/app/build.gradle.kts`:

```kotlin
dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar", "*.aar"))))
}
```

## 3. Health check

```dart
final available = await UrovoPos.isUrovoSdkAvailable();
if (!available) {
  // show setup error
}

await UrovoPos.printerInit();
try {
  final status = await UrovoPos.printerGetStatusDetail();
  if (status.status != UrovoPrinterStatus.ok) {
    // show status.message + status.recommendation
  }
} finally {
  await UrovoPos.printerClose();
}
```

## 4. Print receipt

```dart
final job = UrovoPrintJob()
  ..setGray(5)
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
```

## 5. Error handling

Catch `UrovoPrinterException` and branch by type:

- `sdkNotFound`
- `invalidArgument`
- `deviceUnavailable`
- `printFailed`
- `internal`

Use `error.statusDetail` (if present) to display actionable guidance.

## 6. Scanner flow (v0.2.0)

```dart
final sub = UrovoPos.scannerEvents.listen((event) {
  switch (event.type) {
    case UrovoScannerEventType.decoded:
      final payload = event.result?.data ?? '';
      // use payload
      break;
    case UrovoScannerEventType.error:
      // show event.errorCode + event.message
      break;
    case UrovoScannerEventType.timeout:
      // show timeout UI
      break;
    case UrovoScannerEventType.canceled:
      // user canceled
      break;
    case UrovoScannerEventType.unknown:
      // fallback logging
      break;
  }
});

await UrovoPos.scannerStart(timeout: const Duration(seconds: 10));
// later...
await UrovoPos.scannerStop();
await sub.cancel();
```
