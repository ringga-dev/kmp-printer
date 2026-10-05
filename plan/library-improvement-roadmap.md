# Roadmap Pengembangan KmpPrinter

Status: 4 target aktif — Android, JVM, iOS, JS. Native desktop dan wasmJs
sudah dihapus. Test hijau: **82 JVM, 64 Android, 81 JS (Chrome headless)**.

## Progress

| Fase | Status | Hasil |
|---|---|---|
| 1 — Encoder charset | ✅ selesai | GBK/BIG5/CP437/Windows-1252/ISO-8859-1 benar di semua platform |
| 2 — WebSerial | ✅ selesai | `SERIAL` jalan di browser |
| 3 — Image helper web | ✅ selesai | logo/gambar bisa dicetak dari browser |
| 4 — Test jsMain & iosMain | ✅ selesai | 17 test browser + 7 test iOS, wired ke CI |

---

## Fase 1 — Encoder Charset ✅

**Selesai.** `jsMain` dan `iosMain` sekarang memakai encoder pure Kotlin yang
sama. Sebelumnya keduanya diam-diam jatuh ke UTF-8 di luar ASCII, sehingga
struk berbahasa Indonesia/China/Taiwan salah render di web dan iOS.

### Yang ditambahkan

| File | Isi |
|---|---|
| `tools/charset-gen/Oracle.java` | Dump decode table dari `java.nio.charset.Charset` |
| `tools/charset-gen/generate.mjs` | Generate tabel Kotlin dari dump tersebut |
| `CharsetTableSingleByte.kt` | CP437, ISO-8859-1, Windows-1252 (masing-masing 256 posisi) |
| `CharsetTableDoubleByte.kt` | GBK (23.940 entri), Big5 (13.721 entri) |
| `CharsetTables.kt` | Indeks lookup, dibangun lazy saat pertama dipakai |
| `CharsetEncoder.kt` | Dispatcher, gagal keras dengan `?` alih-alih fallback diam-diam |

### Keputusan yang diambil

**Java adalah sumber kebenaran, bukan `TextDecoder`.** `TextDecoder` diuji
terlebih dulu dan ditolak: ia berbeda dari Java pada 101 posisi GBK dan 458
posisi BIG5 (private-use code points, mapping alternatif, dan lubang padding).
Tabel di-*generate* dari Java supaya output byte-per-byte identik dengan yang
dipakai Android dan JVM.

**Tabel disimpan sebagai satu `String` Kotlin, bukan `Map`.** Indeks
`Char → Byte` dibangun sekali saat pertama kali dipakai (`by lazy`), lalu
di-cache. Menyimpan 37.000 entri sebagai `Map` langsung akan boros memory di
mobile; `String` cuma menyimpan satu salinan compact.

**Karakter tak terpetakan jadi `?`, bukan UTF-8.** Chauder yang bisa dicetak
lebih baik daripada struk berisi karakter rusak. Charset yang **tidak dikenal**
(mis. `"FOO"`) tetap jatuh ke UTF-8 supaya teksnya tidak hilang.

**Android/JVM tidak diubah.** `java.nio` lebih cepat dan sudah benar. Tabel
bersama dipakai sebagai test oracle lewat `CharsetOracleParityTest`.

### Test

`CharsetEncoderTest` (commonTest, jalan di keempat target) — 15 test:
ASCII konsisten lintas charset, vektor byte per charset, alias (`GB2312`,
`CP936`, `GB18030`, `CP950`), degradasi ke `?`, dan charset tak dikenal.

`CharsetOracleParityTest` (jvmTest) — membandingkan tabel dengan Java langsung
atas sampel luas (ASCII penuh, aksen Latin, box drawing, CJK spread, hiragana,
katakana). Menangkap regenerasi yang salah.

---

## Fase 2 — WebSerial ✅

**Selesai.** `SERIAL` sebelumnya jatuh ke stub. Sekarang jalan lewat Web Serial
API — pelengkap untuk `WebUsbConnector`, bukan penggantinya, karena banyak
printer thermal USB mengekspos bridge CH340/FTDI dan terdaftar sebagai port
serial, bukan USB printer class.

| File | Perubahan |
|---|---|
| `WebSerialConnector.kt` | baru — `requestPort()`, `open(baudRate)`, tulis via `getWriter()` |
| `PrinterConnectorFactory.web.kt` | `SERIAL -> WebSerialConnector()` |
| `PrinterPlatformDiagnostics.web.kt` | capability SERIAL → `supported`, hint & diagnoseSerial diperbarui |

Catatan: `readData` mengembalikan `null` — printer thermal umumnya write-only.
Tidak ada discovery; user selalu memilih port lewat dialog browser.

---

## Fase 3 — Image Helper Web ✅

**Selesai.** `processToRaster()` di JS sebelumnya melempar
`UnsupportedOperationException`, jadi logo/gambar tidak bisa dicetak sama sekali
di browser. Sekarang draw ke `<canvas>` dan baca via `getImageData`.

**Duplikasi dihapus, bukan ditambah.** Algoritma Floyd-Steinberg ternyata
sudah ada di `commonMain` (`applyFloydSteinberg`). `jvmMain` punya versi
`DoubleArray`-nya sendiri; sekarang keduanya memanggil implementasi bersama,
sehingga pixel yang sama menghasilkan byte yang sama di semua platform.

Canvas diisi putih lebih dulu — latar transparan akan terbaca sebagai hitam
setelah thresholding.

Menerima `HTMLImageElement`, `HTMLCanvasElement`, dan `ImageBitmap`, dengan
`processToRasterAsync()` untuk `<img>` yang belum selesai decode.

### Test

`ESCPosRasterTest` (commonTest) — 11 test: packing 8 pixel per byte MSB-first,
padding baris parsial, baris independen, hitam/putihsolid, threshold 128,
monotonic gradient, Atkinson, dan clamping levels.

---

## Fase 4 — Test jsMain & iosMain ✅

**Selesai.** Rupa-rupanya `jsBrowserTest` sudah punya Karma bawaan dari Kotlin
Gradle Plugin — yang belum ada hanya runner Chrome-nya. Setelah Chrome terpasang,
`./gradlew :printer:jsBrowserTest` langsung jalan: 81 test hijau di headless.

### Test browser (`printer/src/jsTest/`)

`WebPrinterFactoryTest` — 8 test. Routing factory (BLE & Bluetooth Classic
berbagi `WebBluetoothConnector`, USB, Serial, Virtual), konektor network
melapor gagal tanpa crash, dan konektor baru selalu mulai disconnected.
`connect()` sengaja **tidak** diuji: `requestPort()` hanya resolve dari user
gesture nyata, jadi di headless akan menggantung, bukan gagal cepat.

`WebPlatformDiagnosticsTest` — 9 test. Capability USB/BLE/Serial harus
`supported`, Network harus tidak, tidak ada transport browser yang boleh
mengiklankan `supportsDiscovery`, setiap transport punya hint, dan
`diagnoseSerial` menjelaskan bahwa port harus dipilih manual.

### Test iOS (`printer/src/iosTest/`)

`IosPlatformDiagnosticsTest` — 7 test. Network & BLE didukung, USB & Serial
tidak, hint tersedia untuk semua transport, factory resolve semua tipe tanpa
throw.

Kompilasi terverifikasi (`compileTestKotlinIosArm64` ✅). **Eksekusinya butuh
macOS**, jadi sudah wired ke job `build-ios` di `publish.yml`.

### CI

`publish.yml` — job `check` sekarang menjalankan `jsBrowserTest` dengan
Chrome dari `browser-actions/setup-chrome`; job `build-ios` menjalankan
`iosSimulatorArm64Test`. Paket untuk JS device diuji, bukan hanya dikompilasi.

### Perbaikan yang diperlukan

`CharsetEncoderTest` sempat gagal dikompilasi untuk JS: `String.encodeToByteArray()`
tidak ada di semua target. Test sudah ditulis ulang dengan byte eksplisit, yang
justru lebih baik — sekarang vektor yang diharapkan terlihat langsung di kode
test dan tidak bergantung pada encoder bawaan platform.

---

## Prinsip yang dipakai

- **Jangan ubah kontrak.** `encodeString`, `ESCPosImageHelper`,
  `PrinterConnectorFactory`, `PrinterConfig`, `PrinterConnector` — signature-nya
  dibekukan, hanya isi `actual` yang diganti.
- **commonMain hanya untuk logika yang benar-benar platform-agnostic.** Tabel
  charset dan dithering memenuhi itu.
- **Jangan diam-diam fallback.** Karakter tak terpetakan jadi `?`. Charset yang
  **tidak dikenal** jatuh ke UTF-8 tapi dicatat lewat logger, karena itu hampir
  selalu typo di config pemanggil. Ini sumber bug yang baru saja diperbaiki.
- **Bukti, bukan asumsi.** Tabel charset diverifikasi terhadap implementasi
  Java yang jadi acuan runtime, bukan terhadap `TextDecoder` yang sudah ditolak
  karena 559 posisi berbeda.

## Verifikasi per fase

Setiap fase harus lolos:

```powershell
.\gradlew.bat :printer:jvmTest :printer:testDebugUnitTest :printer:jsBrowserTest `
              :printer:compileKotlinJs :printer:compileKotlinJvm `
              :printer:compileDebugKotlinAndroid :printer:compileTestKotlinIosArm64 `
              :shared:compileKotlinJs :shared:compileKotlinJvm `
              :shared:compileDebugKotlinAndroid
```

`:printer:jsBrowserTest` butuh Chrome terpasang. Kalau tidak ada, jalankan di
CI atau tambahkan lewat `browser-actions/setup-chrome`.

Status terakhir: **BUILD SUCCESSFUL**, 82 test JVM + 64 Android + 81 JS hijau.
Kompilasi test iOS terverifikasi; eksekusinya dijadwalkan di `macos-latest`.