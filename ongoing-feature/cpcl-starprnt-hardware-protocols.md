# 🏷️ Rancangan Fitur: Protokol Hardware Lanjutan (CPCL, StarPRNT, Cash Drawer & Buzzer)

**Status**: Planned / Ongoing  
**Target Module**: `:printer`  
**Target Platform**: Android, iOS, JVM (Desktop), Web (JS/Wasm)  

---

## 📌 1. Latar Belakang & Tujuan
Selain ESC/POS dan TSPL, terdapat ekosistem perangkat keras khusus di industri retail, F&B, dan logistik internasional:
1. **CPCL (Compuprint / Zebra Programming Language)**: Protokol wajib pada printer mobile logistik (Zebra ZQ500/ZQ300 series, Bixolon, HPRT, Panda) yang digunakan kurir ekspedisi untuk mencetak resi jalan (waybill / shipping label).
2. **StarPRNT (Star Micronics Mode)**: Protokol khusus printer Star Micronics (TSP100, TSP650, mPOP) yang dominan pada sistem POS iPad/iOS dan cafe modern.
3. **Cash Drawer & Kitchen Buzzer**: Kontrol hardware pembuka laci uang otomatis (kick pin 2 / pin 5) dan bel notifikasi dapur.

---

## 🏗️ 2. Desain Protokol & Perintah

### A. CPCL Command Builder (`CpclCommandBuilder`)
Perintah standar CPCL untuk mobile printer:
* `! <offset> <dpi-x> <dpi-y> <height> <qty>` (Header job)
* `TEXT <font> <size> <x> <y> <data>` (Teks presisi titik)
* `BARCODE <type> <width> <ratio> <height> <x> <y> <data>` (Barcode resi)
* `BARCODE QR <x> <y> M 2 U <size> <data>` (QR Code)
* `BOX <x0> <y0> <x1> <y1> <width>` (Kotak label)
* `PRINT` / `FORM` (Eksekusi cetak)

### B. StarPRNT Emulation & Commands
* Command set Star Line Mode (`ESC *`, `ESC d`, `ESC GS #`).
* Raster mode berkecepatan tinggi untuk logo grafis.

### C. Cash Drawer & Hardware Beep Controller
* Pin 2 Kick: `0x1B, 0x70, 0x00, 0x19, 0xFA`
* Pin 5 Kick: `0x1B, 0x70, 0x01, 0x19, 0xFA`
* Buzzer Melody: `0x1B, 0x42, <times>, <duration>`

---

## 💻 3. Contoh Desain API

### A. Cetak Resi Logistik dengan CPCL
```kotlin
printer.printCpcl(device, heightDots = 600, copies = 1) {
    text(x = 30, y = 30, font = 7, size = 1, data = "J&T EXPRESS - EXP-99210")
    line(x0 = 30, y0 = 70, x1 = 540, y1 = 70, width = 3)
    barcode(x = 30, y = 90, type = CpclBarcode.CODE128, height = 80, data = "JT99210398210")
    box(x0 = 30, y0 = 200, x1 = 540, y1 = 450, width = 2)
    text(x = 45, y = 220, font = 7, size = 0, data = "Penerima: PT. MAJU BERSAMA")
    text(x = 45, y = 260, font = 7, size = 0, data = "Alamat: Gedung Cyber 2, Lt. 15, Jakarta")
    print()
}
```

### B. Buka Laci Kasir & Bunyikan Bel Dapur
```kotlin
// Buka laci kasir (Cash Drawer Kick)
printer.openCashDrawer(device, pin = CashDrawerPin.PIN_2)

// Bunyikan bel dapur 3 kali
printer.beep(device, times = 3, durationMs = 200)
```

---

## 📋 4. Tahapan Implementasi (Checklist)
- [ ] Buat `CpclCommandBuilder.kt` murni Kotlin untuk menyusun format resi CPCL.
- [ ] Buat enumerasi barcode CPCL (`CpclBarcode.CODE128`, `EAN13`, `QR`).
- [ ] Tambahkan helper `openCashDrawer(...)` dan `beep(...)` pada `KmpPrinter` dan `PrinterSession`.
- [ ] Buat modul adapter untuk StarPRNT emulation command set.
- [ ] Tulis unit test untuk validasi output format byte CPCL dan StarPRNT.
