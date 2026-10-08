---
title: "UI Components (kmp_printer-ui)"
description: "Ready-to-use, responsive, zero-bloat Compose Multiplatform UI components for thermal receipts and printer discovery"
---

# 🎨 UI Components (`kmp_printer-ui`)

`kmp_printer-ui` is an optional, lightweight UI extension module built with **pure JetBrains Compose Multiplatform**. It gives you production-ready, beautiful, and fully adaptive POS printer components with **zero unnecessary dependencies**.

---

## 📦 Installation

Add the dependency to your `commonMain` source set:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Core engine
            implementation("io.github.ringga-dev:kmp_printer:1.0.0")
            
            // UI Components (Compose Multiplatform)
            implementation("io.github.ringga-dev:kmp_printer-ui:1.0.0")
        }
    }
}
```

---

## 🧩 Key Components

### 1. 🧾 `ThermalReceiptPreview`
An authentic, customizable receipt preview component with paper tear zigzag edges, realistic shadow, pinch-to-zoom, and responsive layout.

```kotlin
import ngga.ring.printer.ui.components.ThermalReceiptPreview
import ngga.ring.printer.ui.theme.ReceiptPaperTheme

@Composable
fun ReceiptPreviewScreen() {
    ThermalReceiptPreview(
        theme = ReceiptPaperTheme.ClassicThermal, // ClassicThermal, FreshWhite, DarkTerminal, or Custom
        paperWidth = 58.dp, // Or 80.dp
        enablePinchZoom = true,
        headerSlot = {
            Text(
                text = "☕ KOPI KENANGAN",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Text(
                text = "Jl. Sudirman No. 42, Jakarta",
                fontSize = 12.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        bodySlot = {
            ReceiptDivider()
            ReceiptItemRow("1x Espresso Single", "Rp 22.000")
            ReceiptItemRow("1x Croissant Butter", "Rp 28.000")
            ReceiptDivider()
            ReceiptSummaryRow("Subtotal", "Rp 50.000")
            ReceiptSummaryRow("PB1 (10%)", "Rp 5.000")
            ReceiptSummaryRow("TOTAL", "Rp 55.000", isBold = true)
        },
        footerSlot = {
            ReceiptDivider()
            Text(
                text = "Terima Kasih Atas Kunjungan Anda!",
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    )
}
```

---

### 2. 🔍 `PrinterDiscoverySheet`
An adaptive printer scanner that automatically renders as a **Modal BottomSheet** on phones (Compact) and smoothly shifts to a **Centered Responsive Dialog** on Tablets & Desktop.

```kotlin
import ngga.ring.printer.ui.components.PrinterDiscoverySheet
import ngga.ring.printer.ui.state.rememberPrinterDiscoveryState

@Composable
fun CashierScreen(printer: KmpPrinter) {
    val discoveryState = rememberPrinterDiscoveryState(printer)
    var showScanner by remember { mutableStateOf(false) }

    Button(onClick = { 
        showScanner = true
        discoveryState.startDiscovery()
    }) {
        Text("Scan Printer")
    }

    if (showScanner) {
        PrinterDiscoverySheet(
            state = discoveryState,
            onDismissRequest = { 
                discoveryState.stopDiscovery()
                showScanner = false 
            },
            onDeviceSelected = { device ->
                println("Selected: ${device.name} (${device.address})")
                showScanner = false
            }
        )
    }
}
```

---

### 3. 🏷️ `PrinterStatusBadge` & `PrinterDeviceCard`
Ready-to-use status indicators and device cards with multi-transport badges (Bluetooth, USB, Network, BLE, Virtual).

```kotlin
import ngga.ring.printer.ui.components.PrinterStatusBadge
import ngga.ring.printer.ui.components.PrinterDeviceCard
import ngga.ring.printer.util.ConnectionState

@Composable
fun PrinterHeader(state: ConnectionState) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Active Printer: ")
        PrinterStatusBadge(state = state)
    }
}
```

---

## 🎨 Themes & Customization

You can fully customize paper color, typography, tear zigzag effects, and invert colors:

```kotlin
val CustomCyberpunkTheme = ReceiptPaperTheme(
    paperColor = Color(0xFF0F172A),
    textColor = Color(0xFF38BDF8),
    dividerColor = Color(0xFF1E293B),
    shadowElevation = 0.dp,
    showCutEdgeZigzag = true,
    invertedBackgroundColor = Color(0xFF38BDF8),
    invertedTextColor = Color(0xFF0F172A)
)
```

---

## 📐 Responsive Breakpoints (Zero Bloat)

`kmp_printer-ui` uses a built-in, pure Kotlin responsive engine using native Compose primitives:

| Breakpoint | Window Width | UI Adaptation |
|---|---|---|
| **Compact** | `< 600dp` | BottomSheets, Single Column, Auto-fit Paper |
| **Medium** | `600dp - 840dp` | Centered Modal Dialogs, 2-Column Grid |
| **Expanded** | `> 840dp` | Split Master-Detail Layouts, Persistent Scanner & Live Preview |
