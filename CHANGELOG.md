# Changelog

## [2.4.0] — minor release

### Added
- **Direct File & PDF Printing from Bytes**: Cetak file langsung dari raw bytes (`printFile`, `printImageFile`, `printPdf`, `printPdfAll`) dengan multiplatform renderer untuk Android (`PdfRenderer` & `BitmapFactory`), JVM (`PDFBox` & `ImageIO`), iOS (`PDFKit` & `UIImage`), dan Web (`HTML5 Canvas` & `PDF.js`).
- **Automatic Magic-Bytes File Type Detection**: `PrintFileType` mendeteksi format file secara otomatis (PNG, JPEG, GIF, BMP, WebP, PDF) tanpa bergantung pada ekstensi file atau MIME type.
- **TSPL Label Command Builder**: Dukungan protokol cetak label barcode/thermal TSPL (`tspl { ... }`) mencakup drawing text, barcode 1D, QR code, box, line, directional bitwise image printing, dan auto-commit `PRINT 1,1`.
- **Server-Driven Template Engine**: Deserializer template cetak dinamis berbasis AST dengan Pure-Kotlin recursive-descent JSON parser (`ReceiptTemplate.fromJson`) dan Map parser (`ReceiptTemplate.fromMap`) tanpa dependensi JSON eksternal, mendukung elemen header, key-value, table, barcode, qr, cut, dan feed.
- **Table Formatting & Word-Wrapping**: Penambahan `tableRowWrapped` dan `lineWrapped` dengan per-column custom alignment (`LEFT`, `CENTER`, `RIGHT`) pada `ESCPosCommandBuilder` agar teks panjang tidak terpotong (*silent truncation*).
- **Direct Hardware Controls**: Shortcut method `openCashDrawer(pin)` dan `beep(count, duration)` langsung pada `PrinterSession` dan `KmpPrinter`.
- **Exponential Backoff & Reconnect Policy**: Penambahan `ReconnectPolicy` dengan jitter dan backoff eksponensial murni KMP untuk stabilitas koneksi jaringan dan Bluetooth.
- **Extended ASCII Virtual Preview Renderer**: Support decoding byte extended `0x80..0xFF` untuk codepages CP437, ISO-8859-1, dan Windows-1252 pada ESC/POS virtual previewer.

### Fixed
- **OS Print Spooler Glitch (JVM)**: Mengirim seluruh payload byte array dalam satu `DocPrintJob` utuh, mengeliminasi chunking 512-byte yang memicu form-feed/line-cut berulang pada spooler Windows & CUPS.
- **Bluetooth Classic Double-Chunking (JVM)**: Mengeliminasi secondary chunking di layer hardware transport.
- **Thread-Safety & Race Condition**: Melindungi `DefaultPrinterRepository.printRaw()` dan `disconnect()` dengan `Mutex` reentrant-safe.
- **Android USB Non-Blocking Suspend**: Mengganti loop polling izin USB dengan `suspendCancellableCoroutine` dan dynamic `BroadcastReceiver` (`RECEIVER_NOT_EXPORTED` untuk Android 13+).
- **Multiplatform Memory & Pointer Safety (iOS/JS)**: Menggunakan safe UIImage decoding pada iOS, guard pinned memory buffer kosong, dan universal `new Blob([data])` pada Kotlin/JS.
- **Dithering & Monokrom Raster (Issue #5)**: Validasi dan verifikasi algoritma Floyd-Steinberg & Atkinson dithering serta bitonal raster packing.

## [2.3.6] — patch release

### Fixed
- **Margin overflow (Issue #1)**: saat `autoCenter=false`, `printWidth` di
  `ESCPosCommandBuilder.fromPrinterConfig` sekarang `dots - leftMargin - rightMargin`,
  sehingga `leftMargin + lebar teks + rightMargin` tidak lagi melebihi lebar kertas
  (sisi kanan tidak terpotong).
- `effectiveChars` selalu dihitung dari `printWidth / dotsPerChar`, bukan mentah
  `characterPerLine`.
- `centeringPadding` memakai `roundToInt()` (sebelumnya `toInt()`), sehingga tidak
  miring 1 dot untuk karakter ganjil.
- **Centering teks**: `centerText()` dan `centerWrapped()` sekarang memakai
  perataan HARDWARE (`ESC a 1`) untuk semua baris hasil wrap (termasuk >2 baris),
  bukan software padding. `ESCPosTextLayout.centeredText()` tidak lagi memotong
  teks (`take(safeMax)` dihapus) — teks di-word-wrap dulu, padding kiri/kanan seimbang.
- `ESCPosTextLayout.centerText()` memakai `wrapText` (potong di batas kata),
  bukan `chunked`.

### Added
- `rightMargin: Int = 0` pada `PrinterConfig`, `PrinterProfile`, dan
  `PrinterViewModel.updateRightMargin()`.
- `lineSpacing: Int = 0` pada `PrinterConfig`, `PrinterProfile`, `ESCPosConfig`
  (diterapkan via `ESC 3 n` di `initialize()` saat > 0), plus
  `PrinterViewModel.updateLineSpacing()`.
  (Catatan: versi ini dirilis sebagai patch 2.3.6 meskipun menambah API baru,
  sesuai keputusan maintainer.)
- `ESCPosTextLayout.wrapText(text, maxWidth): List<String>` — word-wrap pure Kotlin
  (normalisasi spasi, hard-split kata panjang, minimal satu elemen).
- `ESCPosCommandBuilder.centerTextSingleLine()` — perilaku centering versi lama
  (software padding) bila dibutuhkan.
- Test: `ESCPosCenteringTest` (14 kasus) mencakup wrap, keseimbangan padding,
  bracketing byte `ESC a 1`…`ESC a 0`, centering multi-baris, dan line spacing.

### Documentation
- Komentar `paperWidthDots` diperjelas: heuristic fallback `(paperWidth - 10) * 8`
  bisa meleset 10–20 dots; disarankan diisi eksplisit.
