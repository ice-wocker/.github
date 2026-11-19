# iceTerm

> High-performance terminal workstation & Android APK for Android (Termux) & Linux.
> Zero third-party dependencies, single-file core, offline-first, native Chinese localization, and premier coding model routing.

[![Android CI & Release](https://github.com/ice-wocker/iceTerm/actions/workflows/android-release.yml/badge.svg)](https://github.com/ice-wocker/iceTerm/actions)
[![Python 3.8+](https://img.shields.io/badge/Python-3.8%2B-blue.svg)](https://www.python.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-0-success.svg)](#)

---

## Overview

iceTerm is engineered for mobile developers and Unix power users requiring a high-density, low-latency, and dependency-free terminal environment. Version 1.1.0 delivers a comprehensive overhaul: full native Chinese localization, multi-tab session management, an ANSI 16/256-color rendering engine, a 150+ command developer toolbox, hardware/kernel telemetry, socket inode resolution, and multi-model AI coding copilot integration.

### Core Problems Addressed

1. **Terminal Lag & Buffer Bloat**: Standard mobile terminal wrappers suffer from unbounded TextView memory growth and ANSI escape code corruption. iceTerm incorporates a dedicated ANSI parser and a circular ring buffer.
2. **Native Chinese Localization**: Full Chinese UI, localized command explanations, Chinese heuristic AI intent matching, and UTF-8 environment initialization (`LANG=zh_CN.UTF-8`).
3. **Android Phantom Process Killer**: Android 12+ strictly limits child processes to 32, killing background compilation and node/python tasks. iceTerm features real-time phantom process detection and one-click ADB/Shizuku remediation.
4. **Mobile Input Obstacles**: Touchscreens lack developer modifiers. iceTerm provides a dedicated virtual keypad with `^C`, `^D`, `TAB`, `ESC`, `|`, `~`, `UP`, and `DOWN`.
5. **AI Coding Model Routing**: Intelligent routing configured for state-of-the-art coding models (DeepSeek-Coder, Qwen2.5-Coder 32B, GLM-4, Moonshot, Ollama) alongside 50+ offline Chinese heuristic rules.

---

## Feature Matrix

| Subsystem | Architecture / Implementation | Capability |
| :--- | :--- | :--- |
| **Localization** | `res/values-zh-rCN/strings.xml` | Native Simplified Chinese interface, dialogues, command guides, and alerts |
| **Session Multiplexer** | `SessionManager.java` & POSIX PTY | Concurrent multi-tab terminal management with dynamic add, switch, and close |
| **Terminal Buffer** | `TerminalBuffer.java` (Ring Buffer) | High-performance circular scrollback preventing UI memory exhaustion |
| **ANSI Engine** | `AnsiParser.java` (16 & 256 Colors) | ANSI SGR color, background, bold, underline, and RGB rendering |
| **Command Toolbox** | `CommandToolbox.java` (150+ Commands) | Categorized Linux & Android recipes with Chinese search and instant execution |
| **System Telemetry** | `SystemMonitor.java` | Real-time monitoring of CPU, RAM, thermal sensors, battery, and Phantom Process limits |
| **Port Radar** | `PortRadar.java` (`/proc/net/tcp`) | Active socket discovery mapping inode numbers to PIDs, process names, and LAN endpoints |
| **AI Copilot** | `AICopilot.java` (Dual-Engine) | Multi-model routing (DeepSeek, Qwen, GLM, Moonshot, Ollama) + 50+ offline Chinese regex patterns |
| **Security Guard** | `ShellSession.java` | Proactive interception of destructive commands (`rm -rf /`, raw disk overwrite, fork bombs) |
| **Themes & Display** | SharedPreferences & Palette Engine | Multiple themes (Matrix Green, Cyber Cyan, Solarized Dark, Monochrome) + font scaling |
| **Android APK** | Zero-Gradle Pure SDK Toolchain | Single-dex APK under 100KB compiled directly via `aapt2`, `d8`, `zipalign`, and `apksigner` |

---

## Command Toolbox Categories (150+ Presets)

The built-in Command Toolbox organizes essential developer workflows with Chinese explanations:

1. **系统信息与监控 (System & Telemetry)**: CPU core details, load averages, memory distribution, thermal sensor readings, disk partition utilization, and kernel dmesg inspection.
2. **网络与端口诊断 (Networking & Ports)**: Socket discovery, IP routing, ping latency, DNS resolution, cURL header tests, traceroute, and packet diagnostics.
3. **进程与后台管理 (Process & Task Control)**: Process trees (`pstree`), resource consumers (`top`/`ps`), signal delivery (`kill`/`killall`), background job management, and Android Phantom Process Killer verification.
4. **文件检索与管理 (Filesystem & Storage)**: Fast pattern search (`find`), regex line search (`grep`), disk space hogs (`du -sh *`), file hashing (`sha256sum`), and permission manipulation (`chmod`/`chown`).
5. **Git 与版本控制 (Git Operations)**: Commit graph log, stash management, branch switching, dirty tree diff, tag management, and clean submodule synchronization.
6. **开发者环境 (Developer Toolchains)**: Node.js, Python, Go, Rust, and C/C++ environment checks, package installations, and dependency diagnostics.
7. **Android 与 Termux 专用 (Android & Termux Integration)**: Package manager (`pm list packages`), Termux API battery/clipboard/vibrate, Android properties (`getprop`), and Shizuku bridge triggers.

---

## AI Copilot Coding Model Routing

The AI Copilot supports direct zero-latency offline heuristic parsing as well as online routing to premier code intelligence models:

### Supported Providers & Models

- **DeepSeek**: `deepseek-coder`, `deepseek-chat`
- **Alibaba Cloud / Qwen**: `qwen2.5-coder-32b-instruct`, `qwen2.5-coder-7b-instruct`
- **Zhipu AI (GLM)**: `glm-4-plus`, `glm-4`
- **Moonshot AI (Kimi)**: `moonshot-v1-8k`, `moonshot-v1-32k`
- **Ollama / Local LLM**: Direct LAN connection to locally hosted models (e.g., `http://192.168.1.100:11434/v1`)

### Offline Chinese Natural Language Heuristic Engine

Without network connectivity or API tokens, the offline engine matches Chinese natural language queries to shell commands:

| Natural Language Query | Generated Shell Command |
| :--- | :--- |
| `查找大于100M的文件` | `find . -type f -size +100M -exec ls -lh {} +` |
| `释放端口 8080` / `杀死端口 8080` | `fuser -k 8080/tcp || kill -9 $(lsof -t -i:8080)` |
| `解除安卓进程限制` | `device_config put activity_manager max_phantom_processes 2147483647` |
| `查找包含 error 的日志` | `grep -rnI --color=auto "error" .` |
| `解压 backup.tar.gz` | `tar -zxvf backup.tar.gz` |
| `查看内存占用最高进程` | `ps aux --sort=-%mem | head -n 10` |

---

## Android APK Architecture

```
android/
├── AndroidManifest.xml
├── build.sh                 # Zero-Gradle build: aapt2 -> javac -> d8 -> zipalign -> apksigner
├── res/
│   ├── drawable/ic_launcher.xml
│   ├── layout/activity_main.xml
│   ├── values/
│   │   ├── colors.xml       # Terminal palettes & tab colors
│   │   ├── strings.xml      # Default English strings
│   │   └── styles.xml       # Dark developer theme
│   └── values-zh-rCN/
│       └── strings.xml      # Native Simplified Chinese strings
└── src/org/icewocker/iceterm/
    ├── MainActivity.java    # Multi-tab UI, toolbar, dialogs, virtual keys, preferences
    ├── SessionManager.java  # Multi-session lifecycle and output routing
    ├── ShellSession.java    # Interactive shell process, UTF-8 locale, safety guard
    ├── TerminalBuffer.java  # High-efficiency circular ring scrollback buffer
    ├── AnsiParser.java      # ANSI 16/256-color & formatting parser to Spannable
    ├── CommandToolbox.java  # 150+ categorized developer commands with Chinese guides
    ├── SystemMonitor.java   # Telemetry for CPU, RAM, thermal sensors, Phantom Process
    ├── PortRadar.java       # Local socket discovery (/proc/net/tcp) with inode-to-PID
    └── AICopilot.java       # Premier coding model routing & offline heuristic engine
```

---

## Building and Releasing

### Zero-Gradle Pure SDK Build

Builds in seconds without Gradle or external build systems:

```bash
cd android
./build.sh
```

### GitHub Actions CI/CD Pipeline

The `.github/workflows/android-release.yml` workflow:
1. Executes unit tests with zero third-party dependencies.
2. Compiles `iceterm.apk` on `ubuntu-latest` using JDK 17 and Android build-tools 35.
3. Signs and verifies the APK using `apksigner`.
4. Publishes `iceterm.apk` as a release asset whenever a version tag (`v*`) is pushed.

---

## License

MIT License. Copyright (c) 2026 ice-wocker.
