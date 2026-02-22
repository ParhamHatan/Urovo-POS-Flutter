# Carbon POS Integration

## 1. Add dependency

```yaml
dependencies:
  urovo_pos: ^0.1.0
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

final status = await UrovoPos.printerGetStatusDetail();
if (status.status != UrovoPrinterStatus.ok) {
  // show status.message + status.recommendation
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

await UrovoPos.printerRunJob(job);
```

## 5. Error handling

Catch `UrovoPrinterException` and branch by type:

- `sdkNotFound`
- `invalidArgument`
- `deviceUnavailable`
- `printFailed`
- `internal`

Use `error.statusDetail` (if present) to display actionable guidance.
