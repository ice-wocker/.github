#!/usr/bin/env python3
"""
iceTerm: Dependency-free, highly-optimized terminal workstation for Android (Termux) & Linux.

Architecture:
- PTY Multiplexer: Multi-tab session manager with hotkeys (Ctrl+A)
- AI Copilot Native: Routes queries to premier coding models (DeepSeek-Coder, Qwen-Coder)
- Offline Heuristic Engine: Zero-latency offline fallback for shell command generation
- Android / Termux Integration: Port radar, battery/thermal telemetry, Phantom Process guard
- Zero-Dependency Web Terminal: Built-in responsive web terminal server
- Security Sandbox: Proactive interceptor for destructive commands
"""

import os
import sys
import pty
import tty
import termios
import select
import subprocess
import signal
import json
import time
import shutil
import re
import socket
import threading
import urllib.request
import urllib.error
import http.server
import argparse
from typing import Dict, List, Tuple, Optional, Any

VERSION = "1.0.0"

# ==============================================================================
# 1. ANSI & Terminal UI Formatting
# ==============================================================================

class ANSI:
    RESET = "\033[0m"
    BOLD = "\033[1m"
    DIM = "\033[2m"
    ITALIC = "\033[3m"
    UNDERLINE = "\033[4m"

    # Colors
    BLACK = "\033[30m"
    RED = "\033[31m"
    GREEN = "\033[32m"
    YELLOW = "\033[33m"
    BLUE = "\033[34m"
    MAGENTA = "\033[35m"
    CYAN = "\033[36m"
    WHITE = "\033[37m"

    # Bright Colors
    BRIGHT_RED = "\033[91m"
    BRIGHT_GREEN = "\033[92m"
    BRIGHT_YELLOW = "\033[93m"
    BRIGHT_BLUE = "\033[94m"
    BRIGHT_MAGENTA = "\033[95m"
    BRIGHT_CYAN = "\033[96m"
    BRIGHT_WHITE = "\033[97m"

    # Backgrounds
    BG_BLACK = "\033[40m"
    BG_RED = "\033[41m"
    BG_GREEN = "\033[42m"
    BG_YELLOW = "\033[43m"
    BG_BLUE = "\033[44m"
    BG_MAGENTA = "\033[45m"
    BG_CYAN = "\033[46m"
    BG_WHITE = "\033[47m"
    BG_DARK = "\033[48;5;236m"

    # Control
    CLEAR_LINE = "\033[2K"
    CLEAR_SCREEN = "\033[2J\033[H"
    SAVE_CURSOR = "\033[s"
    RESTORE_CURSOR = "\033[u"
    HIDE_CURSOR = "\033[?25l"
    SHOW_CURSOR = "\033[?25h"

    @classmethod
    def strip(cls, text: str) -> str:
        """Remove ANSI escape sequences."""
        return re.sub(r"\033\[[0-9;]*[a-zA-Z]", "", text)


# ==============================================================================
# 2. Safety Guard (Security Interceptor)
# ==============================================================================

class SafetyGuard:
    """Detects and blocks destructive or hazardous terminal commands."""

    DANGEROUS_PATTERNS = [
        (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|\s+--recursive\s+--force)\s+/\s*$", "Recursive forced removal of root directory (rm -rf /)"),
        (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|\s+--recursive\s+--force)\s+/\*$", "Recursive forced removal of all root items (rm -rf /*)"),
        (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|\s+--recursive\s+--force)\s+~\s*$", "Recursive forced removal of home directory (rm -rf ~)"),
        (r"\bmkfs(\.[a-zA-Z0-9]+)?\s+/dev/", "Raw filesystem format (mkfs)"),
        (r"\bdd\s+.*of=/dev/(sd[a-z]|nvme[0-9]n[0-9]|mmcblk[0-9])", "Direct block device overwrite (dd)"),
        (r":\(\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:", "Bash fork bomb resource exhaustion"),
        (r">\s*/dev/(sd[a-z]|nvme[0-9]|kmem|mem|port)", "Direct write redirection to raw block/kernel memory"),
        (r"\bchmod\s+(-[a-zA-Z]*R)?\s*777\s+/\s*$", "Global 777 permission on root directory"),
    ]

    WARNING_PATTERNS = [
        (r"\brm\s+-[a-zA-Z]*r", "Recursive file/directory removal"),
        (r"\bkillall\s+-9\b|\bpkill\s+-9\b", "Mass process kill with SIGKILL"),
        (r"\bchown\s+-[a-zA-Z]*R", "Recursive file ownership change"),
        (r"\bkill\s+-9\s+1\b", "Attempt to kill init / PID 1 process"),
    ]

    @classmethod
    def evaluate(cls, cmd: str) -> Tuple[str, Optional[str]]:
        clean_cmd = cmd.strip()
        for pattern, reason in cls.DANGEROUS_PATTERNS:
            if re.search(pattern, clean_cmd):
                return "DANGEROUS", reason

        for pattern, reason in cls.WARNING_PATTERNS:
            if re.search(pattern, clean_cmd):
                return "WARN", reason

        return "SAFE", None


# ==============================================================================
# 3. Error Analyzer & Fix Auto-Suggest
# ==============================================================================

class ErrorAnalyzer:
    """Analyzes failed command outputs and suggests actionable fixes."""

    @classmethod
    def analyze(cls, cmd: str, exit_code: int, output_tail: str) -> Dict[str, str]:
        result = {
            "title": "Command execution failed",
            "cause": f"Process exited with status code {exit_code}",
            "fix": "",
            "category": "general"
        }
        if exit_code == 0:
            return {"title": "Execution successful", "cause": "Normal return code 0", "fix": "", "category": "ok"}

        out = output_tail.lower()

        # 1. Command not found
        if "command not found" in out or "未找到命令" in out:
            missing_bin = cmd.strip().split()[0] if cmd.strip() else "unknown"
            result["title"] = f"Executable not found: {missing_bin}"
            is_termux = "com.termux" in os.environ.get("PREFIX", "") or "termux" in os.environ.get("PATH", "")
            if is_termux:
                result["cause"] = f"Package providing '{missing_bin}' is not installed in Termux."
                result["fix"] = f"pkg install {missing_bin}  # or: apt update && apt install {missing_bin}"
            else:
                result["cause"] = f"'{missing_bin}' is not installed or not in PATH."
                result["fix"] = f"sudo apt update && sudo apt install {missing_bin}  # Debian/Ubuntu\n# or: sudo pacman -S {missing_bin} # Arch"
            result["category"] = "missing_binary"
            return result

        # 2. Port already in use / Address already in use
        if "address already in use" in out or "port is already allocated" in out or "eaddrinuse" in out:
            port_match = re.search(r":(\d{2,5})\b", output_tail)
            port_hint = port_match.group(1) if port_match else "<PORT>"
            result["title"] = f"Port Conflict: Port {port_hint} is already in use"
            result["cause"] = "Another process is actively listening on this TCP port."
            result["fix"] = f"fuser -k {port_hint}/tcp  # Terminate occupying process\n# or: lsof -i :{port_hint} | grep LISTEN"
            result["category"] = "port_conflict"
            return result

        # 3. Permission denied
        if "permission denied" in out or "权限不够" in out:
            result["title"] = "Permission Denied"
            result["cause"] = "Current process lacks read/write/execution permission for the target resource."
            result["fix"] = f"chmod +x {cmd.split()[-1]}  # If binary/script execution permission\n# or run with elevated privileges (sudo/su)"
            result["category"] = "permission"
            return result

        # 4. Python ModuleNotFoundError
        if "modulenotfounderror: no module named" in out or "no module named" in out:
            mod_match = re.search(r"no module named ['\"]?([a-zA-Z0-9_\.]+)['\"]?", out)
            mod_name = mod_match.group(1) if mod_match else "package"
            root_mod = mod_name.split(".")[0]
            result["title"] = f"Missing Python module: {root_mod}"
            result["cause"] = f"Python runtime could not locate '{root_mod}' in sys.path."
            result["fix"] = f"pip install {root_mod}  # or in Termux: pkg install python-{root_mod}"
            result["category"] = "python_dependency"
            return result

        # 5. Missing shared library
        if "cannot open shared object file" in out or "error while loading shared libraries" in out:
            lib_match = re.search(r"loading shared libraries:\s+([a-zA-Z0-9_\-\.]+):", output_tail)
            lib_name = lib_match.group(1) if lib_match else "lib"
            result["title"] = f"Missing dynamic library: {lib_name}"
            result["cause"] = "The dynamic linker (ld.so) cannot locate the required shared object."
            result["fix"] = f"apt-file search {lib_name}  # Query package name\n# or: pkg install {lib_name.replace('lib', '').split('.')[0]}"
            result["category"] = "missing_library"
            return result

        # 6. Git merge / non-fast-forward conflict
        if "[rejected]" in out and ("fetch first" in out or "non-fast-forward" in out):
            result["title"] = "Git push rejected: Remote contains unintegrated changes"
            result["cause"] = "Remote branch has commits not present in the local branch."
            result["fix"] = "git pull --rebase origin $(git branch --show-current)\n# Resolve conflicts then retry git push"
            result["category"] = "git_conflict"
            return result

        return result


# ==============================================================================
# 4. AI Copilot (Coding-Optimized Router & Heuristic Engine)
# ==============================================================================

class AICopilot:
    """
    Dual-mode AI copilot:
    1. Fast offline rule engine for instant zero-latency command translation.
    2. Intelligent routing to premier coding models: DeepSeek-Coder, Qwen2.5-Coder, Claude 3.5 Sonnet.
    """

    # Top-tier coding models routing defaults
    DEFAULT_CODING_MODELS = ["deepseek-coder", "deepseek-chat", "qwen2.5-coder-32b"]

    OFFLINE_PATTERNS = [
        # Search & Files
        (r"(?:find|search|查找|搜索).*?(?:大于|>)\s*(\d+)\s*(m|mb|g|gb)?", lambda m: f"find . -type f -size +{m.group(1)}{'G' if (m.group(2) or '').lower().startswith('g') else 'M'} -exec ls -lh {{}} +"),
        (r"(?:find|search|查找|搜索).*?名(?:字|称|为|包含)*\s*([a-zA-Z0-9_\-\.\*]+)", lambda m: f"find . -name '*{m.group(1)}*'"),
        (r"统计.*行数|count lines", lambda _: "wc -l *"),
        (r"(?:查|看|check).*?(?:磁盘|剩余空间|disk)", lambda _: "df -h"),
        (r"(?:查|看|check).*?(?:目录大小|文件夹大小|dir size)", lambda _: "du -sh * | sort -hr | head -n 15"),

        # Ports & Network
        (r"(?:查|看|找|check|find).*?端口\s*(\d+)", lambda m: f"lsof -i :{m.group(1)} || fuser {m.group(1)}/tcp"),
        (r"杀(?:死|掉|kill).*?端口\s*(\d+)", lambda m: f"fuser -k {m.group(1)}/tcp"),
        (r"(?:查|看|check).*?(?:本机|内网)?ip", lambda _: "ip -br a || ifconfig"),
        (r"(?:查|看|check).*?监听.*?端口", lambda _: "ss -tulpn || netstat -tulpn"),

        # Processes & System
        (r"(?:查|看|check).*?(?:cpu|内存|memory).*?占用", lambda _: "ps aux --sort=-%cpu | head -n 15"),
        (r"杀死.*进程\s*([a-zA-Z0-9_\-]+)", lambda m: f"pkill -f '{m.group(1)}'"),

        # Archives
        (r"(?:解压|extract|unzip|tar)\s*([a-zA-Z0-9_\-\.]+\.tar\.gz)", lambda m: f"tar -zxvf {m.group(1)}"),
        (r"(?:解压|extract|unzip)\s*([a-zA-Z0-9_\-\.]+\.zip)", lambda m: f"unzip {m.group(1)}"),
        (r"(?:打包|compress)\s*([a-zA-Z0-9_\-]+)\s*(?:为|to)?\s*([a-zA-Z0-9_\-\.]+\.tar\.gz)", lambda m: f"tar -zcvf {m.group(2)} {m.group(1)}"),

        # Git
        (r"(?:查|看|check).*?(?:最近|提交|git log)", lambda _: "git log --oneline --graph --decorate -n 10"),
        (r"(?:放弃|discard).*?(?:所有|本地)?修改", lambda _: "git restore . && git clean -fd"),
        (r"(?:暂存|提交|commit).*?(?:全部|all)", lambda _: 'git add -A && git commit -m "update"'),

        # Android & Termux
        (r"(?:关闭|解除|disable).*?(?:幽灵进程|phantom)", lambda _: "settings put global settings_enable_monitor_phantom_procs false"),
        (r"(?:保持后台|休眠锁|wakelock)", lambda _: "termux-wake-lock"),
    ]

    def __init__(self, api_base: Optional[str] = None, api_key: str = "not-needed", model: Optional[str] = None):
        # Default route to DeepSeek coding endpoint or local endpoint
        default_base = os.environ.get("OPENAI_BASE_URL", "https://api.deepseek.com/v1")
        self.api_base = (api_base or default_base).rstrip("/")
        self.api_key = os.environ.get("OPENAI_API_KEY", api_key)
        self.model = os.environ.get("OPENAI_MODEL", model or "deepseek-coder")

    def query_offline(self, natural_prompt: str) -> Optional[str]:
        text = natural_prompt.strip()
        for pattern, handler in self.OFFLINE_PATTERNS:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                try:
                    return handler(match)
                except Exception:
                    continue
        return None

    def query_llm(self, natural_prompt: str, timeout: int = 10) -> Tuple[bool, str]:
        url = f"{self.api_base}/chat/completions"
        system_prompt = (
            "You are iceTerm Code Assistant. Convert user natural language query into the most accurate, secure, production-grade Shell command on Linux/Android. "
            "Return ONLY the executable shell command inside markdown code block ```bash ... ``` without conversation."
        )
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": natural_prompt}
            ],
            "temperature": 0.1,
            "max_tokens": 160
        }
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.api_key}",
                "User-Agent": "iceTerm-Engine/1.0"
            }
        )

        try:
            with urllib.request.urlopen(req, timeout=timeout) as response:
                body = response.read().decode("utf-8")
                res_json = json.loads(body)
                content = res_json["choices"][0]["message"]["content"].strip()
                code_match = re.search(r"```(?:bash|sh)?\s*([\s\S]*?)\s*```", content)
                cmd = code_match.group(1).strip() if code_match else content.strip()
                return True, cmd
        except Exception as e:
            return False, f"LLM routing failed ({self.model}): {e}"

    def ask(self, natural_prompt: str) -> Tuple[str, str]:
        # 1. Offline rule match (zero latency, zero network)
        offline_cmd = self.query_offline(natural_prompt)
        if offline_cmd:
            return offline_cmd, "[Offline-Engine (0-Latency)]"

        # 2. Routed coding model
        ok, res = self.query_llm(natural_prompt)
        if ok:
            return res, f"[Routed-Coding-Model: {self.model}]"

        # 3. Fallback
        return f"# Unrecognized intent: '{natural_prompt}'. Provide more detail or configure model endpoint.", "[Fallback]"


# ==============================================================================
# 5. Android / Termux Superpowers & Port Radar
# ==============================================================================

class TermuxBridge:
    """Android & Termux environment detection and system enhancements."""

    @classmethod
    def is_android(cls) -> bool:
        return os.path.exists("/system/build.prop") or "ANDROID_ROOT" in os.environ or "com.termux" in os.environ.get("PREFIX", "")

    @classmethod
    def get_system_metrics(cls) -> Dict[str, Any]:
        data = {
            "battery_pct": None,
            "battery_status": "Unknown",
            "battery_temp_c": None,
            "cpu_temp_c": None,
            "mem_total_mb": None,
            "mem_avail_mb": None,
            "is_android": cls.is_android(),
        }

        # Memory stats
        try:
            with open("/proc/meminfo", "r") as f:
                for line in f:
                    if line.startswith("MemTotal:"):
                        data["mem_total_mb"] = int(line.split()[1]) // 1024
                    elif line.startswith("MemAvailable:"):
                        data["mem_avail_mb"] = int(line.split()[1]) // 1024
        except Exception:
            pass

        # Android Battery stats
        batt_dir = "/sys/class/power_supply/battery"
        if os.path.exists(batt_dir):
            try:
                cap_file = os.path.join(batt_dir, "capacity")
                if os.path.exists(cap_file):
                    with open(cap_file, "r") as f:
                        data["battery_pct"] = int(f.read().strip())

                status_file = os.path.join(batt_dir, "status")
                if os.path.exists(status_file):
                    with open(status_file, "r") as f:
                        data["battery_status"] = f.read().strip()

                temp_file = os.path.join(batt_dir, "temp")
                if os.path.exists(temp_file):
                    with open(temp_file, "r") as f:
                        raw_t = int(f.read().strip())
                        data["battery_temp_c"] = raw_t / 10.0 if raw_t > 100 else raw_t
            except Exception:
                pass

        # CPU Thermal stats
        thermal_dir = "/sys/class/thermal"
        if os.path.exists(thermal_dir):
            try:
                for z in sorted(os.listdir(thermal_dir)):
                    if z.startswith("thermal_zone"):
                        t_path = os.path.join(thermal_dir, z, "temp")
                        if os.path.exists(t_path):
                            with open(t_path, "r") as f:
                                raw_t = int(f.read().strip())
                                val = raw_t / 1000.0 if raw_t > 1000 else raw_t
                                if 10 <= val <= 110:
                                    data["cpu_temp_c"] = round(val, 1)
                                    break
            except Exception:
                pass

        return data

    @classmethod
    def phantom_process_fix_script(cls) -> str:
        return (
            "# Bypass Android 12+ Phantom Process Killer (>32 child processes killed):\n"
            "# 1. With Shizuku (recommended, rootless):\n"
            "rish -c 'settings put global settings_enable_monitor_phantom_procs false'\n\n"
            "# 2. Or via computer ADB:\n"
            "adb shell settings put global settings_enable_monitor_phantom_procs false\n\n"
            "# 3. Verify status (returns 'false' when disabled):\n"
            "/system/bin/settings get global settings_enable_monitor_phantom_procs\n"
        )


class PortRadar:
    """Pure-Python scanning of /proc/net/tcp and /proc/net/tcp6 to identify listening ports."""

    @classmethod
    def get_lan_ips(cls) -> List[str]:
        ips = ["127.0.0.1"]
        try:
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.settimeout(0.1)
            s.connect(("8.8.8.8", 80))
            lan_ip = s.getsockname()[0]
            if lan_ip not in ips:
                ips.append(lan_ip)
            s.close()
        except Exception:
            pass
        return ips

    @classmethod
    def parse_hex_endpoint(cls, hex_str: str) -> Tuple[str, int]:
        ip_hex, port_hex = hex_str.split(":")
        port = int(port_hex, 16)
        if len(ip_hex) == 8:
            raw_bytes = bytes.fromhex(ip_hex)
            ip = ".".join(str(b) for b in reversed(raw_bytes))
        else:
            ip = "::"
        return ip, port

    @classmethod
    def scan_listening_ports(cls) -> List[Dict[str, Any]]:
        listeners = []
        seen = set()

        for proc_file in ["/proc/net/tcp", "/proc/net/tcp6"]:
            if not os.path.exists(proc_file):
                continue
            try:
                with open(proc_file, "r") as f:
                    lines = f.readlines()
                for line in lines[1:]:
                    parts = line.strip().split()
                    if len(parts) >= 4:
                        local_addr = parts[1]
                        state = parts[3]
                        if state.upper() == "0A":  # TCP_LISTEN
                            ip, port = cls.parse_hex_endpoint(local_addr)
                            key = (ip, port)
                            if key not in seen:
                                seen.add(key)
                                listeners.append({
                                    "ip": ip,
                                    "port": port,
                                    "is_local_only": ip in ["127.0.0.1", "::1", "0.0.0.0", "::"],
                                    "is_all_interfaces": ip in ["0.0.0.0", "::"]
                                })
            except Exception:
                pass

        listeners.sort(key=lambda x: x["port"])
        return listeners


# ==============================================================================
# 6. Zero-Dependency Web Terminal Server
# ==============================================================================

class WebTerminalServer:
    """Lightweight, responsive Web Terminal server running on pure Python standard library."""

    HTML_PAGE = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>iceTerm Web Terminal</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: #0d1117;
            color: #c9d1d9;
            font-family: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace;
            display: flex;
            flex-direction: column;
            height: 100vh;
            overflow: hidden;
        }
        header {
            background: #161b22;
            padding: 8px 14px;
            display: flex;
            align-items: center;
            justify-content: space-between;
            border-bottom: 1px solid #30363d;
            user-select: none;
        }
        .title { font-weight: bold; color: #58a6ff; font-size: 14px; display: flex; align-items: center; gap: 8px; }
        .badge { background: #238636; color: #fff; padding: 2px 6px; border-radius: 4px; font-size: 11px; }
        #terminal-container {
            flex: 1;
            padding: 10px;
            overflow-y: auto;
            white-space: pre-wrap;
            word-break: break-all;
            background: #090d13;
            font-size: 13px;
            line-height: 1.4;
        }
        .out-line { color: #8b949e; }
        .cmd-line { color: #58a6ff; font-weight: bold; }
        .err-line { color: #f85149; }
        #mobile-keyboard {
            background: #161b22;
            border-top: 1px solid #30363d;
            display: flex;
            overflow-x: auto;
            padding: 6px;
            gap: 6px;
        }
        .key-btn {
            background: #21262d;
            border: 1px solid #30363d;
            color: #c9d1d9;
            padding: 6px 12px;
            border-radius: 4px;
            font-size: 12px;
            font-family: inherit;
            cursor: pointer;
            white-space: nowrap;
        }
        .key-btn:active { background: #388bfd; color: #fff; }
        #input-bar {
            display: flex;
            padding: 8px;
            background: #161b22;
            border-top: 1px solid #30363d;
        }
        #cmd-input {
            flex: 1;
            background: #0d1117;
            border: 1px solid #30363d;
            border-radius: 4px;
            color: #fff;
            padding: 8px 12px;
            font-size: 14px;
            font-family: inherit;
            outline: none;
        }
        #cmd-input:focus { border-color: #58a6ff; }
        #send-btn {
            background: #238636;
            color: #fff;
            border: none;
            border-radius: 4px;
            padding: 8px 16px;
            margin-left: 8px;
            cursor: pointer;
            font-weight: bold;
        }
    </style>
</head>
<body>
    <header>
        <div class="title">
            <span>[iceTerm] Terminal Workstation</span>
            <span class="badge">ONLINE</span>
        </div>
        <div style="font-size: 12px; color: #8b949e;">Zero-Dependency · Pure Python</div>
    </header>

    <div id="terminal-container">
        <div class="out-line">[iceTerm] Remote Web Terminal active.</div>
        <div class="out-line">Type commands or use the quick access bar below.</div>
        <div class="out-line">----------------------------------------------------</div>
    </div>

    <div id="mobile-keyboard">
        <button class="key-btn" onclick="insertKey('Ctrl+C')">^C</button>
        <button class="key-btn" onclick="insertKey('TAB')">TAB</button>
        <button class="key-btn" onclick="insertKey('ESC')">ESC</button>
        <button class="key-btn" onclick="insertText('|')">|</button>
        <button class="key-btn" onclick="insertText('~')">~</button>
        <button class="key-btn" onclick="insertText('-')">-</button>
        <button class="key-btn" onclick="insertText('/')">/</button>
        <button class="key-btn" onclick="sendCmd('ls -la')">ls -la</button>
        <button class="key-btn" onclick="sendCmd('iceterm radar')">Radar</button>
        <button class="key-btn" onclick="sendCmd('iceterm status')">Status</button>
        <button class="key-btn" onclick="clearTerm()">Clear</button>
    </div>

    <div id="input-bar">
        <input type="text" id="cmd-input" placeholder="Enter shell command (e.g. uname -a)..." autofocus autocomplete="off">
        <button id="send-btn" onclick="handleSend()">RUN</button>
    </div>

    <script>
        const term = document.getElementById('terminal-container');
        const input = document.getElementById('cmd-input');

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') handleSend();
        });

        function insertText(str) {
            input.value += str;
            input.focus();
        }

        function insertKey(k) {
            if (k === 'Ctrl+C') sendCmd('^C');
            else if (k === 'TAB') insertText('  ');
            else if (k === 'ESC') input.value = '';
        }

        function clearTerm() {
            term.innerHTML = '<div class="out-line">Terminal cleared.</div>';
        }

        function appendLine(text, className) {
            const div = document.createElement('div');
            div.className = className;
            div.textContent = text;
            term.appendChild(div);
            term.scrollTop = term.scrollHeight;
        }

        function handleSend() {
            const cmd = input.value.trim();
            if (!cmd) return;
            input.value = '';
            sendCmd(cmd);
        }

        async function sendCmd(cmd) {
            appendLine('$ ' + cmd, 'cmd-line');
            try {
                const res = await fetch('/api/exec', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ command: cmd })
                });
                const data = await res.json();
                if (data.output) appendLine(data.output, 'out-line');
                if (data.exit_code !== 0 && data.exit_code !== undefined) {
                    appendLine(`[Exit status: ${data.exit_code}]`, 'err-line');
                }
            } catch (err) {
                appendLine('Execution error: ' + err, 'err-line');
            }
        }
    </script>
</body>
</html>
"""

    @classmethod
    def start(cls, host: str = "0.0.0.0", port: int = 7681):
        class Handler(http.server.BaseHTTPRequestHandler):
            def log_message(self, format, *args):
                pass

            def do_GET(self):
                if self.path == "/" or self.path == "/index.html":
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(WebTerminalServer.HTML_PAGE.encode("utf-8"))
                elif self.path == "/api/status":
                    self.send_response(200)
                    self.send_header("Content-Type", "application/json")
                    self.end_headers()
                    metrics = TermuxBridge.get_system_metrics()
                    self.wfile.write(json.dumps(metrics).encode("utf-8"))
                else:
                    self.send_response(404)
                    self.end_headers()

            def do_POST(self):
                if self.path == "/api/exec":
                    content_len = int(self.headers.get("Content-Length", 0))
                    body = self.rfile.read(content_len).decode("utf-8")
                    req = json.loads(body)
                    cmd = req.get("command", "")

                    risk, reason = SafetyGuard.evaluate(cmd)
                    if risk == "DANGEROUS":
                        res = {
                            "output": f"[SECURITY-ALERT] Destructive command rejected: {reason}",
                            "exit_code": 126
                        }
                    else:
                        try:
                            proc = subprocess.run(
                                cmd,
                                shell=True,
                                stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT,
                                text=True,
                                timeout=15
                            )
                            res = {
                                "output": proc.stdout or "(no output)",
                                "exit_code": proc.returncode
                            }
                        except subprocess.TimeoutExpired:
                            res = {"output": "[TIMEOUT] Process exceeded 15s limit", "exit_code": 124}
                        except Exception as e:
                            res = {"output": f"Execution error: {e}", "exit_code": 1}

                    self.send_response(200)
                    self.send_header("Content-Type", "application/json; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(json.dumps(res).encode("utf-8"))

        server = http.server.ThreadingHTTPServer((host, port), Handler)
        print(f"{ANSI.GREEN}[OK] iceTerm Web Terminal listening on port {port}{ANSI.RESET}")
        for ip in PortRadar.get_lan_ips():
            print(f"  Endpoint: {ANSI.CYAN}http://{ip}:{port}{ANSI.RESET}")
        print(f"Press Ctrl+C to terminate server.\n")
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down Web Terminal...")
            server.server_close()


# ==============================================================================
# 7. Core Terminal Multiplexer & PTY Engine
# ==============================================================================

class PTYTab:
    """Represents one terminal tab / PTY child session."""

    def __init__(self, tab_id: int, title: str = "shell", shell: Optional[str] = None):
        self.tab_id = tab_id
        self.title = title
        self.shell = shell or os.environ.get("SHELL", "/bin/sh")
        self.master_fd: Optional[int] = None
        self.slave_fd: Optional[int] = None
        self.pid: Optional[int] = None
        self.is_alive = False

    def spawn(self, rows: int = 24, cols: int = 80):
        master, slave = pty.openpty()
        self.master_fd = master
        self.slave_fd = slave

        try:
            import struct, fcntl
            winsize = struct.pack("HHHH", rows, cols, 0, 0)
            fcntl.ioctl(slave, termios.TIOCSWINSZ, winsize)
        except Exception:
            pass

        pid = os.fork()
        if pid == 0:
            os.close(master)
            os.setsid()
            import fcntl
            try:
                fcntl.ioctl(slave, termios.TIOCSCTTY, 0)
            except Exception:
                pass
            os.dup2(slave, 0)
            os.dup2(slave, 1)
            os.dup2(slave, 2)
            if slave > 2:
                os.close(slave)

            os.environ["TERM"] = "xterm-256color"
            os.environ["ICETERM_ACTIVE"] = "1"
            os.environ["ICETERM_TAB"] = str(self.tab_id)

            try:
                os.execvp(self.shell, [self.shell])
            except Exception as e:
                sys.stderr.write(f"Failed to exec {self.shell}: {e}\n")
                sys.exit(1)
        else:
            os.close(slave)
            self.slave_fd = None
            self.pid = pid
            self.is_alive = True

    def close(self):
        if self.pid and self.is_alive:
            try:
                os.kill(self.pid, signal.SIGTERM)
                time.sleep(0.05)
                os.kill(self.pid, signal.SIGKILL)
            except Exception:
                pass
            try:
                os.waitpid(self.pid, os.WNOHANG)
            except Exception:
                pass
            self.is_alive = False

        if self.master_fd is not None:
            try:
                os.close(self.master_fd)
            except Exception:
                pass
            self.master_fd = None


class Multiplexer:
    """Zero-dependency interactive terminal multiplexer."""

    def __init__(self):
        self.tabs: List[PTYTab] = []
        self.active_idx = 0
        self.running = False
        self.orig_termios = None
        self.in_prefix = False
        self.copilot = AICopilot()

    def get_active_tab(self) -> Optional[PTYTab]:
        if 0 <= self.active_idx < len(self.tabs):
            return self.tabs[self.active_idx]
        return None

    def add_tab(self, title: str = "shell") -> PTYTab:
        cols, rows = shutil.get_terminal_size()
        tab_id = len(self.tabs) + 1
        tab = PTYTab(tab_id=tab_id, title=title)
        tab.spawn(rows=max(rows - 1, 10), cols=cols)
        self.tabs.append(tab)
        self.active_idx = len(self.tabs) - 1
        return tab

    def close_current_tab(self):
        tab = self.get_active_tab()
        if not tab:
            return
        tab.close()
        self.tabs.pop(self.active_idx)
        if self.active_idx >= len(self.tabs):
            self.active_idx = max(0, len(self.tabs) - 1)

    def draw_status_bar(self):
        cols, rows = shutil.get_terminal_size()
        sys.stdout.write(f"\033[{rows};1H{ANSI.BG_DARK}")

        tab_chunks = []
        for i, tab in enumerate(self.tabs):
            if i == self.active_idx:
                tab_chunks.append(f"{ANSI.BG_BLUE}{ANSI.BRIGHT_WHITE} [{i+1}:{tab.title}] {ANSI.RESET}{ANSI.BG_DARK}")
            else:
                tab_chunks.append(f"{ANSI.WHITE} {i+1}:{tab.title} {ANSI.BG_DARK}")

        left_side = f" [iceTerm] " + "".join(tab_chunks)

        metrics = TermuxBridge.get_system_metrics()
        info_parts = []
        if metrics.get("cpu_temp_c"):
            info_parts.append(f"T:{metrics['cpu_temp_c']}C")
        if metrics.get("battery_pct") is not None:
            info_parts.append(f"BAT:{metrics['battery_pct']}%")
        info_parts.append("^A:Menu")

        right_side = " | ".join(info_parts) + " "

        left_len = len(ANSI.strip(left_side))
        right_len = len(ANSI.strip(right_side))
        spaces = max(0, cols - left_len - right_len)

        full_bar = left_side + (" " * spaces) + right_side + ANSI.RESET
        sys.stdout.write(full_bar)
        sys.stdout.flush()

    def update_terminal_size(self):
        cols, rows = shutil.get_terminal_size()
        effective_rows = max(rows - 1, 5)
        import struct, fcntl
        winsize = struct.pack("HHHH", effective_rows, cols, 0, 0)
        for tab in self.tabs:
            if tab.master_fd:
                try:
                    fcntl.ioctl(tab.master_fd, termios.TIOCSWINSZ, winsize)
                except Exception:
                    pass
        self.draw_status_bar()

    def handle_ai_prompt(self):
        cols, rows = shutil.get_terminal_size()
        sys.stdout.write(f"\033[{rows};1H{ANSI.CLEAR_LINE}{ANSI.BG_BLUE}{ANSI.WHITE} [AI-Copilot] Natural language query: {ANSI.RESET} ")
        sys.stdout.flush()

        termios.tcsetattr(sys.stdin.fileno(), termios.TCSADRAIN, self.orig_termios)
        try:
            prompt = sys.stdin.readline().strip()
        except Exception:
            prompt = ""
        finally:
            tty.setraw(sys.stdin.fileno())

        if prompt:
            cmd, source = self.copilot.ask(prompt)
            tab = self.get_active_tab()
            if tab and tab.master_fd:
                os.write(tab.master_fd, cmd.encode("utf-8"))

        self.draw_status_bar()

    def handle_help_overlay(self):
        help_text = (
            f"\r\n{ANSI.CYAN}+------------------- iceTerm Shortcuts -------------------+{ANSI.RESET}\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A c{ANSI.RESET}     : Create new tab                          |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A 1..9{ANSI.RESET}  : Switch to tab [N]                       |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A n / p{ANSI.RESET} : Next / Previous tab                     |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A x{ANSI.RESET}     : Close active tab                        |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A a{ANSI.RESET}     : AI Copilot natural language prompt      |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A s{ANSI.RESET}     : Port radar and system telemetry         |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A Ctrl+A{ANSI.RESET}: Send literal Ctrl+A to terminal         |\r\n"
            f"|  {ANSI.YELLOW}Ctrl+A d{ANSI.RESET}     : Exit iceTerm session                    |\r\n"
            f"+---------------------------------------------------------+\r\n"
            f"Press any key to resume terminal...\r\n"
        )
        sys.stdout.write(help_text)
        sys.stdout.flush()
        os.read(sys.stdin.fileno(), 1)
        self.draw_status_bar()

    def run(self):
        if not sys.stdin.isatty():
            print("iceTerm multiplexer requires an interactive TTY.")
            return

        self.orig_termios = termios.tcgetattr(sys.stdin.fileno())
        tty.setraw(sys.stdin.fileno())
        self.running = True

        self.add_tab()
        self.update_terminal_size()

        def on_resize(signum, frame):
            self.update_terminal_size()

        signal.signal(signal.SIGWINCH, on_resize)

        try:
            while self.running and self.tabs:
                active_tab = self.get_active_tab()
                if not active_tab or not active_tab.is_alive:
                    self.close_current_tab()
                    if not self.tabs:
                        break
                    continue

                rfds = [sys.stdin.fileno(), active_tab.master_fd]
                try:
                    r, _, _ = select.select(rfds, [], [], 0.5)
                except (select.error, InterruptedError):
                    continue

                if active_tab.master_fd in r:
                    try:
                        data = os.read(active_tab.master_fd, 4096)
                        if data:
                            os.write(sys.stdout.fileno(), data)
                        else:
                            self.close_current_tab()
                            continue
                    except OSError:
                        self.close_current_tab()
                        continue

                if sys.stdin.fileno() in r:
                    try:
                        char = os.read(sys.stdin.fileno(), 1)
                    except OSError:
                        break

                    if not char:
                        break

                    if not self.in_prefix:
                        if char == b"\x01":  # Ctrl+A
                            self.in_prefix = True
                            continue
                        else:
                            os.write(active_tab.master_fd, char)
                    else:
                        self.in_prefix = False
                        if char == b"\x01":
                            os.write(active_tab.master_fd, b"\x01")
                        elif char in [b"c", b"C"]:
                            self.add_tab()
                            self.draw_status_bar()
                        elif char in [b"x", b"X"]:
                            self.close_current_tab()
                            self.draw_status_bar()
                        elif char in [b"n", b"N"]:
                            self.active_idx = (self.active_idx + 1) % len(self.tabs)
                            self.draw_status_bar()
                        elif char in [b"p", b"P"]:
                            self.active_idx = (self.active_idx - 1 + len(self.tabs)) % len(self.tabs)
                            self.draw_status_bar()
                        elif b"1" <= char <= b"9":
                            target = int(char.decode("ascii")) - 1
                            if target < len(self.tabs):
                                self.active_idx = target
                                self.draw_status_bar()
                        elif char in [b"a", b"A"]:
                            self.handle_ai_prompt()
                        elif char in [b"h", b"H", b"?"]:
                            self.handle_help_overlay()
                        elif char in [b"d", b"D", b"q", b"Q"]:
                            self.running = False
                            break

                self.draw_status_bar()

        finally:
            if self.orig_termios:
                termios.tcsetattr(sys.stdin.fileno(), termios.TCSADRAIN, self.orig_termios)
            for tab in self.tabs:
                tab.close()
            print(f"\n{ANSI.GREEN}[OK] iceTerm session terminated.{ANSI.RESET}")


# ==============================================================================
# 8. Command Runner with Safety Guard & Error Analysis
# ==============================================================================

def run_protected_command(cmd_args: List[str]):
    cmd = " ".join(cmd_args)
    if not cmd.strip():
        print("No command specified.")
        return

    risk, reason = SafetyGuard.evaluate(cmd)
    if risk == "DANGEROUS":
        print(f"\n{ANSI.BG_RED}{ANSI.BRIGHT_WHITE} [SECURITY-ALERT: HIGH RISK] {ANSI.RESET}")
        print(f"{ANSI.RED}Command: {cmd}{ANSI.RESET}")
        print(f"{ANSI.RED}Reason: {reason}{ANSI.RESET}")
        print(f"\nThis command can permanently corrupt system data or crash the OS.")
        try:
            confirm = input(f"To execute anyway, type '{ANSI.BOLD}I_KNOW_WHAT_I_AM_DOING{ANSI.RESET}': ")
            if confirm.strip() != "I_KNOW_WHAT_I_AM_DOING":
                print(f"{ANSI.YELLOW}Operation canceled.{ANSI.RESET}")
                return
        except KeyboardInterrupt:
            print(f"\n{ANSI.YELLOW}Operation canceled.{ANSI.RESET}")
            return
    elif risk == "WARN":
        print(f"{ANSI.YELLOW}[WARN] {reason}: {cmd}{ANSI.RESET}")

    start_t = time.time()
    proc = subprocess.Popen(
        cmd,
        shell=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )

    output_lines = []
    try:
        for line in proc.stdout:
            sys.stdout.write(line)
            sys.stdout.flush()
            output_lines.append(line)
            if len(output_lines) > 200:
                output_lines.pop(0)
    except KeyboardInterrupt:
        proc.terminate()
        print(f"\n{ANSI.YELLOW}[INTERRUPT] Command interrupted by user.{ANSI.RESET}")
        return

    proc.wait()
    duration = time.time() - start_t
    exit_code = proc.returncode

    if exit_code != 0:
        tail = "".join(output_lines[-30:])
        analysis = ErrorAnalyzer.analyze(cmd, exit_code, tail)
        print(f"\n{ANSI.BG_RED}{ANSI.BRIGHT_WHITE} [ERROR-DIAGNOSTIC]: {analysis['title']} {ANSI.RESET}")
        print(f"{ANSI.DIM}Exit Code: {exit_code} | Duration: {duration:.2f}s{ANSI.RESET}")
        print(f"Cause: {ANSI.YELLOW}{analysis['cause']}{ANSI.RESET}")
        if analysis["fix"]:
            print(f"\n{ANSI.GREEN}Suggested Fix:{ANSI.RESET}")
            for fix_line in analysis["fix"].split("\n"):
                print(f"  {ANSI.BOLD}{ANSI.CYAN}{fix_line}{ANSI.RESET}")
        print()


# ==============================================================================
# 9. CLI Subcommands & Entrypoint
# ==============================================================================

def print_banner():
    banner = f"""{ANSI.CYAN}
   ___               __                           
  / (_)______       / /____  ______ _             
 / / / __/ -_)_    / __/ -_)/ __/  ' \            
/_/_/\__/\__/( )  /__/\__//_/ /_/_/_/  v{VERSION}  
             |/   Advanced Terminal Workstation
{ANSI.RESET}"""
    print(banner)


def cmd_radar():
    print(f"\n{ANSI.BOLD}[PORT-RADAR]{ANSI.RESET}")
    ips = PortRadar.get_lan_ips()
    print(f"Local Interface IPs: {ANSI.CYAN}{', '.join(ips)}{ANSI.RESET}\n")

    listeners = PortRadar.scan_listening_ports()
    if not listeners:
        print("No active TCP listening sockets detected.")
        return

    print(f"{'PROTO':<6} {'LOCAL ADDRESS':<20} {'PORT':<8} {'ENDPOINT'}")
    print("-" * 60)
    for l in listeners:
        ip_display = f"{l['ip']}:{l['port']}"
        preview = ""
        if l["is_all_interfaces"]:
            preview = f"http://{ips[-1]}:{l['port']}"
        elif l["is_local_only"]:
            preview = f"http://127.0.0.1:{l['port']} (Local Only)"
        else:
            preview = f"http://{l['ip']}:{l['port']}"

        print(f"{'TCP':<6} {ip_display:<20} {l['port']:<8} {ANSI.GREEN}{preview}{ANSI.RESET}")
    print()


def cmd_status():
    metrics = TermuxBridge.get_system_metrics()
    print(f"\n{ANSI.BOLD}[SYS-METRICS]{ANSI.RESET}")
    print(f"  * Environment: {'Android (Termux)' if metrics['is_android'] else 'Standard Linux'}")
    if metrics["battery_pct"] is not None:
        print(f"  * Battery    : {metrics['battery_pct']}% ({metrics['battery_status']})")
    if metrics["battery_temp_c"] is not None:
        print(f"  * Battery T  : {metrics['battery_temp_c']} C")
    if metrics["cpu_temp_c"] is not None:
        print(f"  * CPU T      : {metrics['cpu_temp_c']} C")
    if metrics["mem_total_mb"]:
        used = metrics["mem_total_mb"] - (metrics["mem_avail_mb"] or 0)
        print(f"  * Memory     : {used} MB / {metrics['mem_total_mb']} MB")
    print()


def cmd_ai(prompt: str):
    copilot = AICopilot()
    cmd, source = copilot.ask(prompt)
    print(f"\n{ANSI.BOLD}[AI-Copilot]{ANSI.RESET} {ANSI.DIM}{source}{ANSI.RESET}")
    print(f"Query: {prompt}")
    print(f"Command:\n  {ANSI.BOLD}{ANSI.GREEN}{cmd}{ANSI.RESET}\n")


def cmd_explain(output_file: Optional[str] = None):
    if output_file and os.path.exists(output_file):
        with open(output_file, "r") as f:
            content = f.read()
    else:
        if not sys.stdin.isatty():
            content = sys.stdin.read()
        else:
            print("Enter error log (terminate with Ctrl+D):")
            content = sys.stdin.read()

    analysis = ErrorAnalyzer.analyze("command", 1, content)
    print(f"\n{ANSI.BG_RED}{ANSI.BRIGHT_WHITE} [ERROR-DIAGNOSTIC]: {analysis['title']} {ANSI.RESET}")
    print(f"Cause: {ANSI.YELLOW}{analysis['cause']}{ANSI.RESET}")
    if analysis["fix"]:
        print(f"\n{ANSI.GREEN}Suggested Fix:{ANSI.RESET}")
        for fix_line in analysis["fix"].split("\n"):
            print(f"  {ANSI.BOLD}{ANSI.CYAN}{fix_line}{ANSI.RESET}")
    print()


def cmd_fix_phantom():
    print(f"\n{ANSI.BOLD}[PHANTOM-GUARD]{ANSI.RESET}")
    print(TermuxBridge.phantom_process_fix_script())


def main():
    parser = argparse.ArgumentParser(
        prog="iceterm",
        description="iceTerm: Advanced dependency-free terminal workstation for Android & Linux"
    )
    parser.add_argument("-v", "--version", action="version", version=f"iceTerm {VERSION}")

    subparsers = parser.add_subparsers(dest="subcommand")

    run_parser = subparsers.add_parser("run", help="Execute command under security sandbox & error diagnostics")
    run_parser.add_argument("cmd", nargs=argparse.REMAINDER)

    ai_parser = subparsers.add_parser("ai", help="Generate shell command via coding-optimized AI router")
    ai_parser.add_argument("prompt")

    explain_parser = subparsers.add_parser("explain", help="Analyze error log and suggest fixes")
    explain_parser.add_argument("file", nargs="?")

    serve_parser = subparsers.add_parser("serve", help="Start zero-dependency Web Terminal")
    serve_parser.add_argument("--port", type=int, default=7681)
    serve_parser.add_argument("--host", default="0.0.0.0")

    subparsers.add_parser("radar", help="Scan active listening ports and interface addresses")
    subparsers.add_parser("status", help="Show system telemetry, thermal, and battery metrics")
    subparsers.add_parser("fix-phantom", help="Show instructions to disable Android 12+ Phantom Process Killer")

    args = parser.parse_args()

    if args.subcommand == "run":
        run_protected_command(args.cmd)
    elif args.subcommand == "ai":
        cmd_ai(args.prompt)
    elif args.subcommand == "explain":
        cmd_explain(args.file)
    elif args.subcommand == "serve":
        WebTerminalServer.start(host=args.host, port=args.port)
    elif args.subcommand == "radar":
        cmd_radar()
    elif args.subcommand == "status":
        cmd_status()
    elif args.subcommand == "fix-phantom":
        cmd_fix_phantom()
    else:
        if sys.stdin.isatty():
            mux = Multiplexer()
            mux.run()
        else:
            print_banner()
            parser.print_help()


if __name__ == "__main__":
    main()
