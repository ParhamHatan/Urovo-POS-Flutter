/// Method-channel implementation internals for the `urovo_pos` plugin.
library;

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:urovo_pos/src/exceptions/urovo_printer_exception.dart';
import 'package:urovo_pos/src/printer/printer_channel_contract.dart';
import 'package:urovo_pos/src/printer/urovo_print_job.dart';
import 'package:urovo_pos/src/printer/urovo_printer_status.dart';
import 'package:urovo_pos/src/printer/urovo_printer_status_detail.dart';
import 'package:urovo_pos/urovo_pos_platform_interface.dart';

/// Default Android method-channel implementation for [UrovoPosPlatform].
class MethodChannelUrovoPos extends UrovoPosPlatform {
  /// Creates a method-channel backed platform implementation.
  MethodChannelUrovoPos();

  /// Underlying method channel used by this implementation.
  @visibleForTesting
  final MethodChannel methodChannel = const MethodChannel(
    PrinterChannelContract.channelName,
  );

  @override
  Future<bool> isUrovoSdkAvailable() async {
    final data = await _invokeData(PrinterChannelContract.isUrovoSdkAvailable);
    return data is bool && data;
  }

  @override
  Future<void> printerInit() async {
    await _invokeData(PrinterChannelContract.printerInit);
  }

  @override
  Future<UrovoPrinterStatus> printerGetStatus() async {
    final detail = await printerGetStatusDetail();
    return detail.status;
  }

  @override
  Future<UrovoPrinterStatusDetail> printerGetStatusDetail() async {
    final data = await _invokeData(PrinterChannelContract.printerGetStatusDetail);
    final map = _asMap(data, method: PrinterChannelContract.printerGetStatusDetail);
    return UrovoPrinterStatusDetail.fromMap(map);
  }

  @override
  Future<void> printerSetGray(int level) async {
    await _invokeData(PrinterChannelContract.printerSetGray, <String, Object>{
      'gray': level,
    });
  }

  @override
  Future<void> printerStartPrint() async {
    await _invokeData(PrinterChannelContract.printerStartPrint);
  }

  @override
  Future<void> printerRunJob(UrovoPrintJob job) async {
    await _invokeData(PrinterChannelContract.printerRunJob, job.toMap());
  }

  @override
  Future<void> printerClose() async {
    await _invokeData(PrinterChannelContract.printerClose);
  }

  Future<dynamic> _invokeData(
    String method, [
    Map<String, Object>? arguments,
  ]) async {
    try {
      final raw = await methodChannel.invokeMethod<dynamic>(method, arguments);
      final response = _UrovoPlatformResponse.fromRaw(raw);
      if (response.code == _UrovoPlatformResponse.okCode) {
        return response.data;
      }
      throw _toException(response);
    } on PlatformException catch (error) {
      throw _fromPlatformException(error);
    }
  }

  UrovoPrinterException _fromPlatformException(PlatformException error) {
    final response = _UrovoPlatformResponse(
      code: error.code,
      message: error.message ?? 'Platform exception on method channel.',
      data: error.details,
    );
    return _toException(response);
  }

  UrovoPrinterException _toException(_UrovoPlatformResponse response) {
    final detail = _extractStatusDetail(response.data);
    final isKnownPrinterStatusFailure = response.code == 'print_failed' &&
        detail != null &&
        detail.status != UrovoPrinterStatus.unknown &&
        detail.status != UrovoPrinterStatus.ok;

    final type = switch (response.code) {
      'sdk_not_found' => UrovoPrinterExceptionType.sdkNotFound,
      'not_initialized' => UrovoPrinterExceptionType.notInitialized,
      'invalid_argument' => UrovoPrinterExceptionType.invalidArgument,
      'device_unavailable' => UrovoPrinterExceptionType.deviceUnavailable,
      'print_failed' when isKnownPrinterStatusFailure => UrovoPrinterExceptionType.deviceUnavailable,
      'print_failed' => UrovoPrinterExceptionType.printFailed,
      _ => UrovoPrinterExceptionType.internal,
    };
    return UrovoPrinterException(
      type: type,
      message: response.message,
      statusDetail: detail,
    );
  }

  UrovoPrinterStatusDetail? _extractStatusDetail(dynamic data) {
    if (data is! Map) {
      return null;
    }

    final map = Map<dynamic, dynamic>.from(data);

    if (map['statusDetail'] is Map) {
      return UrovoPrinterStatusDetail.fromMap(
        Map<dynamic, dynamic>.from(map['statusDetail'] as Map),
      );
    }

    if (map['status'] is String || map['rawCode'] != null) {
      return UrovoPrinterStatusDetail.fromMap(map);
    }

    return null;
  }

  Map<dynamic, dynamic> _asMap(
    dynamic data, {
    required String method,
  }) {
    if (data is Map) {
      return Map<dynamic, dynamic>.from(data);
    }

    throw UrovoPrinterException(
      type: UrovoPrinterExceptionType.internal,
      message: 'Invalid response payload for $method. Expected map data.',
    );
  }
}

class _UrovoPlatformResponse {
  static const String okCode = 'ok';

  final String code;
  final String message;
  final dynamic data;

  const _UrovoPlatformResponse({
    required this.code,
    required this.message,
    this.data,
  });

  factory _UrovoPlatformResponse.fromRaw(dynamic raw) {
    if (raw is! Map) {
      return _UrovoPlatformResponse(code: okCode, message: 'OK', data: raw);
    }

    final map = Map<dynamic, dynamic>.from(raw);
    return _UrovoPlatformResponse(
      code: map['code'] as String? ?? okCode,
      message: map['message'] as String? ?? 'OK',
      data: map['data'],
    );
  }
}
