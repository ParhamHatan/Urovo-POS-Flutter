## 0.1.2

- Added real POS feature-test run instructions in README (`Run example on real POS`).
- Hardened Android bridge behavior:
  - `printerClose` now surfaces close failures as typed errors.
  - Broadened `PrinterProviderImpl` reflection instantiation fallbacks.
- Updated integration/architecture docs to align with manual lifecycle and current version references.

## 0.1.1

- Added README badges (pub version, coverage, license, issues, stars, platform).
- Improved package metadata text in `pubspec.yaml`.

## 0.1.0

- Set printer as the first production feature (`v0.1.0`) with a modular structure for future features.
- Added printer-first public API:
  - `isUrovoSdkAvailable`
  - `printerInit`
  - `printerClose`
  - `printerGetStatus`
  - `printerGetStatusDetail`
  - `printerSetGray`
  - `printerStartPrint`
  - `printerRunJob`
  - `printSample`
- Added new status model (`UrovoPrinterStatus`) and status detail type (`UrovoPrinterStatusDetail`).
- Added structured MethodChannel response contract (`code`, `message`, `data`).
- Split Android code into router and reflection bridge (`UrovoPrinterBridge`) for easier future feature expansion.
- Updated README and docs with legal model, integration flow, and upcoming roadmap versions (`v0.2.0+`).
- Enforced printer lifecycle checks with explicit `not_initialized` errors for status/gray/startPrint/runJob before `printerInit`.
- Updated `printSample` and example flows to use managed lifecycle (`printerInit` + `printerClose`) and set gray level to `8`.
- Added scenario tests for lifecycle edge cases and channel error mapping.
