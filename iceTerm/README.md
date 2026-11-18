# 🧊 iceTerm (冰端)

> **极客与移动端极其强大的终端增强工作站 · 零第三方依赖 · 单文件纯 Python · 离线优先**
> 
> *A supercharged, dependency-free terminal workstation for Android (Termux) & Linux — multiplexer, AI copilot, port radar, phantom process guard, and built-in mobile web terminal.*

[![CI](https://github.com/ice-wocker/iceTerm/actions/workflows/ci.yml/badge.svg)](https://github.com/ice-wocker/iceTerm/actions)
[![Python 3.8+](https://img.shields.io/badge/Python-3.8%2B-blue.svg)](https://www.python.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![Zero Dependencies](https://img.shields.io/badge/dependencies-0-success.svg)](#)

---

## 💡 为什么需要 iceTerm？

在手机端（Termux）或轻量 Linux 环境下进行日常开发和运维时，开发者经常遇到几个核心痛点：

1. **依赖过重**：想用会话分屏得装 `tmux`，想用 AI 辅助得跑复杂的 node/npm 工具链，频繁遇到 glibc/bionic 编译与依赖地狱。
2. **Android 系统严苛杀后台**：Android 12+ 引入了**幽灵进程杀手 (Phantom Process Killer)**，子进程超过 32 个就会被 SIGKILL 暴毙，编译大型项目或跑服务经常莫名中断。
3. **缺少端口与网络感知**：在手机上启动了本地 Web 服务（如 3000、8000 端口），不知道局域网 IP 是多少、局域网设备无法方便联调。
4. **小屏手机输入极难**：手机虚拟键盘缺少 `Ctrl`、`Alt`、`ESC`、`TAB`、管道符 `|`、反引号等高频按键。
5. **排错成本高**：移动端打字麻烦，遇到命令报错时缺乏一键分析与修复指导。

**iceTerm 从零用纯 Python 标准库手写打造**，体积不到几十 KB，**零第三方依赖**，不依赖编译链，装上就能用，把全套终端增强能力塞进移动端！

---

## 🌟 核心特性一览

| 模块 | 功能说明 | 传统方案对比 |
| :--- | :--- | :--- |
| **轻量虚拟终端多路复用** | 单文件纯 PTY 实现多标签页、无缝切换（`Ctrl+A 1..9`、`Ctrl+A c`） | 无需安装体积庞大的 `tmux`，毫秒级秒开 |
| **端侧 AI 终端副驾驶** | 双引擎：0 延迟离线规则引擎 + OpenAI/端侧小模型 (iceLLM/llama.cpp/Ollama) 协议 | 断网也能自然语言生成 Shell 命令 |
| **智能错误诊断与一键修复** | 命令非零退出时自动截取错误堆栈，给出精准原因与修复命令 | 免去手动 Google 搜报错的时间 |
| **危险命令安全防护网** | 拦截 `rm -rf /`、`mkfs`、`dd` 覆写物理磁盘、Fork 炸弹等致命高危命令 | 防止在手机触摸屏上误触回车误删整机系统 |
| **Android 幽灵进程守卫** | 内置 Android 12+ Phantom Process Killer 检测与一键 Shizuku/ADB 解除方案 | 根除单应用超过 32 进程被系统直接杀后台问题 |
| **网络与端口感知雷达** | 纯 Python 解析 `/proc/net/tcp`，自动列出活动端口并生成局域网预览链接 | 免装 `netstat` / `ss` / `lsof` |
| **移动端友好 Web 终端** | 内置微型 Web 服务器，手机或电脑浏览器直接打开，带专用虚拟扩展键盘 | 跨设备无缝编写与调试 |

---

## 🚀 极速上手

### 方式一：一行命令安装 / 运行 (推荐)

在 **Termux (Android)** 或任意 **Linux / macOS** 终端中运行：

```bash
curl -fsSL https://raw.githubusercontent.com/ice-wocker/iceTerm/main/install.sh | bash
```

或者直接下载单文件执行（无任何依赖）：

```bash
curl -fsSL -o iceterm https://raw.githubusercontent.com/ice-wocker/iceTerm/main/ice_term.py
chmod +x iceterm
./iceterm
```

---

## ⌨️ 快捷键速查 (交互式多标签模式)

进入 `iceterm` 交互模式后，默认通过 `Ctrl+A`（类似 screen / tmux 习惯）唤出快捷菜单：

- `Ctrl+A c` : 创建新标签页 (New Tab)
- `Ctrl+A 1..9` : 快速跳转到第 1~9 个标签页
- `Ctrl+A n` / `p` : 下一个 / 上一个标签页
- `Ctrl+A x` : 关闭当前活跃标签页
- `Ctrl+A a` : 呼出 **AI Copilot**，直接输入自然语言生成命令
- `Ctrl+A s` : 呼出端口雷达与系统指标面板
- `Ctrl+A Ctrl+A` : 发送原生 `Ctrl+A` 字符给终端程序
- `Ctrl+A d` 或 `q` : 退出 iceTerm 会话

---

## 🛠️ 常用命令行工具箱

除了交互式终端，`iceterm` 还提供强大的独立 CLI 工具：

### 1. 自然语言转命令 (`iceterm ai`)

```bash
# 本地离线规则秒级匹配（不消耗流量、不依赖外网）
iceterm ai "查找当前目录下大于100MB的文件"
# 推荐命令: find . -type f -size +100M -exec ls -lh {} +

iceterm ai "查找占用端口8080的进程"
# 推荐命令: lsof -i :8080 || fuser 8080/tcp

iceterm ai "解压 backup.tar.gz"
# 推荐命令: tar -zxvf backup.tar.gz
```

> **配置自定义大模型**（可选）：
> 支持接入本地 `iceLLM`、`llama.cpp`、`Ollama` 或云端模型：
> ```bash
> export OPENAI_BASE_URL="http://127.0.0.1:8080/v1"
> export OPENAI_API_KEY="sk-xxx"
> export OPENAI_MODEL="qwen2.5-coder"
> ```

### 2. 智能错误诊断 (`iceterm explain`)

```bash
# 方式 A: 执行命令并在失败时自动分析
iceterm run npm run dev

# 方式 B: 诊断已有错误日志
cat build.log | iceterm explain
```

输出示例：
```
❌ 诊断报告: 端口冲突: 端口 3000 已被占用
原因: 有其他后台进程正在监听该网络端口。
💡 建议修复命令:
  fuser -k 3000/tcp  # 一键释放该端口
```

### 3. 端口与网络雷达 (`iceterm radar`)

```bash
iceterm radar
```

```
📡 [iceTerm 端口与网络雷达]
本机网络接口 IP: 127.0.0.1, 192.168.1.108

协议   本地监听地址         端口     外部访问预览
────────────────────────────────────────────────────────────
TCP    0.0.0.0:3000         3000     http://192.168.1.108:3000
TCP    127.0.0.1:8080       8080     http://127.0.0.1:8080 (仅本机)
```

### 4. 启动内置 Web 终端 (`iceterm serve`)

```bash
iceterm serve --port 7681
```
- 在同局域网的电脑、平板或另一台手机上访问 `http://<手机IP>:7681`。
- 手机端专属响应式 UI，自带 `Ctrl+C`、`TAB`、`ESC`、`|`、`~` 等常用扩展软按键。

### 5. 解除 Android 12+ 幽灵进程杀手限制 (`iceterm fix-phantom`)

```bash
iceterm fix-phantom
```
输出在 Shizuku（免 Root）或 ADB 下的执行指令，一键解除 32 进程上限。

---

## 📐 架构设计

```
┌──────────────────────────────────────────────────────────┐
│                   iceTerm Architecture                   │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  [CLI / WebUI / Interactive MUX]                         │
│   ├── PTY Allocation (pty.openpty, TIOCSWINSZ)           │
│   ├── Event Loop (select, tty raw, termios)              │
│   └── Tab Manager (Multiple concurrent processes)       │
│                                                          │
│  [AI Copilot Layer]                                      │
│   ├── Offline Heuristic Rule Engine (Zero-Latency)       │
│   └── OpenAI-Compatible Protocol Client (urllib.request) │
│                                                          │
│  [Safety & Diagnostic Layer]                             │
│   ├── SafetyGuard (Dangerous command sandbox)            │
│   └── ErrorAnalyzer (Automated stack trace analysis)     │
│                                                          │
│  [System Bridge Layer]                                   │
│   ├── PortRadar (/proc/net/tcp parser)                   │
│   ├── Battery & Thermal Monitor (/sys/class/...)         │
│   └── Android Shizuku / ADB Bridge                       │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

---

## 🧪 自动化测试

所有测试均使用 Python 标准库 `unittest` 编写，不依赖任何第三方单测库：

```bash
python3 -m unittest discover -s tests
```

---

## 📜 许可证

本项目采用 [MIT License](LICENSE) 开源。
