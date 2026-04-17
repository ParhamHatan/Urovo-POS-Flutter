import 'package:flutter/material.dart';
import 'package:urovo_pos/urovo_pos.dart';

class _TableItem {
  final String name;
  final int qty;
  final int unitPrice;

  const _TableItem({
    required this.name,
    required this.qty,
    required this.unitPrice,
  });
}

void main() {
  runApp(const UrovoExampleApp());
}

class UrovoExampleApp extends StatefulWidget {
  const UrovoExampleApp({super.key});

  @override
  State<UrovoExampleApp> createState() => _UrovoExampleAppState();
}

class _UrovoExampleAppState extends State<UrovoExampleApp> {
  static const String _persianFontAsset = 'assets/fonts/RaviFaNum-Regular.ttf';

  String _status = 'Ready';
  final List<String> _logs = <String>[];
  bool _isBusy = false;

  @override
  void initState() {
    super.initState();
    _checkSdk();
  }

  Future<void> _run(String label, Future<void> Function() action) async {
    if (_isBusy) {
      return;
    }
    setState(() {
      _isBusy = true;
      _status = label;
    });

    try {
      await action();
    } catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _status = 'Failed: $error';
        _logs.insert(0, '[ERROR] $label -> $error');
      });
    } finally {
      if (mounted) {
        setState(() {
          _isBusy = false;
        });
      }
    }
  }

  Future<void> _checkSdk() async {
    await _run('Checking SDK...', () async {
      final available = await UrovoPos.isUrovoSdkAvailable();
      setState(() {
        _status = available
            ? 'SDK available'
            : 'SDK missing. Place urovoSDK-v1.0.13.aar in example/android/app/libs.';
        _logs.insert(0, 'SDK available: $available');
      });
    });
  }

  Future<void> _initPrinter() async {
    await _run('Initializing printer...', () async {
      await UrovoPos.printerInit();
      setState(() {
        _status = 'Printer initialized';
        _logs.insert(0, 'printerInit: OK');
      });
    });
  }

  Future<void> _readStatus() async {
    await _run('Reading printer status...', () async {
      final detail = await UrovoPos.printerGetStatusDetail();
      setState(() {
        _status = detail.status.name;
        _logs.insert(
          0,
          'status: ${detail.status.name} | code=${detail.rawCode} | ${detail.message} | retryable=${detail.retryable}',
        );
      });
    });
  }

  Future<void> _printSample() async {
    await _run('Running printSample...', () async {
      await UrovoPos.printSample();
      setState(() {
        _status = 'Sample print done';
        _logs.insert(
          0,
          'printSample: OK @ ${DateTime.now().toIso8601String()}',
        );
      });
    });
  }

  Future<void> _printTextOnly() async {
    await _run('Running single text sample...', () async {
      final job = UrovoPrintJob()
        ..setGray(2)
        ..text(
          'UROVO TEXT SAMPLE',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            bold: true,
            font: UrovoFont.large,
          ),
        )
        ..feedLine(3);

      await UrovoPos.printerRunJob(job);
      setState(() {
        _status = 'Single text print done';
        _logs.insert(
          0,
          'singleTextPrint: OK @ ${DateTime.now().toIso8601String()}',
        );
      });
    });
  }

  Future<void> _printTableDemo() async {
    await _run('Running table print demo...', () async {
      const items = <_TableItem>[
        _TableItem(name: 'Burger', qty: 2, unitPrice: 180000),
        _TableItem(name: 'Fries', qty: 1, unitPrice: 95000),
        _TableItem(name: 'Cola', qty: 3, unitPrice: 45000),
      ];

      var total = 0;
      final job = UrovoPrintJob()
        ..setGray(0)
        ..text(
          'TABLE DEMO',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            bold: true,
            font: UrovoFont.large,
          ),
        )
        ..feedLine(1)
        ..blackLine()
        ..textLeftCenterRight(
          'Item',
          'Qty',
          'Price',
          style: const UrovoTextStyle(bold: true),
        )
        ..blackLine();

      for (final item in items) {
        final lineTotal = item.qty * item.unitPrice;
        total += lineTotal;
        job.textLeftCenterRight(item.name, 'x${item.qty}', '$lineTotal');
      }

      job
        ..blackLine()
        ..textLeftRight(
          'TOTAL',
          '$total',
          style: const UrovoTextStyle(bold: true, font: UrovoFont.large),
        )
        ..feedLine(3);

      await UrovoPos.printerRunJob(job);
      setState(() {
        _status = 'Table print done';
        _logs.insert(0, 'tablePrint: OK @ ${DateTime.now().toIso8601String()}');
      });
    });
  }

  Future<void> _printCustomDemo() async {
    await _run('Running custom receipt print...', () async {
      final job = UrovoPrintJob()
        ..setGray(2)
        ..text(
          'UROVO POS DEMO RECEIPT',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            bold: true,
            font: UrovoFont.large,
          ),
        )
        ..feedLine(1)
        ..blackLine()
        ..textLeftRight('Item', 'Price')
        ..textLeftCenterRight('Coffee', 'x1', '120,000')
        ..textLeftCenterRight('Cake', 'x1', '180,000')
        ..blackLine()
        ..textLeftRight(
          'TOTAL',
          '300,000',
          style: const UrovoTextStyle(bold: true, font: UrovoFont.large),
        )
        ..feedLine(1)
        ..text(
          'BARCODE TEST',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            bold: true,
          ),
        )
        ..barcode(
          '123456789012',
          align: UrovoAlign.center,
          type: UrovoBarcodeType.ean13,
        )
        ..text(
          'EAN13: 123456789012',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            font: UrovoFont.small,
          ),
        )
        ..feedLine(2)
        ..text(
          'QR TEST',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            bold: true,
          ),
        )
        ..qr(
          '222222222222222222222',
          expectedHeight: 220,
          align: UrovoAlign.center,
        )
        ..text(
          'QR payload: 222222222222222222222',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            font: UrovoFont.small,
          ),
        )
        ..feedLine(5);

      await UrovoPos.printerRunJob(job);
      setState(() {
        _status = 'Custom print done';
        _logs.insert(
          0,
          'customPrint: OK @ ${DateTime.now().toIso8601String()}',
        );
      });
    });
  }

  Future<void> _printCustomDemoPersian() async {
    await _run('Running Persian print demo...', () async {
      final job = UrovoPrintJob()
        ..setGray(3)
        ..text(
          'نمونه چاپ فارسی',
          style: const UrovoTextStyle(
            align: UrovoAlign.center,
            bold: true,
            font: UrovoFont.large,
            fontAsset: _persianFontAsset,
          ),
        )
        ..feedLine(1)
        ..text(
          'فروشگاه آزمایشی',
          style: const UrovoTextStyle(
            align: UrovoAlign.right,
            font: UrovoFont.normal,
            fontAsset: _persianFontAsset,
          ),
        )
        ..text(
          'تعداد: ۲',
          style: const UrovoTextStyle(
            align: UrovoAlign.right,
            font: UrovoFont.normal,
            fontAsset: _persianFontAsset,
          ),
        )
        ..text(
          'مبلغ کل: ۳۰۰٬۰۰۰',
          style: const UrovoTextStyle(
            align: UrovoAlign.right,
            bold: true,
            font: UrovoFont.large,
            fontAsset: _persianFontAsset,
          ),
        )
        ..feedLine(4);

      await UrovoPos.printerRunJob(job);
      setState(() {
        _status = 'Persian print done';
        _logs.insert(
          0,
          'persianPrint: OK @ ${DateTime.now().toIso8601String()}',
        );
      });
    });
  }

  Future<void> _closePrinter() async {
    await _run('Closing printer...', () async {
      await UrovoPos.printerClose();
      setState(() {
        _status = 'Printer closed';
        _logs.insert(0, 'printerClose: OK');
      });
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(title: const Text('Urovo POS Example')),
        body: Padding(
          padding: const EdgeInsets.all(24),
          child: ListView(
            children: <Widget>[
              Text(
                'Status: $_status',
                style: Theme.of(context).textTheme.titleMedium,
              ),
              const SizedBox(height: 16),
              Wrap(
                spacing: 12,
                runSpacing: 12,
                children: <Widget>[
                  ElevatedButton(
                    onPressed: _isBusy ? null : _checkSdk,
                    child: const Text('Check SDK'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _initPrinter,
                    child: const Text('Init Printer'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _readStatus,
                    child: const Text('Get Status'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _printSample,
                    child: const Text('Print Sample'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _printTextOnly,
                    child: const Text('Print Sample Text'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _printTableDemo,
                    child: const Text('Print Table Demo'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _printCustomDemo,
                    child: const Text('Print Demo Receipt'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _printCustomDemoPersian,
                    child: const Text('Print Persian Demo'),
                  ),
                  ElevatedButton(
                    onPressed: _isBusy ? null : _closePrinter,
                    child: const Text('Close Printer'),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              const Text('Logs'),
              const SizedBox(height: 8),
              if (_logs.isEmpty)
                const Text('No logs yet.')
              else
                ..._logs.map(
                  (entry) => Padding(
                    padding: const EdgeInsets.only(bottom: 6),
                    child: Text(entry),
                  ),
                ),
              const SizedBox(height: 24),
              if (_isBusy) const Center(child: CircularProgressIndicator()),
              const SizedBox(height: 16),
              const Text(
                'AAR expected at: example/android/app/libs/urovoSDK-v1.0.13.aar',
              ),
            ],
          ),
        ),
      ),
    );
  }
}
