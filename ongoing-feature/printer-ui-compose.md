# 🎨 Spesifikasi Lengkap & Desain Arsitektur: `kmp_printer-ui`

**Status**: Ready for Implementation  
**Artifact ID**: `io.github.ringga-dev:kmp_printer-ui`  
**Target Module**: `:printer-ui`  
**Core Dependency**: `:printer` (`io.github.ringga-dev:kmp_printer`)  
**Target Platform**: Android, iOS, Desktop (JVM), Web (Wasm/JS)  

---

## 📌 1. Prinsip Utama & Kebijakan Bebas Bloatware (Zero Unnecessary Dependencies)

Modul `kmp_printer-ui` dan core `kmp_printer` dirancang dengan **kebijakan nol dependensi pihak ketiga yang tidak perlu** (*Strict Zero-Bloat Policy*):

1. **Pure First-Party Compose Multiplatform**:  
   HANYA menggunakan dependensi resmi dari JetBrains Compose Multiplatform (`runtime`, `foundation`, `material3`, `ui`). Tidak menggunakan library UI pihak ketiga dari luar.
2. **Built-in Pure Kotlin Responsive Engine**:  
   Tidak menggunakan library eksternal untuk *WindowSizeClass* atau *Adaptive layouts*. Seluruh logika deteksi breakpoint layar ditulis murni (~35 baris) menggunakan `BoxWithConstraints` native Compose.
3. **Pure Kotlin Graphics & Text Engine**:  
   Seluruh kalkulasi gerigi kertas, bayangan, font monospace, dan sub-pixel rastering dibuat murni menggunakan primitive `androidx.compose.ui.graphics.Canvas` dan `Path` bawaan.
4. **Clean Architecture & Unidirectional Data Flow (UDF)**:  
   State selalu *hoisted*, tidak ada *side-effects* tersembunyi, dan komponen UI sepenuhnya *stateless* di level terendah.
5. **Themeable & Accessible**:  
   Mendukung tema kertas termal kustom, Dark/Light Mode adaptif, Dynamic Color, dan kepatuhan aksesibilitas kontras.

---

### 🛡️ Matriks Dependensi yang Diizinkan vs Dilarang

| Kategori | Yang Digunakan (First-Party & Minimal) | Yang DILARANG (Hindari Bloat) |
|---|---|---|
| **UI Framework** | `org.jetbrains.compose:compose-material3` | ❌ Library UI pihak ketiga luar (misal Moko-UI, Voyager, dsb) |
| **Responsive Layout** | Native Compose `BoxWithConstraints` | ❌ Library layout adapter eksternal |
| **Icons** | Standar Compose Material Icons | ❌ Third-party Icon pack besar |
| **Coroutines/State** | `kotlinx.coroutines` (Official) | ❌ Library reactive state pihak ketiga |
| **Rendering** | Compose Native `Canvas` & `Path` | ❌ External graphic rendering engines |

---

## 📱 2. Matriks Responsivitas & Adaptabilitas Layar

Komponen UI secara cerdas mengadaptasi tata letaknya berdasarkan **Window Size Class**:

```
 ┌───────────────────────┐   ┌─────────────────────────────────┐   ┌──────────────────────────────────────────────┐
 │ Compact (< 600dp)     │   │ Medium (600dp - 840dp)          │   │ Expanded (> 840dp)                           │
 │ (Smartphone Kasir)    │   │ (Tablet / POS Stand)            │   │ (Desktop / Large POS Kiosk / Web)            │
 ├───────────────────────┤   ├─────────────────────────────────┤   ├──────────────────────────────────────────────┤
 │ • Modal BottomSheet   │   │ • Side Sheet / Dialog Lebar     │   │ • 2-Column Split Master-Detail Layout        │
 │ • Single Column View  │   │ • 2-Column Grid Device List     │   │ • Persistent Left Scanner + Right Live Prevw │
 │ • Auto-fit Paper Width│   │ • Dual Action Floating Toolbar  │   │ • Multi-Printer Monitor Grid Dashboard       │
 └───────────────────────┘   └─────────────────────────────────┘   └──────────────────────────────────────────────┘
```

| Tipe Komponen | Compact (HP Kasir) | Medium (Tablet POS) | Expanded (Desktop / Kiosk) |
|---|---|---|---|
| **`PrinterDiscoverySheet`** | Modal BottomSheet (Swipe-to-dismiss) | Centered Adaptive Dialog (500dp) | Left Panel / Sidebar Drawer |
| **`ThermalReceiptPreview`** | Full-width scrollable with pinch-zoom | Centered paper container with shadows | Side-by-side Live Interactive Paper |
| **`PrinterStatusBadge`** | Compact icon + status dot | Full pill badge (Name + Status) | Expanded metric card with latency |
| **`PrintJobDialog`** | Minimal progress toast / sheet | Modal progress dialog with retry | Non-blocking bottom status banner |

---

## 🏗️ 3. Struktur Folder & Modul `:printer-ui`

```
printer-ui/
├── src/
│   └── commonMain/kotlin/ngga/ring/printer/ui/
│       ├── adaptive/                      # Helper breakpoint layar & WindowSize
│       │   ├── AdaptiveLayoutUtils.kt
│       │   └── WindowSizeClass.kt
│       ├── theme/                         # Sistem tema & kustomisasi kertas
│       │   ├── ReceiptPaperTheme.kt
│       │   ├── ReceiptColors.kt
│       │   └── ReceiptTypography.kt
│       ├── components/                    # Primitif komponen UI
│       │   ├── ThermalReceiptPreview.kt   # Widget visualisasi struk
│       │   ├── PrinterDiscoverySheet.kt   # Dialog pemindai perangkat
│       │   ├── PrinterDeviceCard.kt       # Kartu item perangkat responsif
│       │   ├── PrinterStatusBadge.kt      # Indikator status koneksi
│       │   ├── PaperCutDecoration.kt      # Dekorasi zigzag potong kertas
│       │   └── VirtualLineRenderer.kt     # Canvas sub-pixel renderer
│       └── model/                         # State holder UI (Immutable)
│           ├── DiscoveryUiState.kt
│           └── PreviewUiState.kt
```

---

## 🎛️ 4. Arsitektur Kustomisasi 3-Tingkat (3-Tier Customization & Slot API)

Agar developer memiliki keleluasaan penuh (*Maximum Developer Freedom*), modul ini menyediakan **3 level kustomisasi**:

```
┌─────────────────────────────────────────────────────────────────────────────────────────┐
│ Tier 1: Zero-Config Ready UI                                                            │
│ Panggil langsung PrinterDiscoverySheet() atau ThermalReceiptPreview() -> Langsung Jalan!│
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Tier 2: Slot-Based Customization (Slot API)                                             │
│ Ubah itemCard, header, emptyState, actionButtons, dan tema warna kertas sesuai brand.   │
├─────────────────────────────────────────────────────────────────────────────────────────┤
│ Tier 3: Headless / State-Only API                                                       │
│ Gunakan rememberPrinterDiscoveryState() -> Bebas bangun 100% UI kustom dari nol!       │
└─────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 🎨 5. Desain Sistem Tema Kertas (`ReceiptPaperTheme`)

Memungkinkan developer mengubah tampilan struk sesuai estetika aplikasi mereka:

```kotlin
@Immutable
data class ReceiptPaperTheme(
    val paperColor: Color = Color(0xFFFDFBF7),        // Krem hangat khas kertas termal
    val textColor: Color = Color(0xFF1E1E1E),         // Hitam arang tajam
    val dividerColor: Color = Color(0xFFCCCCCC),
    val shadowElevation: Dp = 4.dp,
    val cornerRadius: Dp = 6.dp,
    val showCutEdgeZigzag: Boolean = true,            // Efek potongan gerigi kertas
    val fontFamily: FontFamily = FontFamily.Monospace,
    val isDarkBackground: Boolean = false
) {
    companion object {
        val ClassicThermal = ReceiptPaperTheme()
        val FreshWhite = ReceiptPaperTheme(paperColor = Color(0xFFFFFFFF), shadowElevation = 2.dp)
        val DarkTerminal = ReceiptPaperTheme(
            paperColor = Color(0xFF18181B),
            textColor = Color(0xFF4ADE80),            // Hijau fosfor retro
            dividerColor = Color(0xFF27272A),
            isDarkBackground = true
        )
    }
}
```

---

## 💻 6. Spesifikasi Komponen, Slot API & Headless State

### A. `ThermalReceiptPreview` (Dengan Slot Kustomisasi)

```kotlin
@Composable
fun ThermalReceiptPreview(
    previewResult: PreviewResult,
    modifier: Modifier = Modifier,
    paperTheme: ReceiptPaperTheme = ReceiptPaperTheme.ClassicThermal,
    isZoomable: Boolean = true,
    // Slot Kustomisasi Bebas:
    headerContent: @Composable (() -> Unit)? = null,
    footerContent: @Composable (() -> Unit)? = null,
    actionButtons: @Composable (RowScope.() -> Unit)? = null,
    customLineRenderer: @Composable ((VirtualLine) -> Unit)? = null // Override per-baris jika mau
)
```

---

### B. `PrinterDiscoverySheet` (Dengan Slot API)

```kotlin
@Composable
fun PrinterDiscoverySheet(
    printer: KmpPrinter,
    onDismiss: () -> Unit,
    onDeviceSelected: (PrinterDevice) -> Unit,
    modifier: Modifier = Modifier,
    initialConnection: PrinterConnection = PrinterConnection.BLUETOOTH,
    availableTransports: List<PrinterConnection> = listOf(
        PrinterConnection.BLUETOOTH,
        PrinterConnection.BLE,
        PrinterConnection.USB,
        PrinterConnection.NETWORK
    ),
    // Slot Kustomisasi Tampilan:
    titleContent: @Composable (() -> Unit)? = null,
    emptyStateContent: @Composable (() -> Unit)? = null,
    itemCardContent: @Composable ((device: PrinterDevice, isSelected: Boolean, onSelect: () -> Unit) -> Unit)? = null,
    onTestPrintClick: ((PrinterDevice) -> Unit)? = null
)
```

---

### C. Headless State Controller: `rememberPrinterDiscoveryState`

Untuk developer yang ingin membuat **UI 100% dari nol** dengan desain sendiri, tapi tidak ingin repot mengurus logika coroutine scan, filtering, dan permission:

```kotlin
@Composable
fun rememberPrinterDiscoveryState(
    printer: KmpPrinter,
    initialTransport: PrinterConnection = PrinterConnection.BLUETOOTH
): PrinterDiscoveryState

// Contoh Penggunaan Headless (100% Custom UI):
@Composable
fun FullyCustomPosUi(printer: KmpPrinter) {
    val state = rememberPrinterDiscoveryState(printer)

    // State menyediakan data reaktif bersih:
    // state.devices (List<PrinterDevice>)
    // state.isScanning (Boolean)
    // state.selectedTransport (PrinterConnection)
    // state.selectTransport(PrinterConnection.USB)
    // state.startScan(), state.stopScan()

    LazyColumn {
        items(state.devices) { device ->
            // Bangun kartu kustom dengan desain Anda sendiri!
            MyBespokeBrandCard(
                name = device.name,
                address = device.address,
                onClick = { /* Hubungkan */ }
            )
        }
    }
}
```

#### Fitur Utama:
1. **Responsive Scaling**: Lebar kertas otomatis menyesuaikan rasio 58mm (384 dots) atau 80mm (576 dots) terhadap lebar layar.
2. **Sub-Pixel Line Rendering**: Merender teks bold, underline, invert mode, barcode, dan QR code secara presisi tanpa blur di layar Retina/HiDPI.
3. **Pinch-to-Zoom & Pan**: Pada layar sentuh HP/Tablet, user dapat memperbesar struk untuk membaca teks kecil.

---

### B. `PrinterDiscoverySheet` (Dialog Pemindai Adaptif)

BottomSheet di mobile, berubah menjadi Dialog modal di tablet dan Panel samping di Desktop:

```kotlin
@Composable
fun PrinterDiscoverySheet(
    printer: KmpPrinter,
    onDismiss: () -> Unit,
    onDeviceSelected: (PrinterDevice) -> Unit,
    modifier: Modifier = Modifier,
    initialConnection: PrinterConnection = PrinterConnection.BLUETOOTH,
    availableTransports: List<PrinterConnection> = listOf(
        PrinterConnection.BLUETOOTH,
        PrinterConnection.BLE,
        PrinterConnection.USB,
        PrinterConnection.NETWORK
    ),
    onTestPrintClick: ((PrinterDevice) -> Unit)? = null
)
```

#### Fitur Utama:
1. **Automated Permissions**: Meminta izin Bluetooth / Nearby Devices / Location otomatis saat tab Bluetooth dibuka.
2. **Smart Filtering**: Tab navigasi halus dengan pencarian real-time dan indikator loading spinner.
3. **Signal & Type Indicator**: Menampilkan kekuatan sinyal BLE (dBm), status pairing Bluetooth, atau status port IP.
4. **Direct Test Print**: Tombol kecil "Tes Cetak" di tiap kartu printer untuk memastikan printer merespons sebelum dipilih.

---

### C. `PrinterStatusBadge` (Widget Status Reaktif)

```kotlin
@Composable
fun PrinterStatusBadge(
    connectionState: ConnectionState,
    printerStatus: PrinterStatus? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
```

---

## 🖥️ 6. Contoh Implementasi Layar Adaptif (Master-Detail POS)

Contoh layout kasir yang otomatis menyesuaikan tampilan dari Smartphone ke Tablet/Desktop:

```kotlin
@Composable
fun PosAdaptivePrinterScreen(
    printer: KmpPrinter,
    receiptBytes: ByteArray,
    config: PrinterConfig
) {
    val windowSize = rememberWindowSizeClass() // Compact / Medium / Expanded
    var selectedDevice by remember { mutableStateOf<PrinterDevice?>(null) }
    var isScanning by remember { mutableStateOf(false) }

    when (windowSize) {
        // 1. HP / Layar Kecil: Tampilan Single Column + BottomSheet
        WindowSizeClass.Compact -> {
            Box(modifier = Modifier.fillMaxSize()) {
                ThermalReceiptPreview(
                    previewResult = ESCPosVirtualRenderer.render(receiptBytes, config),
                    modifier = Modifier.fillMaxSize()
                )

                FloatingActionButton(
                    onClick = { isScanning = true },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Cari Printer")
                }

                if (isScanning) {
                    PrinterDiscoverySheet(
                        printer = printer,
                        onDismiss = { isScanning = false },
                        onDeviceSelected = { device ->
                            selectedDevice = device
                            isScanning = false
                        }
                    )
                }
            }
        }

        // 2. Tablet / Desktop / Kiosk: Tampilan 2-Kolom Split Layar Bersisian
        WindowSizeClass.Medium, WindowSizeClass.Expanded -> {
            Row(modifier = Modifier.fillMaxSize()) {
                // Kolom Kiri: Manajemen & Pemindai Printer
                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    PrinterScannerPane(
                        printer = printer,
                        selectedDevice = selectedDevice,
                        onDeviceSelected = { selectedDevice = it }
                    )
                }

                // Kolom Kanan: Live Struk Preview & Eksekusi Cetak
                Surface(
                    modifier = Modifier.weight(1.2f).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        ThermalReceiptPreview(
                            previewResult = ESCPosVirtualRenderer.render(receiptBytes, config),
                            modifier = Modifier.weight(1f)
                        )
                        
                        Button(
                            onClick = { selectedDevice?.let { printer.printRaw(it, receiptBytes) } },
                            enabled = selectedDevice != null,
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        ) {
                            Text("Cetak ke ${selectedDevice?.name ?: "Pilih Printer Dahulu"}")
                        }
                    }
                }
            }
        }
    }
}
```

---

## 📦 7. Konfigurasi Gradle & Penerbitan Artefak

### `settings.gradle.kts`
```kotlin
include(":printer")
include(":printer-ui")
```

### `printer-ui/build.gradle.kts`
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("maven-publish")
    id("signing")
}

kotlin {
    androidTarget { publishLibraryVariants("release") }
    jvm()
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework { baseName = "KmpPrinterUi" }
    }
    js(IR) { browser() }

    sourceSets {
        commonMain.dependencies {
            api(project(":printer")) // Transitif ke pengguna
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
        }
    }
}
```

---

## 📋 8. Checklist Verifikasi & Quality Assurance (QA)

- [ ] **Adaptive Verification**: Komponen diuji di HP (Android/iOS), Tablet (7"-11"), dan Desktop Window resize.
- [ ] **Dark Mode & Contrast Testing**: Memastikan keterbacaan teks termal tetap tajam di mode gelap & terang.
- [ ] **Reconnection & State Resilience**: Animasi status badge bereaksi mulus saat kabel USB dicabut atau Bluetooth terputus.
- [ ] **Zero Memory Leak**: Memastikan coroutine scanner dibatalkan secara bersih saat BottomSheet ditutup (*onDismiss*).
