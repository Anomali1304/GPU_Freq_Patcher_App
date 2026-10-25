# GPU Freq Patcher

<div align="center">

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)

A powerful, native Android application designed to inspect, modify, and patch ARM Mali GPU kernel modules (`.ko`) DVFS OPP (Operating Performance Point) frequency and voltage tables for MediaTek and Android devices.

</div>

---

## ✨ Key Features

- **ELF & Symbol Parsing**: Automatically scans ELF64 binaries and parses symbol tables (`g_default_gpu`, `g_gpu_default_opp_table`, etc.) to locate GPU OPP structs without manual offset guesswork.
- **Dynamic OPP Row Expansion & Reduction**: Add, delete, or modify OPP rows while automatically managing slot constraints and null-terminating unused slots for driver safety.
- **Graduated Overclocking (OC) Generator**: Create tiered overclocking steps above stock maximum frequency that precisely match natural stock step intervals, complete with **Auto-Sync** mode that dynamically adapts when rows are modified.
- **External `.ko` OPP Extraction & Cross-Device Fitting**: Directly extract real OPP tables from another device's compiled kernel module binary (`.ko`), automatically scaling and fitting the extracted table to your active module's slot allocation.
- **Voltage Offset Tuning (UV / OV)**: Apply global or per-entry voltage offsets in millivolts (mV) for undervolting (power savings) or overvoltaging (OC stability).
- **Real-time PLL Validation**: Validates frequency, `posdiv`, and `vsram` against hardware PLL constraints (e.g., MT6789 DVFS rules) before saving to prevent kernel crashes (`__gpufreq_abort`).
- **Visual Diff Inspector**: Side-by-side comparison between stock and modified OPP tables.
- **Modification History**: Persistent patch history tracking powered by Room Database.
- **Modern Glassmorphic M3 UI**: Crafted with Material Design 3, dynamic light/dark theming, and smooth physics animations.

---

## 📱 Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose & Material 3
- **Architecture**: MVVM (Model-View-ViewModel) with StateFlow
- **Concurrency**: Kotlin Coroutines & Flow
- **Local Persistence**: Room Database
- **Binary Processing**: Java NIO (`ByteBuffer`, `ByteOrder`)

---

## 🚀 Getting Started / Usage Guide

1. **Import Module**: Tap the **"Import Module (.ko)"** button to select your device's compiled GPU kernel module binary (e.g., `mtk_gpufreq.ko` or `mt_gpu.ko`).
2. **Inspect & Edit**: View detected hardware target, slot allocation, and frequency/voltage tiers. Edit individual OPP rows or use the **Tools** menu.
3. **Apply Presets / Overclock**:
   - Use **Load OPP .ko Lain** to extract and adapt the real OPP table directly from another device's `.ko` file.
   - Use **Generate OC** to build stepped overclocking tiers with automatic step matching.
   - Use **Offset Volt** for rapid undervolting / overvoltaging.
4. **Validate & Save**: The engine validates all parameters against hardware rules in real-time. Tap **Apply** to generate and save your patched `.ko` binary.

---

## 🛠️ Build & Development

Requirements:
- Android Studio Ladybug / Koala or newer
- JDK 17+
- Android SDK (API 24+)

To build the project locally via Gradle:
```bash
gradle :app:assembleDebug
```

To run local Robolectric & unit tests:
```bash
gradle :app:testDebugUnitTest
```

---

## ⚠️ Disclaimer

Kernel overclocking, undervolting, and modifying kernel modules (`.ko`) carry inherent risks of system instability, thermal throttling, reboots, or bootloops. Use this tool at your own risk and ensure you have backups of your stock kernel binaries.

---

## 📄 License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.
