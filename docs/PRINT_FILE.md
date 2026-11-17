# 🖼️ Print File — Gambar & PDF

Mencetak **file langsung dari bytes**: gambar (PNG/JPEG/BMP/GIF/WebP) dan PDF.
Berguna saat struk datang sebagai file hasil unduhan atau pilihan galeri, bukan
teks yang disusun manual.

---

## Dukungan Platform

| Target | Gambar (PNG/JPEG/BMP/GIF/WebP) | PDF |
|---|:---:|:---:|
| **JVM / Desktop** | ✅ ImageIO | ✅ Apache PDFBox |
| **Android** | ✅ BitmapFactory | ✅ `android.graphics.pdf.PdfRenderer` |
| **iOS** | ✅ CGImageSource (ImageIO) | ✅ PDFKit |
| **Web (JS)** | ✅ decoder bawaan browser | ❌ tidak ada |

> **Catatan Web:** browser tidak punya rasterizer PDF bawaan. Membawa pdf.js
> (±1 MB + web worker) adalah keputusan packaging aplikasi, bukan library ini.
> `printPdf` mengembalikan `false` di web agar pemanggil bisa menampilkan pesan
> yang jelas, bukan diam-diam gagal.

---

## 1. Cetak File Gambar

`printImageFile` menerima bytes mentah dan membiarkan decoder platform yang
mengerjakan sisanya — tidak perlu membuat `Bitmap`/`UIImage`/`BufferedImage`
terlebih dahulu.

```kotlin
val imageBytes: ByteArray = download("https://cdn.toko.com/logo.png")

printer.print(profile = PrinterProfile.MM80) {
    printImageFile(imageBytes, center = true)
    feed(3)
    cut()
}
```

Kalau bytes bukan gambar yang bisa di-decode, fungsi mengembalikan `false`
dan **tidak ada yang dicetak** — bukan exception, jadi job lain di queue tidak
 ikut gagal.

## 2. Cetak PDF

```kotlin
val pdfBytes: ByteArray = download("https://cdn.toko.com/invoice.pdf")

printer.print(profile = PrinterProfile.MM80) {
    // Satu halaman
    printPdf(pdfBytes, pageIndex = 0)

    // Semua halaman, berurutan
    printPdfAll(pdfBytes)

    feed(3)
    cut()
}
```

`printPdfAll` membaca jumlah halaman lewat `pdfPageCount()`, jadi pemanggil tidak
perlu tahu jumlah halaman di muka.

## 3. Cetak File Apa Pun (Auto-Detect)

`printFile` menebak tipe dari **magic bytes**, jadi satu fungsi cukup untuk
gambar maupun PDF:

```kotlin
printer.print(profile = PrinterProfile.MM80) {
    // AUTO: deteksi dari magic bytes
    printFile(fileBytes)

    // Atau sebutkan eksplisit kalau sudah tahu
    printFile(fileBytes, type = PrintFileType.PNG)
    printFile(fileBytes, type = PrintFileType.PDF)
}
```

### Magic bytes yang dikenali

| Tipe | Signature |
|---|---|
| PNG | `89 50 4E 47 0D 0A 1A 0A` |
| JPEG | `FF D8 FF` |
| GIF | `47 49 46 38` (`GIF8`) |
| BMP | `42 4D` (`BM`) |
| WebP | `52 49 46 46 ?? ?? ?? ?? 57 45 42 50` |
| PDF | `25 50 44 46` (`%PDF`) |

WebP butuh 12 bytes karena tag `WEBP` berada di offset 8. Buffer yang lebih
pendek mengembalikan `null` (bukan tebakan).

---

## 4. Untuk Developer Web

Karena web tidak bisa merasterisasi PDF,render halaman sendiri lalu panggil
`printPdf` — atau gunakan `printFile` dengan tipe gambar yang sudah kamu
konversi.

---

## Referensi API

| Fungsi | Return | Keterangan |
|---|---|---|
| `printImageFile(data, center)` | `Boolean` | Decode + cetak gambar |
| `printPdf(data, pageIndex, center)` | `Boolean` | Render + cetak 1 halaman PDF |
| `printPdfAll(data, center)` | `Boolean` | Cetak semua halaman PDF |
| `printFile(data, type, center)` | `Boolean` | Auto-detect lalu cetak |
| `PrintFileType.fromBytes(data)` | `PrintFileType?` | Deteksi tipe dari magic bytes |
| `ESCPosRenderer.pdfPageCount(data)` | `Int` | Jumlah halaman PDF (`0` = tidak terbaca) |

Semua fungsi pengembalian `Boolean` bernilai `false` saat platform tidak bisa
memproses file — tidak ada yang melempar exception untuk file yang tidak bisa
dibaca.
