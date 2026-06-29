# AutoTrans Android

> Real-time screen translator for Android. Free, open-source, privacy-first.

[![Android CI](https://github.com/autotrans/autotrans-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/autotrans/autotrans-android/actions)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-green.svg)]()

## What is AutoTrans?

AutoTrans captures your screen, extracts text with OCR, translates it, and displays the result in a floating overlay — all on-device, offline by default.

**Works with**: games, apps, PDFs, videos — anything on your screen.

## Features

- 📸 **Real-time screen capture** via MediaProjection
- 🔤 **On-device OCR** using ML Kit Text Recognition
- 🌐 **On-device translation** using ML Kit Translation (offline)
- 🪟 **Floating overlay** rendered on top of all apps
- 🔌 **Pluggable engines** — swap OCR or translation engine without code changes
- 🔒 **Privacy-first** — no accounts, no telemetry, all processing on device

## Supported Engines

### OCR
| Engine | Status | Notes |
|--------|--------|-------|
| ML Kit Text Recognition | ✅ Default | On-device, free |
| Tesseract | 🔜 Planned v1.5 | Community contribution welcome |

### Translation
| Engine | Status | Notes |
|--------|--------|-------|
| ML Kit Translation | ✅ Default | On-device, free, requires model download |
| Google Cloud Translate | 🔜 Planned v1.0 | Requires API key |
| DeepL | 🔜 Planned v2.0 | Requires API key |
| LibreTranslate | 🔜 Planned v2.0 | Self-hosted |

## Requirements

- Android 8.0+ (API 26)
- ~150 MB storage (ML Kit language models)

## Quick Start

1. Download the latest APK from [Releases](../../releases)
2. Grant **"Display over other apps"** permission
3. Tap **"Start Overlay"**
4. Grant screen capture permission when prompted
5. Translation appears on screen automatically

## Building from Source

```bash
git clone https://github.com/autotrans/autotrans-android.git
cd autotrans-android
./gradlew assembleDebug
```

See [CONTRIBUTOR_GUIDE.md](docs/CONTRIBUTOR_GUIDE.md) for full setup instructions.

## Architecture

Clean Architecture · MVVM · Multi-module · Hilt · Coroutines + Flow · Jetpack Compose

See [ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) for the full design.

## Documentation

| Document | Description |
|----------|-------------|
| [ARCHITECTURE.md](docs/architecture/ARCHITECTURE.md) | System architecture and module design |
| [PIPELINE.md](docs/architecture/PIPELINE.md) | Translation pipeline internals |
| [CONTRIBUTING](docs/CONTRIBUTOR_GUIDE.md) | How to contribute |
| [ROADMAP.md](docs/ROADMAP.md) | Feature roadmap |
| [CHANGELOG.md](CHANGELOG.md) | Version history |

## Contributing

PRs are welcome! See [CONTRIBUTOR_GUIDE.md](docs/CONTRIBUTOR_GUIDE.md) to get started.

Specifically looking for help with:
- 🔌 New OCR engines (Tesseract, PaddleOCR)
- 🌐 New translation engines (DeepL, LibreTranslate)
- 🧪 Test coverage improvements
- 🌍 String translations for the app UI

## License

```
Copyright 2026 AutoTrans Contributors

Licensed under the Apache License, Version 2.0
See LICENSE for full text.
```
