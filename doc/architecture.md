# Architecture

## Design goals

- Publishable on pub.dev without bundling proprietary Urovo binaries.
- Runtime reflection boundary for vendor SDK integration.
- Feature-first structure so scanner/beeper/pinpad can be added without API churn.

## Current module layout (v0.1.x)

- Dart public facade:
  - `lib/urovo_pos.dart`
- Dart platform adapter:
  - `lib/urovo_pos_platform_interface.dart`
  - `lib/urovo_pos_method_channel.dart`
- Printer feature models/builders:
  - `lib/src/printer/urovo_printer_status.dart`
  - `lib/src/printer/urovo_printer_status_detail.dart`
  - `lib/src/printer/urovo_print_enums.dart`
  - `lib/src/printer/urovo_text_style.dart`
  - `lib/src/printer/urovo_print_job.dart`
  - `lib/src/printer/printer_channel_contract.dart`
- Exceptions:
  - `lib/src/exceptions/urovo_printer_exception.dart`
- Android plugin router:
  - `android/src/main/kotlin/com/urovo/pos/urovo_pos/UrovoPosPlugin.kt`
- Android reflection bridge:
  - `android/src/main/kotlin/com/urovo/pos/urovo_pos/printer/UrovoPrinterBridge.kt`

## Method channel contract

Channel: `urovo_pos/methods`

Current methods:

- `isUrovoSdkAvailable`
- `printerInit`
- `printerClose`
- `printerGetStatus`
- `printerGetStatusDetail`
- `printerSetGray`
- `printerStartPrint`
- `printerRunJob`

Android returns structured payloads for every handled method:

- `code`: `ok` or typed error code
- `message`: human-readable message
- `data`: method payload or error details

## Reflection boundary

Primary reflection target classes:

- `com.urovo.sdk.print.PrinterProviderImpl`
- `com.google.zxing.BarcodeFormat`

Optional runtime class for vendor status descriptions:

- `com.urovo.sdk.print.PrintStatus`

Bridge invokes methods like:

- `initPrint`, `close`, `getStatus`, `setGray`, `startPrint`
- `feedLine`, `paperFeed`, `addBlackLine`
- `addText`, `addTextLeft_Right`, `addTextLeft_Center_Right`
- `addBarCode`, `addQrCode`, `addImage`

## Error model

Platform error codes map to typed Dart exceptions:

- `sdk_not_found` -> `UrovoPrinterExceptionType.sdkNotFound`
- `invalid_argument` -> `UrovoPrinterExceptionType.invalidArgument`
- `device_unavailable` -> `UrovoPrinterExceptionType.deviceUnavailable`
- `print_failed` -> `UrovoPrinterExceptionType.printFailed`
- fallback -> `UrovoPrinterExceptionType.internal`

## Status model

Internal raw Urovo printer codes are converted to public enum values:

- `0` -> `ok`
- `240` -> `paperEnded`
- `242` -> `hardError`
- `243` -> `overheat`
- `225` -> `lowVoltage`
- `251` -> `motorError`
- `247` -> `busy`
- unknown -> `unknown`

Public API never exposes raw status integers directly.
