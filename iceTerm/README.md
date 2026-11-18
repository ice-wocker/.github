# iceTerm

> High-performance terminal workstation for Android (Termux) & Linux.
> Zero third-party dependencies, single-file core, offline-first, native Android APK build.

[![Android CI & Release](https://github.com/ice-wocker/iceTerm/actions/workflows/android-release.yml/badge.svg)](https://github.com/ice-wocker/iceTerm/actions)
[![Python 3.8+](https://img.shields.io/badge/Python-3.8%2B-blue.svg)](https://www.python.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-0-success.svg)](#)

---

## Overview

iceTerm is designed to address fundamental limitations in mobile and lightweight Linux development environments:

1. **Dependency bloat**: Modern developer tools require extensive Node/Python runtimes and complex toolchains. iceTerm implements PTY multiplexing, port scanning, and AI routing using only the standard library.
2. **Android process restrictions**: Android 12+ enforces a 32-child-process limit (Phantom Process Killer), causing background compilation tasks to be terminated unexpectedly.
3. **Network opacity**: Services started locally on mobile lack visibility into LAN IP allocation and port bindings.
4. **Mobile input friction**: Touch keyboards lack dedicated modifiers (`Ctrl`, `Alt`, `ESC`, `TAB`, `|`, `~`).
5. **AI Routing for Code**: Direct integration and intelligent routing to top coding models (DeepSeek-Coder, Qwen2.5-Coder, Claude 3.5 Sonnet) combined with a local zero-latency heuristic engine.

---

## Feature Matrix

| Subsystem | Implementation | Capability |
| :--- | :--- | :--- |
| **PTY Multiplexer** | Pure POSIX PTY (`pty`, `termios`, `select`) | Multi-tab session management without tmux dependency |
| **AI Copilot** | Dual-engine: Offline heuristic + OpenAI protocol | Intelligent routing to premier coding models (`deepseek-coder`, `qwen2.5-coder`) |
| **Security Interceptor** | Pattern-based AST matching | Blocks destructive commands (`rm -rf /`, raw disk overwrite, fork bombs) |
| **Error Diagnostic** | Exit code and stack trace analysis | Automated diagnosis with actionable fix commands |
| **Port Radar** | Pure `/proc/net/tcp` parser | Real-time socket discovery and LAN preview URLs without `netstat`/`lsof` |
| **Phantom Process Guard**| Android system setting bridge | Instructions and script for disabling 32-process limit via Shizuku/ADB |
| **Web Terminal** | Built-in HTTP server | Touch-optimized remote web terminal with virtual modifier keys |
| **Android APK** | Zero-Gradle native build pipeline | Single-dex APK under 100KB compiled directly via Android SDK build-tools |

---

## Installation & Quick Start

### 1. Terminal CLI / Workstation (Android Termux & Linux)

One-line installation:
```bash
curl -fsSL https://raw.githubusercontent.com/ice-wocker/iceTerm/main/install.sh | bash
```

Or run directly with Python:
```bash
python3 ice_term.py
```

### 2. Android APK Package

The project includes a standalone Android application under `android/`:
- **Pre-built release**: Available under GitHub Releases (`iceterm.apk`).
- **CI Automated Build**: Compiles directly via GitHub Actions with zero Gradle overhead.
- **Local compilation**:
  ```bash
  cd android
  ./build.sh
  ```

---

## Key Bindings (Interactive Multiplexer)

Default prefix is `Ctrl+A`:

- `Ctrl+A c` : Create new tab
- `Ctrl+A 1..9` : Switch to tab N
- `Ctrl+A n` / `p` : Next / Previous tab
- `Ctrl+A x` : Close active tab
- `Ctrl+A a` : AI Copilot prompt
- `Ctrl+A s` : Port radar and system telemetry
- `Ctrl+A Ctrl+A` : Send literal `Ctrl+A` to shell
- `Ctrl+A d` : Terminate session

---

## CLI Reference

### AI Copilot (`iceterm ai`)
Queries are routed to high-parameter coding models or matched instantly by the local offline engine:

```bash
# Instant offline heuristic translation
iceterm ai "find files larger than 100MB"
# Output: find . -type f -size +100M -exec ls -lh {} +

iceterm ai "kill process on port 8080"
# Output: fuser -k 8080/tcp
```

Configuring remote/local coding endpoints:
```bash
export OPENAI_BASE_URL="https://api.deepseek.com/v1"
export OPENAI_API_KEY="your-api-key"
export OPENAI_MODEL="deepseek-coder"
```

### Automated Error Diagnosis (`iceterm explain`)
```bash
# Run command with proactive failure analysis
iceterm run make build

# Or pipe output logs
cat error.log | iceterm explain
```

### Port & Interface Radar (`iceterm radar`)
```bash
iceterm radar
```
Scans `/proc/net/tcp` and maps active listeners to external access URLs.

### Mobile Web Terminal (`iceterm serve`)
```bash
iceterm serve --port 7681
```
Spins up a lightweight browser-based terminal with virtual soft-keys for mobile devices.

### Phantom Process Guard (`iceterm fix-phantom`)
```bash
iceterm fix-phantom
```
Generates Shizuku and ADB commands to remove Android 12+ process throttling.

---

## Android APK Architecture

```
android/
├── AndroidManifest.xml
├── build.sh                 # Zero-Gradle build: aapt2 -> javac -> d8 -> zipalign -> apksigner
├── res/
│   ├── drawable/ic_launcher.xml
│   ├── layout/activity_main.xml
│   └── values/{colors,strings,styles}.xml
└── src/org/icewocker/iceterm/
    ├── MainActivity.java    # Terminal UI, extra keys bar, history
    ├── ShellSession.java    # Interactive shell process management & safety filter
    ├── AICopilot.java       # Router for coding models & offline regex engine
    └── PortRadar.java       # Local socket parser (/proc/net/tcp)
```

---

## Continuous Integration & Release

GitHub Actions pipeline (`.github/workflows/android-release.yml`):
- Runs core unit tests without external packages.
- Compiles `iceterm.apk` on `ubuntu-latest` via SDK tools.
- Signs and verifies APK with `apksigner`.
- Automatically publishes release assets when a version tag (`v*`) is pushed.

---

## License

MIT License. Copyright (c) 2026 ice-wocker.
