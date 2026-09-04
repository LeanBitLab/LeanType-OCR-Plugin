# LeanType OCR Plugin

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![ML Kit](https://img.shields.io/badge/ML%20Kit-Offline%20v2-orange.svg)](https://developers.google.com/ml-kit/vision/text-recognition/v2)

An offline Optical Character Recognition (OCR) plugin for **[LeanType](https://github.com/LeanBitLab/LeanType)** (and HeliBoard), enabling instant on-device text recognition directly from your keyboard.

---

## ✨ Features

- **🔒 100% Offline & Private**: Zero network permissions. All vision and neural models run entirely on-device with zero data telemetry.
- **⚡ Bundled ML Kit V2 Engines**: Uses Google ML Kit's bundled offline models for high-accuracy recognition across multiple scripts:
  - **Latin** (English, Spanish, French, German, etc.)
  - **Devanagari** (Hindi, Marathi, Sanskrit, etc.)
  - **Chinese** (Simplified & Traditional)
  - **Japanese** (Kanji, Hiragana, Katakana)
  - **Korean** (Hangul)
- **🧩 Lightweight Plugin Architecture**: Dynamically loaded on demand via Android `DexClassLoader` with zero background battery or memory overhead when idle.
- **📱 Multi-Architecture Support**: Pre-configured ABI splits (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) for minimal download and install sizes.

---

## 🚀 Installation & Usage

### 1. Download & Install
Download the latest APK matching your device's CPU architecture (typically `arm64-v8a` for modern Android devices) from the [Releases](https://github.com/LeanBitLab/LeanType-OCR-Plugin/releases) section and install it.

### 2. Enable in LeanType Keyboard
1. Open **LeanType Settings**.
2. Navigate to **Advanced** $\to$ **Plugins**.
3. Select **OCR Plugin** and verify the status shows **Active / Connected**.
4. Use the OCR action from the keyboard toolbar or long-press action to scan and insert text anywhere.

---

## 🛠️ Building from Source

### Prerequisites
- Android Studio Ladybug or later / Android SDK Platform 34
- JDK 17
- Gradle 8.2+

### Build Commands

```bash
# Clone the repository
git clone https://github.com/LeanBitLab/LeanType-OCR-Plugin.git
cd LeanType-OCR-Plugin

# Build Debug APKs for all ABIs
./gradlew assembleDebug

# Build Release APKs (configured with ProGuard/R8 optimization)
./gradlew assembleRelease
```

Generated APKs will be located in:
```
app/build/outputs/apk/{debug,release}/ocr_plugin-{abi}.apk
```

---

## 📐 Architecture & Plugin Interface

LeanType loads this plugin dynamically at runtime through a standardized interface contract:

```kotlin
package helium314.keyboard.latin.ocr

interface ITextRecognizer {
    suspend fun recognizeText(bitmap: Bitmap): String
}
```

Implementation is managed by `TextRecognizerImpl` in `helium314.keyboard.ocr.plugin`, which initializes the bundled `TextRecognition` clients on demand and processes image frames asynchronously using Kotlin Coroutines.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)** - see the [LICENSE](LICENSE) file for details.
