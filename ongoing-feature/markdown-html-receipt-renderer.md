# 📝 Rancangan Fitur: Markdown & HTML-to-Receipt Renderer

**Status**: Planned / Ongoing  
**Target Module**: `:printer`  
**Target Platform**: Common Kotlin Multiplatform (Pure Kotlin)  

---

## 📌 1. Latar Belakang & Tujuan
Banyak developer dan backend service menyimpan deskripsi catatan pesanan, syarat ketentuan struk, atau format cetak dalam bentuk teks **Markdown** atau potongan **HTML sederhana**. 

Tujuan fitur ini:
1. **Markdown to ESC/POS**: Mengonversi teks Markdown (headings, bold, tabel, divider, list) langsung menjadi perintah ESC/POS tanpa perlu parsing manual.
2. **Mini-HTML to ESC/POS**: Mengonversi subset tag HTML umum (`<h1>`-`<h3>`, `<b>`, `<u>`, `<center>`, `<table>`, `<tr>`, `<td>`, `<hr>`, `<img>`) menjadi layout struk yang rapi.

---

## 🏗️ 2. Sintaks yang Didukung

### A. Markdown Format
```markdown
# WARUNG MAKAN BU SITI
Jl. Kebon Jeruk No. 12
---
**Meja:** 05 | **Pelanggan:** Bpk. Hendra
---
| Menu | Qty | Harga |
| --- | :-: | ---: |
| Ayam Bakar | 2 | 50.000 |
| Es Teh Manis | 2 | 10.000 |
| Sambal Terasi | 1 | 5.000 |
---
**TOTAL:** Rp 65.000
*(Sudah termasuk PPN 10%)*

[x] Lunas via QRIS
```

### B. Mini-HTML Format
```html
<center>
  <h2>CAFE KITA</h2>
  <p>Free Wi-Fi: CafeKita_Guest</p>
</center>
<hr/>
<table>
  <tr><td>1x Kopi Tubruk</td><td align="right">15.000</td></tr>
  <tr><td>1x Roti Bakar</td><td align="right">20.000</td></tr>
</table>
<hr/>
<b>TOTAL: Rp 35.000</b>
```

---

## 💻 3. Contoh Desain API

```kotlin
val markdownContent = """
    # TOKO SERBA ADA
    ---
    **Tanggal:** 08/10/2026
    | Item | Qty | Subtotal |
    | Sabun Mandi | 2 | 16.000 |
    | Pasta Gigi | 1 | 12.500 |
    ---
    **Total:** Rp 28.500
    ---
    <center>Terima Kasih!</center>
""".trimIndent()

// 1. Cetak langsung string Markdown
printer.print(device) {
    markdown(markdownContent)
    feed(2)
    cut()
}

// 2. Cetak string HTML
printer.print(device) {
    html("<h2>STRUK TRANSAKSI</h2><hr/><p>Status: <b>LUNAS</b></p>")
    cut()
}
```

---

## 📋 4. Tahapan Implementasi (Checklist)
- [ ] Buat pure Kotlin lightweight Markdown Lexer & Parser (tanpa dependensi JVM Java AWT / Android UI).
- [ ] Pemetaan AST Markdown ke perintah builder:
  - `# Heading 1` ➔ `alignCenter()`, `bold(true)`, `fontSize(2, 2)`.
  - `**teks bold**` ➔ `withBold { text(teks) }`.
  - `---` ➔ `divider('-')`.
  - Tabel Markdown `| A | B |` ➔ `tableRow(...)` dengan deteksi perataan kolom kiri/tengah/kanan.
  - Tag `<center>` ➔ `alignCenter()`.
- [ ] Buat Mini-HTML Tag Tokenizer untuk tag: `h1-h6`, `b`, `i`, `u`, `center`, `p`, `br`, `hr`, `table`, `tr`, `td`, `img`.
- [ ] Tulis unit test untuk memvalidasi akurasi konversi Markdown/HTML ke byte ESC/POS.
