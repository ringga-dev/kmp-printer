# 📄 Rancangan Fitur: Server-Driven Receipt & POS Template Engine

**Status**: Planned / Ongoing  
**Target Module**: `:printer` (Core) / `:printer-templates`  
**Target Platform**: Common Kotlin Multiplatform  

---

## 📌 1. Latar Belakang & Tujuan
Pada sistem POS (Point of Sale), restoran, dan e-commerce modern, format struk seringkali dikendalikan secara dinamis dari server (*Server-Driven UI / Receipts*). Format struk (logo toko, urutan teks, syarat & ketentuan, footer promo) dapat berubah kapan saja tanpa perlu merilis pembaruan aplikasi mobile/desktop.

Tujuan fitur ini:
1. **JSON/YAML to ESC/POS Engine**: Membaca payload JSON dari backend dan merendernya langsung menjadi byte ESC/POS secara otomatis.
2. **Built-in Standard POS Templates**: Menyediakan skema data siap pakai untuk format struk umum (Retail, Restoran/Dapur, Tiket Parkir, Nomor Antrean).

---

## 🏗️ 2. Skema & Model Data

### A. Skema JSON Template Struk (Server Payload)
```json
{
  "version": "1.0",
  "paper": { "width": 58, "autoCenter": true },
  "elements": [
    { "type": "header", "text": "WARUNG KOPI NIKMAT", "bold": true, "align": "CENTER", "size": [2, 2] },
    { "type": "text", "text": "Jl. Sudirman No. 45, Jakarta", "align": "CENTER" },
    { "type": "divider", "char": "=" },
    { "type": "key_value", "key": "No. Transaksi", "value": "TRX-20261008-01" },
    { "type": "key_value", "key": "Kasir", "value": "Andi" },
    { "type": "divider", "char": "-" },
    {
      "type": "table",
      "headers": ["Item", "Qty", "Harga"],
      "weights": [2, 1, 1],
      "rows": [
        ["Kopi Latte", "2", "40.000"],
        ["Croissant", "1", "25.000"]
      ]
    },
    { "type": "divider", "char": "=" },
    { "type": "key_value", "key": "TOTAL", "value": "Rp 65.000", "bold": true },
    { "type": "qrcode", "data": "https://qris.id/pay/12345", "align": "CENTER", "size": 6 },
    { "type": "text", "text": "Terima Kasih Atas Kunjungan Anda", "align": "CENTER" },
    { "type": "feed", "lines": 2 },
    { "type": "cut" }
  ]
}
```

---

## 💻 3. Contoh Desain API

### A. Rendering dari JSON String
```kotlin
val jsonString = fetchReceiptJsonFromBackend()

// Langsung cetak dari JSON
printer.printTemplate(device, jsonString)
```

### B. Menggunakan Template Built-in Berbasis Objek
```kotlin
val receipt = RetailReceiptData(
    storeName = "MINIMARKET JAYA",
    storeAddress = "Jl. Merdeka No. 10",
    invoiceNo = "INV-99201",
    cashierName = "Rudi",
    items = listOf(
        ReceiptItem("Beras 5kg", qty = 1, price = 75000),
        ReceiptItem("Minyak Goreng 2L", qty = 2, price = 34000)
    ),
    paymentMethod = "QRIS",
    totalPaid = 143000,
    qrData = "00020101021126580014ID.LINKAJA.WWW..."
)

// Cetak menggunakan template Retail standar
printer.print(device) {
    applyTemplate(ReceiptTemplate.Retail(receipt))
}
```

---

## 📋 4. Tahapan Implementasi (Checklist)
- [ ] Buat model data AST (*Abstract Syntax Tree*) untuk elemen struk: `ReceiptElement` (Header, Text, KeyValue, Table, Image, Barcode, QrCode, Divider, Feed, Cut).
- [ ] Buat parser JSON / kotlinx.serialization untuk menerjemahkan JSON payload menjadi `ReceiptElement`.
- [ ] Buat renderer yang mengeksekusi `List<ReceiptElement>` ke dalam `ESCPosCommandBuilder` dan `TsplCommandBuilder`.
- [ ] Buat model data dan template built-in:
  - `ReceiptTemplate.Retail`: Layout kasir toko ritel / minimarket.
  - `ReceiptTemplate.RestaurantOrder`: Layout order dapur dengan nomor meja besar & catatan khusus (*notes*).
  - `ReceiptTemplate.QueueTicket`: Layout nomor antrean dengan angka besar dan waktu tunggu.
  - `ReceiptTemplate.ParkingTicket`: Layout karcis parkir dengan barcode nomor plat.
- [ ] Tulis unit test untuk validasi parsing JSON dan kesesuaian output byte ESC/POS.
