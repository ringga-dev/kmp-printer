# Changelog

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
