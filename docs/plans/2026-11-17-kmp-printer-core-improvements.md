# KmpPrinter Core Improvements & Bug Fixes Implementation Plan

> **For Claude / Agent:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Menuntaskan bug kritis (OS print spooler multi-job, race condition konkurensi repository, Android USB permission hang), memperbaiki kecacatan layout tabel dan preview charset, serta menyempurnakan fitur driver hardware (TSPL, kontrol laci/buzzer, template engine).

**Architecture:** Mempertahankan arsitektur murni Kotlin Multiplatform (KMP) tanpa ketergantungan `java.*` di `commonMain`. Menggunakan Unidirectional Data Flow, Coroutines/Flow, Mutex synchronization, dan TDD untuk memvalidasi setiap perbaikan pada level `commonTest` dan platform tests.

**Tech Stack:** Kotlin 2.1+, Kotlinx Coroutines, Kotlinx DateTime, Compose Multiplatform, JUnit/KotlinTest, AndroidX, Java Print Service / POSIX.

---

## 📋 Fase 1: Perbaikan Bug Kritis & Integritas Konkurensi (P0)

### Task 1: Bypass Chunking untuk OS Spooler di `JvmPrintServiceConnector` & Hilangkan Double Chunking
**Deskripsi Masalah**: `JvmPrintServiceConnector` mewarisi `BasePrinterConnector` yang memecah data per 512 byte dengan jeda 20ms. Karena `sendRawData` memanggil `DocPrintJob.print()`, struk yang berukuran > 512 byte akan membuat puluhan print job terpisah di Windows Spooler / CUPS. Ini menyebabkan printer termal memotong/ngefeed di tengah struk.
**Files:**
- Modify: `printer/src/jvmMain/kotlin/ngga/ring/printer/manager/JvmPrintServiceConnector.kt:58-86`
- Modify: `printer/src/jvmMain/kotlin/ngga/ring/printer/manager/JvmHardwareConnectors.kt:145-148`
- Test: `printer/src/jvmTest/kotlin/ngga/ring/printer/manager/JvmPrintServiceConnectorTest.kt`

**Step 1: Tulis unit test yang memvalidasi single-job dispatch**
Buat `JvmPrintServiceConnectorTest.kt` yang menguji bahwa payload berukuran 2048 byte dikirimkan sebagai 1 print job utuh, bukan terpotong-potong.

**Step 2: Jalankan test untuk memastikan test gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.manager.JvmPrintServiceConnectorTest`
Expected: FAIL

**Step 3: Implementasi perbaikan**
Di `JvmPrintServiceConnector`:
- Override `override suspend fun sendData(data: ByteArray): Boolean`: kirim `data` secara langsung ke `sendRawData(data)` dalam satu `DocPrintJob` utuh tanpa melalui pemotongan `while (offset < data.size)` di `BasePrinterConnector`.
Di `JvmBluetoothClassicConnector`:
- Jika delegasi ke `serialConnector`, pastikan tidak terjadi pemotongan ganda.

**Step 4: Jalankan test untuk memastikan test lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.manager.JvmPrintServiceConnectorTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "fix(jvm): send whole receipt as single print job in JvmPrintServiceConnector"`

---

### Task 2: Tambahkan Proteksi Mutex pada `DefaultPrinterRepository`
**Deskripsi Masalah**: `DefaultPrinterRepository.printRaw()` tidak memiliki Mutex, sehingga pemanggilan concurrent `printer.print()` dari beberapa coroutine akan menabrak `connectorFor()` dan `connectWithRetry()`, berisiko memutuskan koneksi yang sedang aktif (`current?.disconnect()`).
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/repository/DefaultPrinterRepository.kt:22-72`
- Test: `printer/src/commonTest/kotlin/ngga/ring/printer/DefaultPrinterRepositoryTest.kt`

**Step 1: Tulis test konkurensi di `DefaultPrinterRepositoryTest`**
Tambahkan test `concurrentPrintRawQueuesSequentially` di mana 5 coroutine memanggil `printRaw()` secara simultan dengan delay artificial di connector; verifikasi bahwa seluruh panggilan selesai tanpa tabrakan atau error disconnect.

**Step 2: Jalankan test untuk memverifikasi failure/race condition**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.DefaultPrinterRepositoryTest`

**Step 3: Implementasikan Mutex di `DefaultPrinterRepository`**
Tambahkan `private val mutex = Mutex()` di `DefaultPrinterRepository`. Bungkus blok eksekusi di dalam `printRaw` dengan `mutex.withLock`.

**Step 4: Jalankan test untuk memverifikasi kelulusan**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.DefaultPrinterRepositoryTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "fix(core): ensure thread-safe print operations with repository-level mutex"`

---

### Task 3: Perbaiki `DefaultPrinterRepository.monitorStatus()` Agar Mencoba Koneksi Jika Belum Konek
**Deskripsi Masalah**: `DefaultPrinterRepository.monitorStatus()` langsung me-return `PrinterStatus(isOnline = false)` jika `activeConnector == null`, tanpa pernah mencoba menghubungkan ke `config`.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/repository/DefaultPrinterRepository.kt:89-100`
- Test: `printer/src/commonTest/kotlin/ngga/ring/printer/DefaultPrinterRepositoryTest.kt`

**Step 1: Tulis test di `DefaultPrinterRepositoryTest`**
Verifikasi bahwa `monitorStatus(config)` berhasil mencoba koneksi awal ketika belum ada konektor aktif sebelumnya.

**Step 2: Jalankan test untuk memastikan test gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.DefaultPrinterRepositoryTest`

**Step 3: Implementasi**
Gunakan `connectorFor(normalizedConfig)` dan `connectWithRetry()` jika `connector == null || !connector.isConnected()`, sebelum memulai `statusMonitor.monitor(connector, intervalMs)`.

**Step 4: Jalankan test untuk memastikan kelulusan**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.DefaultPrinterRepositoryTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "fix(core): auto-connect in DefaultPrinterRepository.monitorStatus when disconnected"`

---

### Task 4: Perbaiki Alur Izin USB Android Menjadi Non-blocking Suspending dengan BroadcastReceiver
**Deskripsi Masalah**: `AndroidUsbConnector.connect()` langsung mengembalikan `false` saat izin belum ada, tanpa menunggu konfirmasi user dari dialog sistem.
**Files:**
- Modify: `printer/src/androidMain/kotlin/ngga/ring/printer/manager/AndroidUsbConnector.kt:50-66`

**Step 1: Refactor permintaan izin USB**
Gunakan `suspendCancellableCoroutine<Boolean>` dengan pendaftaran dinamis `BroadcastReceiver` untuk aksi `ACTION_USB_PERMISSION`.
Gunakan `ContextCompat.registerReceiver` dengan flag `RECEIVER_NOT_EXPORTED` (kompatibel Android 14+).
Begitu user memilih *Allow* atau *Deny*, resume coroutine dan buka device jika diizinkan.

**Step 2: Verifikasi kompilasi Android**
Run: `.\gradlew.bat :printer:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**
`git commit -am "fix(android): suspend and await user confirmation in AndroidUsbConnector permission request"`

---

### Task 5: Perbaiki Formatting Tanggal pada `ReceiptService`
**Deskripsi Masalah**: `ldt.month.toString()` menghasilkan nama enum bulan seperti `"NOVEMBER"`, bukan angka bulan (`ldt.monthNumber`).
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/ReceiptService.kt:144-157`
- Create: `printer/src/commonTest/kotlin/ngga/ring/printer/ReceiptServiceTest.kt`

**Step 1: Tulis unit test di `ReceiptServiceTest`**
Verifikasi `formatTimestamp` menghasilkan angka 2 digit untuk hari dan bulan (misal `17-11-2026`).

**Step 2: Jalankan test dan pastikan gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ReceiptServiceTest`

**Step 3: Perbaiki implementasi di `ReceiptService.kt`**
Ganti: `val month = ldt.monthNumber.toString().padStart(2, '0')`.

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ReceiptServiceTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "fix(core): use monthNumber instead of enum name in ReceiptService date formatter"`

---

## 🎨 Fase 2: Penyempurnaan Tata Letak (Layout), Formatting & Hardware (P1)

### Task 6: Tambahkan Perataan Kolom (Alignment) & Multi-line Cell Wrapping pada Tabel Struk
**Deskripsi Masalah**: `tableRow()` saat ini hanya mendukung rata kiri (`padEnd`). Angka/harga tidak bisa rata kanan. Nama item yang panjang terpotong paksa tanpa opsi wrap.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/util/escpos/ESCPosTextLayout.kt:130-163`
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/util/escpos/ESCPosCommandBuilder.kt:126-129`
- Create / Modify: `printer/src/commonTest/kotlin/ngga/ring/printer/ESCPosTableLayoutTest.kt`

**Step 1: Tulis test untuk tableRow dengan alignment per-kolom dan multi-line wrapping**
- Test 1: 3 kolom dengan alignments `[LEFT, CENTER, RIGHT]` menghasilkan padding yang presisi.
- Test 2: Nama item panjang membungkus ke baris baru sementara kolom lain terpadukan dengan rapi.

**Step 2: Jalankan test dan verifikasi gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ESCPosTableLayoutTest`

**Step 3: Implementasi**
- Tambahkan parameter `alignments: List<TextAlignment> = emptyList()` pada `ESCPosTextLayout.tableRow`.
- Tambahkan fungsi `ESCPosTextLayout.tableRowWrapped(columns, weights, alignments, maxCharsPerLine): List<String>`.
- Tambahkan helper `tableRowWrapped(...)` dan overload `tableRow(..., alignments)` pada `ESCPosCommandBuilder`.

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ESCPosTableLayoutTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "feat(escpos): add per-column alignment and multi-line wrapping to tableRow"`

---

### Task 7: Sediakan Opsi Auto-Wrap pada `ESCPosCommandBuilder.line()`
**Deskripsi Masalah**: `line(text)` memotong paksa string panjang menjadi 31 karakter tanpa opsi pembungkusan baris.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/util/escpos/ESCPosCommandBuilder.kt:100-120`
- Test: `printer/src/commonTest/kotlin/ngga/ring/printer/ESCPosCommandBuilderTest.kt`

**Step 1: Tulis test untuk `lineWrapped`**
Verifikasi kalimat 60 karakter di-render menjadi 2 baris `PreviewBlock.Text` dan byte LF yang sesuai.

**Step 2: Jalankan test dan verifikasi gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ESCPosCommandBuilderTest`

**Step 3: Implementasi**
Tambahkan `fun lineWrapped(text: String): ESCPosCommandBuilder` yang menggunakan `ESCPosTextLayout.wrapText()` untuk mencetak semua baris secara aman tanpa terpotong.

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ESCPosCommandBuilderTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "feat(escpos): introduce lineWrapped helper to prevent text truncation"`

---

### Task 8: Shortcut Perintah Hardware (`openCashDrawer`, `beep`) di Level `KmpPrinter` & `PrinterSession`
**Deskripsi Masalah**: Developer harus membuat `print { openCashDrawer() }` hanya untuk membuka laci kasir atau membunyikan buzzer.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/session/PrinterSession.kt`
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/session/DefaultPrinterSession.kt`
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/KmpPrinter.kt`
- Test: `printer/src/commonTest/kotlin/ngga/ring/printer/PrinterSessionTest.kt`

**Step 1: Tulis test di `PrinterSessionTest`**
Verifikasi pemanggilan `session.openCashDrawer(pin = 0)` mengirimkan byte kick drawer `0x1B, 0x70, 0x00, 0x32, 0xFF`.
Verifikasi pemanggilan `session.beep(count = 2, duration = 1)` mengirimkan byte beep yang sesuai.

**Step 2: Jalankan test dan pastikan gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.PrinterSessionTest`

**Step 3: Implementasikan fungsi di `PrinterSession`, `DefaultPrinterSession`, dan `KmpPrinter`**
Tambahkan:
```kotlin
suspend fun openCashDrawer(pin: Int = 0): Boolean
suspend fun beep(count: Int = 1, duration: Int = 1): Boolean
```
pada `PrinterSession`, serta shortcut delegasi pada `KmpPrinter`:
```kotlin
suspend fun openCashDrawer(device: PrinterDevice, pin: Int = 0): Flow<PrintStatus>
suspend fun beep(device: PrinterDevice, count: Int = 1, duration: Int = 1): Flow<PrintStatus>
```

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.PrinterSessionTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "feat(core): add direct openCashDrawer and beep shortcuts to KmpPrinter and PrinterSession"`

---

### Task 9: Decode Extended ASCII (0x80..0xFF) di `ESCPosVirtualRenderer` Menggunakan Charset Tables
**Deskripsi Masalah**: Byte non-ASCII diubah menjadi `'?'` saat preview raw byte.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/util/preview/ESCPosVirtualRenderer.kt:254-258`
- Test: `printer/src/commonTest/kotlin/ngga/ring/printer/ESCPosVirtualRendererTest.kt`

**Step 1: Tulis test di `ESCPosVirtualRendererTest`**
Uji byte yang mewakili karakter aksen atau simbol box-drawing (misal `0xA4` / `0xC4`) di-decode dengan benar, bukan menjadi `'?'`.

**Step 2: Jalankan test dan pastikan gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ESCPosVirtualRendererTest`

**Step 3: Implementasi**
Lacak perintah code page aktif (`ESC t n`) di dalam state machine `ESCPosVirtualRenderer`, lalu lakukan decode byte `0x80..0xFF` menggunakan `CharsetTableSingleByte` (misal CP437/Windows-1252) yang telah tersedia.

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.ESCPosVirtualRendererTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "fix(preview): decode extended ASCII bytes accurately in ESCPosVirtualRenderer"`

---

## 🚀 Fase 3: Fitur Driver Hardware Lanjutan & Template Engine (P2)

### Task 10: Sempurnakan Driver Label TSPL (`TsplCommandBuilder`)
**Deskripsi Masalah**: Tidak ada perintah bitmap/gambar pada label sticker TSPL (`rawPayloads` tidak digunakan), dan perintah `PRINT 1, 1` tidak otomatis ditambahkan jika user lupa.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/util/tspl/TsplCommandBuilder.kt:140-168`
- Test: `printer/src/commonTest/kotlin/ngga/ring/printer/TsplCommandBuilderTest.kt`

**Step 1: Tulis test untuk perintah `bitmap()` dan auto-print**
Uji bahwa pemanggilan `bitmap(x, y, width, height, mode, data)` menghasilkan sintaks `BITMAP X,Y,widthBytes,height,mode,payload`.
Uji bahwa `build()` secara default menyertakan `PRINT 1,1` jika belum ada perintah `PRINT`.

**Step 2: Jalankan test dan pastikan gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.TsplCommandBuilderTest`

**Step 3: Implementasi**
Tambahkan fungsi `bitmap(...)` pada `TsplCommandBuilder` dan periksa keberadaan perintah PRINT saat `build()`.

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.TsplCommandBuilderTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "feat(tspl): add bitmap graphic support and auto-print fallback to TsplCommandBuilder"`

---

### Task 11: Kembangkan Server-Driven Template Engine & AST Table
**Deskripsi Masalah**: `PrinterTemplate.kt` belum memiliki elemen Table, Feed, Cut, dan belum mendukung parsing JSON dinamis dari backend.
**Files:**
- Modify: `printer/src/commonMain/kotlin/ngga/ring/printer/util/template/PrinterTemplate.kt`
- Create: `printer/src/commonTest/kotlin/ngga/ring/printer/PrinterTemplateTest.kt`

**Step 1: Tulis test untuk parsing & rendering template struk**
Uji template yang memuat Header, KeyValue, Table, Barcode, QRCode, Feed, dan Cut.

**Step 2: Jalankan test dan pastikan gagal**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.PrinterTemplateTest`

**Step 3: Implementasi**
- Tambahkan `TemplateElement.Table(headers, weights, rows, alignments)`
- Tambahkan `TemplateElement.Cut(full: Boolean = true)`
- Tambahkan `TemplateElement.Feed(lines: Int = 1)`
- Hapus hardcoded `feed(3)` dan `cut()` di akhir fungsi `render()`, gantikan dengan elemen opsional.
- Sediakan helper parser JSON berbasis Map sederhana (zero-dependency KMP).

**Step 4: Jalankan test dan pastikan lolos**
Run: `.\gradlew.bat :printer:jvmTest --tests ngga.ring.printer.PrinterTemplateTest`
Expected: PASS

**Step 5: Commit**
`git commit -am "feat(template): add table, feed, cut elements and dynamic parsing to PrinterTemplate"`

---

## 🏁 Verifikasi Akhir Seluruh Fase

Setelah setiap fase selesai diimplementasikan, jalankan rangkaian uji regresi lengkap:

```powershell
.\gradlew.bat :printer:jvmTest :printer-ui:jvmTest :printer:testDebugUnitTest `
              :printer:compileKotlinJs :printer:compileKotlinJvm `
              :printer:compileDebugKotlinAndroid :printer:compileTestKotlinIosArm64 `
              :printer-ui:compileKotlinJvm :printer-ui:compileDebugKotlinAndroid
```
Semua target kompilasi dan unit test wajib berstatus **BUILD SUCCESSFUL**.
