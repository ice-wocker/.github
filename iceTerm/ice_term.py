#!/usr/bin/env python3
"""
🧊 iceTerm (冰端) · 极其强大的移动端与 Linux 终端增强环境
Zero-dependency, single-file, highly-optimized terminal workstation for Android (Termux) & Linux.

Features:
- PTY Multiplexer: Multi-tab session manager with hotkeys (Ctrl+A)
- AI Copilot Native: Natural language to shell command generator & auto error explain
- Offline Heuristic Engine: Zero-network fallback for common command translations
- Android / Termux Superpowers: Port radar, battery/thermal monitor, Phantom Process killer
- Zero-Dependency Web Terminal: Built-in mobile-responsive web terminal server
- Safety Guard: Intercepts dangerous commands before execution
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
# 2. Safety Guard (危险命令安全防护)
# ==============================================================================

class SafetyGuard:
    """Detects and warns about destructive or hazardous terminal commands."""

    DANGEROUS_PATTERNS = [
        (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|\s+--recursive\s+--force)\s+/\s*$", "递归强制删除根目录 (rm -rf /)"),
        (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|\s+--recursive\s+--force)\s+/\*$", "递归强制删除根目录下所有内容 (rm -rf /*)"),
        (r"\brm\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|\s+--recursive\s+--force)\s+~\s*$", "递归强制清空用户主目录 (rm -rf ~)"),
        (r"\bmkfs(\.[a-zA-Z0-9]+)?\s+/dev/", "格式化磁盘分区 (mkfs)"),
        (r"\bdd\s+.*of=/dev/(sd[a-z]|nvme[0-9]n[0-9]|mmcblk[0-9])", "直接覆写底层物理存储块设备 (dd)"),
        (r":\(\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:", "Bash Fork 炸弹，会导致设备耗尽进程资源崩溃"),
        (r">\s*/dev/(sd[a-z]|nvme[0-9]|kmem|mem|port)", "向系统核心块设备或内核内存直接重定向写入"),
        (r"\bchmod\s+(-[a-zA-Z]*R)?\s*777\s+/\s*$", "全局修改根目录为 777 权限，严重破坏系统安全基线"),
    ]

    WARNING_PATTERNS = [
        (r"\brm\s+-[a-zA-Z]*r", "递归删除文件/目录操作"),
        (r"\bkillall\s+-9\b|\bpkill\s+-9\b", "强制强杀整组进程 (SIGKILL)"),
        (r"\bchown\s+-[a-zA-Z]*R", "递归修改文件所有者"),
        (r"\bkill\s+-9\s+1\b", "试图强制杀死 init / PID 1 进程"),
    ]

    @classmethod
    def evaluate(cls, cmd: str) -> Tuple[str, Optional[str]]:
        """
        Evaluate risk level of a command.
        Returns: ('SAFE' | 'WARN' | 'DANGEROUS', reason)
        """
        clean_cmd = cmd.strip()
        for pattern, reason in cls.DANGEROUS_PATTERNS:
            if re.search(pattern, clean_cmd):
                return "DANGEROUS", reason

        for pattern, reason in cls.WARNING_PATTERNS:
            if re.search(pattern, clean_cmd):
                return "WARN", reason

        return "SAFE", None


# ==============================================================================
# 3. Error Analyzer & Fix Auto-Suggest (智能错误诊断与修复建议)
# ==============================================================================

class ErrorAnalyzer:
    """Analyzes failed command outputs and suggests actionable fixes."""

    @classmethod
    def analyze(cls, cmd: str, exit_code: int, output_tail: str) -> Dict[str, str]:
        result = {
            "title": "命令执行失败",
            "cause": f"进程退出码为 {exit_code}",
            "fix": "",
            "category": "general"
        }
        if exit_code == 0:
            return {"title": "执行正常", "cause": "命令已成功返回", "fix": "", "category": "ok"}

        out = output_tail.lower()

        # 1. Command not found
        if "command not found" in out or "未找到命令" in out:
            missing_bin = cmd.strip().split()[0] if cmd.strip() else "unknown"
            result["title"] = f"未找到可执行程序: {missing_bin}"
            is_termux = "com.termux" in os.environ.get("PREFIX", "") or "termux" in os.environ.get("PATH", "")
            if is_termux:
                result["cause"] = f"系统中尚未安装包含 '{missing_bin}' 的 Termux 软件包。"
                result["fix"] = f"pkg install {missing_bin}  # 或 apt update && apt install {missing_bin}"
            else:
                result["cause"] = f"系统中尚未安装 '{missing_bin}' 或该程序不在系统 PATH 路径中。"
                result["fix"] = f"sudo apt update && sudo apt install {missing_bin}  # Debian/Ubuntu\n# 或 sudo pacman -S {missing_bin} # Arch"
            result["category"] = "missing_binary"
            return result

        # 2. Port already in use / Address already in use
        if "address already in use" in out or "port is already allocated" in out or "eaddrinuse" in out:
            port_match = re.search(r":(\d{2,5})\b", output_tail)
            port_hint = port_match.group(1) if port_match else "<PORT>"
            result["title"] = f"端口冲突: 端口 {port_hint} 已被占用"
            result["cause"] = f"有其他后台进程正在监听该网络端口。"
            result["fix"] = f"fuser -k {port_hint}/tcp  # 一键释放该端口\n# 或 lsof -i :{port_hint} | grep LISTEN 查看占用进程"
            result["category"] = "port_conflict"
            return result

        # 3. Permission denied
        if "permission denied" in out or "权限不够" in out:
            result["title"] = "权限不足 (Permission Denied)"
            result["cause"] = "当前用户缺乏读取、写入或执行目标资源的权限。"
            result["fix"] = f"chmod +x {cmd.split()[-1]}  # 如果是脚本可执行权限\n# 或使用 sudo/su 提升权限运行"
            result["category"] = "permission"
            return result

        # 4. Python ModuleNotFoundError / No module named
        if "modulenotfounderror: no module named" in out or "no module named" in out:
            mod_match = re.search(r"no module named ['\"]?([a-zA-Z0-9_\.]+)['\"]?", out)
            mod_name = mod_match.group(1) if mod_match else "package"
            root_mod = mod_name.split(".")[0]
            result["title"] = f"缺少 Python 模块: {root_mod}"
            result["cause"] = f"Python 运行时无法在 sys.path 中定位 '{root_mod}'。"
            result["fix"] = f"pip install {root_mod}  # 或在 Termux 中: pkg install python-{root_mod}"
            result["category"] = "python_dependency"
            return result

        # 5. Missing shared library / cannot open shared object file
        if "cannot open shared object file" in out or "error while loading shared libraries" in out:
            lib_match = re.search(r"loading shared libraries:\s+([a-zA-Z0-9_\-\.]+):", output_tail)
            lib_name = lib_match.group(1) if lib_match else "lib"
            result["title"] = f"缺少动态共享库: {lib_name}"
            result["cause"] = "系统动态链接器 (ld.so) 找不到该程序运行所依赖的动态库。"
            result["fix"] = f"apt-file search {lib_name}  # 查询所属软件包\n# 或 pkg install {lib_name.replace('lib', '').split('.')[0]}"
            result["category"] = "missing_library"
            return result

        # 6. Git merge conflict / rejected push
        if "[rejected]" in out and ("fetch first" in out or "non-fast-forward" in out):
            result["title"] = "Git 推送冲突: 远端有尚未拉取的改动"
            result["cause"] = "远端分支存在本地未包含的新提交，直接 push 被拒绝。"
            result["fix"] = "git pull --rebase origin $(git branch --show-current)\n# 解决可能存在的冲突后再执行 git push"
            result["category"] = "git_conflict"
            return result

        return result


# ==============================================================================
# 4. AI Copilot (离线规则引擎 + 在线/本地大模型端点)
# ==============================================================================

class AICopilot:
    """
    Dual-mode AI copilot:
    1. Fast offline rule engine for instant zero-network command translation.
    2. OpenAI-compatible protocol for local LLM (iceLLM/llama.cpp/Ollama) or cloud endpoints.
    """

    OFFLINE_PATTERNS = [
        # Search & Files
        (r"查找.*大于\s*(\d+)\s*(m|mb|g|gb)", lambda m: f"find . -type f -size +{m.group(1)}{m.group(2).upper()[:1]} -exec ls -lh {{}} +"),
        (r"查找.*名[字称为为包包含含含]*\s*([a-zA-Z0-9_\-\.\*]+)", lambda m: f"find . -name '*{m.group(1)}*'"),
        (r"统计.*行数", lambda _: "wc -l *"),
        (r"(查|看).*磁盘|剩余空间", lambda _: "df -h"),
        (r"(查|看).*目录大小|文件夹大小", lambda _: "du -sh * | sort -hr | head -n 15"),

        # Ports & Network
        (r"(?:查|看|找).*端口\s*(\d+)", lambda m: f"lsof -i :{m.group(1)} || fuser {m.group(1)}/tcp"),
        (r"杀(?:死|掉).*端口\s*(\d+)", lambda m: f"fuser -k {m.group(1)}/tcp"),
        (r"查看.*本机.*ip|内网ip", lambda _: "ip -br a || ifconfig"),
        (r"查看.*监听.*端口", lambda _: "ss -tulpn || netstat -tulpn"),

        # Processes & System
        (r"(?:查|看).*cpu|内存.*占用", lambda _: "ps aux --sort=-%cpu | head -n 15"),
        (r"杀死.*进程\s*([a-zA-Z0-9_\-]+)", lambda m: f"pkill -f '{m.group(1)}'"),

        # Archives
        (r"解压\s*([a-zA-Z0-9_\-\.]+\.tar\.gz)", lambda m: f"tar -zxvf {m.group(1)}"),
        (r"解压\s*([a-zA-Z0-9_\-\.]+\.zip)", lambda m: f"unzip {m.group(1)}"),
        (r"打包\s*([a-zA-Z0-9_\-]+)\s*为\s*([a-zA-Z0-9_\-\.]+\.tar\.gz)", lambda m: f"tar -zcvf {m.group(2)} {m.group(1)}"),

        # Git
        (r"查看.*最近.*提交|git log", lambda _: "git log --oneline --graph --decorate -n 10"),
        (r"放弃.*所有.*本地修改", lambda _: "git restore . && git clean -fd"),
        (r"暂存.*提交.*全部", lambda _: 'git add -A && git commit -m "update"'),

        # Termux / Android
        (r"关闭.*幽灵进程|解除.*32.*限制", lambda _: "settings put global settings_enable_monitor_phantom_procs false"),
        (r"保持后台.*不杀|休眠锁", lambda _: "termux-wake-lock"),
    ]

    def __init__(self, api_base: str = "http://127.0.0.1:8080/v1", api_key: str = "not-needed", model: str = "default"):
        self.api_base = os.environ.get("OPENAI_BASE_URL", api_base).rstrip("/")
        self.api_key = os.environ.get("OPENAI_API_KEY", api_key)
        self.model = os.environ.get("OPENAI_MODEL", model)

    def query_offline(self, natural_prompt: str) -> Optional[str]:
        """Try matching offline heuristic rules."""
        text = natural_prompt.strip()
        for pattern, handler in self.OFFLINE_PATTERNS:
            match = re.search(pattern, text, re.IGNORECASE)
            if match:
                try:
                    return handler(match)
                except Exception:
                    continue
        return None

    def query_llm(self, natural_prompt: str, timeout: int = 8) -> Tuple[bool, str]:
        """
        Query OpenAI-compatible endpoint with pure Python standard library.
        Returns: (success, result_or_error)
        """
        url = f"{self.api_base}/chat/completions"
        system_prompt = (
            "You are iceTerm AI Copilot on Linux/Android. "
            "Convert the user natural language prompt into the single most effective, safe, idiomatic shell command. "
            "Return ONLY the executable shell command inside markdown code block ```bash ... ``` with no chit-chat."
        )
        payload = {
            "model": self.model,
            "messages": [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": natural_prompt}
            ],
            "temperature": 0.1,
            "max_tokens": 128
        }
        data = json.dumps(payload).encode("utf-8")
        req = urllib.request.Request(
            url,
            data=data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.api_key}",
                "User-Agent": "iceTerm/1.0"
            }
        )

        try:
            with urllib.request.urlopen(req, timeout=timeout) as response:
                body = response.read().decode("utf-8")
                res_json = json.loads(body)
                content = res_json["choices"][0]["message"]["content"].strip()
                # Extract command from markdown if present
                code_match = re.search(r"```(?:bash|sh)?\s*([\s\S]*?)\s*```", content)
                cmd = code_match.group(1).strip() if code_match else content.strip()
                return True, cmd
        except Exception as e:
            return False, f"LLM 离线或请求失败: {e}"

    def ask(self, natural_prompt: str) -> Tuple[str, str]:
        """
        Hybrid query: Offline rule match first, then online/local LLM.
        Returns: (command, source_tag)
        """
        # 1. Offline rule match (instant)
        offline_cmd = self.query_offline(natural_prompt)
        if offline_cmd:
            return offline_cmd, "⚡ 本地离线规则引擎 (0延迟)"

        # 2. LLM query
        ok, res = self.query_llm(natural_prompt)
        if ok:
            return res, "🤖 端侧/云端大模型"

        # 3. Fallback
        return f"# 未能识别意图: '{natural_prompt}'。请使用更具体的描述或配置大模型端点。", "⚠️ 启发式兜底"


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
        """Read battery, thermal, and memory statistics."""
        data = {
            "battery_pct": None,
            "battery_status": "Unknown",
            "battery_temp_c": None,
            "cpu_temp_c": None,
            "mem_total_mb": None,
            "mem_avail_mb": None,
            "is_android": cls.is_android(),
        }

        # 1. Memory stats
        try:
            with open("/proc/meminfo", "r") as f:
                for line in f:
                    if line.startswith("MemTotal:"):
                        data["mem_total_mb"] = int(line.split()[1]) // 1024
                    elif line.startswith("MemAvailable:"):
                        data["mem_avail_mb"] = int(line.split()[1]) // 1024
        except Exception:
            pass

        # 2. Android Battery stats
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
                        # Battery temp is usually reported in tenths of a degree Celsius
                        raw_t = int(f.read().strip())
                        data["battery_temp_c"] = raw_t / 10.0 if raw_t > 100 else raw_t
            except Exception:
                pass

        # 3. CPU Thermal stats
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
        """Returns the commands required to disable Android 12+ Phantom Process Killer."""
        return (
            "# 解除 Android 12+ 幽灵进程限制 (单应用 > 32 进程自动杀后台):\n"
            "# 1. 如果你在手机内安装了 Shizuku (推荐免 Root):\n"
            "rish -c 'settings put global settings_enable_monitor_phantom_procs false'\n\n"
            "# 2. 或者在电脑/车机通过 ADB 运行:\n"
            "adb shell settings put global settings_enable_monitor_phantom_procs false\n\n"
            "# 3. 验证是否成功关闭 (输出 false 即成功):\n"
            "/system/bin/settings get global settings_enable_monitor_phantom_procs\n"
        )


class PortRadar:
    """Pure-Python scanning of /proc/net/tcp and /proc/net/tcp6 to identify listening ports."""

    @classmethod
    def get_lan_ips(cls) -> List[str]:
        """Detect local IP addresses."""
        ips = ["127.0.0.1"]
        try:
            # Connect UDP socket to a public IP to determine default egress interface
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
        """Converts /proc/net/tcp hex IP:Port to human readable format."""
        ip_hex, port_hex = hex_str.split(":")
        port = int(port_hex, 16)
        if len(ip_hex) == 8:  # IPv4
            # Little-endian hex to dotted decimal
            raw_bytes = bytes.fromhex(ip_hex)
            ip = ".".join(str(b) for b in reversed(raw_bytes))
        else:  # IPv6
            ip = "::"
        return ip, port

    @classmethod
    def scan_listening_ports(cls) -> List[Dict[str, Any]]:
        """Parses /proc/net/tcp & /proc/net/tcp6 for TCP LISTEN (state 0A)."""
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
                        # 0A is TCP_LISTEN
                        if state.upper() == "0A":
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
# 6. Zero-Dependency Web Terminal Server (内置移动端 Web 终端)
# ==============================================================================

class WebTerminalServer:
    """
    Lightweight, responsive Web Terminal server running on pure Python standard library.
    Allows accessing the phone's terminal from any browser on PC, tablet, or another phone.
    """

    HTML_PAGE = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
    <title>🧊 iceTerm Web Terminal</title>
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
        .badge { background: #238636; color: #fff; padding: 2px 6px; border-radius: 10px; font-size: 11px; }
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
        /* Virtual Keyboard Bar for Mobile */
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
            border-radius: 6px;
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
            border-radius: 6px;
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
            border-radius: 6px;
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
            <span>🧊 iceTerm Web Workstation</span>
            <span class="badge">Online</span>
        </div>
        <div style="font-size: 12px; color: #8b949e;">0-Dependency · Pure Python</div>
    </header>

    <div id="terminal-container">
        <div class="out-line">🧊 iceTerm (冰端) Web 远程终端已就绪。</div>
        <div class="out-line">输入命令即可直接执行，或点击快捷按键进行操作。</div>
        <div class="out-line">────────────────────────────────────────────</div>
    </div>

    <div id="mobile-keyboard">
        <button class="key-btn" onclick="insertKey('Ctrl+C')">Ctrl+C</button>
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
        <input type="text" id="cmd-input" placeholder="输入 Shell 命令 (例如: uname -a 或 ls)..." autofocus autocomplete="off">
        <button id="send-btn" onclick="handleSend()">执行</button>
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
            if (k === 'Ctrl+C') {
                sendCmd('^C');
            } else if (k === 'TAB') {
                insertText('  ');
            } else if (k === 'ESC') {
                input.value = '';
            }
        }

        function clearTerm() {
            term.innerHTML = '<div class="out-line">已清屏。</div>';
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
                if (data.output) {
                    appendLine(data.output, 'out-line');
                }
                if (data.exit_code !== 0 && data.exit_code !== undefined) {
                    appendLine(`[进程退出码: ${data.exit_code}]`, 'err-line');
                }
            } catch (err) {
                appendLine('执行出错: ' + err, 'err-line');
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
                pass  # Quiet logger

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

                    # Safety check
                    risk, reason = SafetyGuard.evaluate(cmd)
                    if risk == "DANGEROUS":
                        res = {
                            "output": f"🚨 [iceTerm 安全防护拦截] 危险命令被拒绝执行: {reason}",
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
                                "output": proc.stdout or "(无输出)",
                                "exit_code": proc.returncode
                            }
                        except subprocess.TimeoutExpired:
                            res = {"output": "⏱️ 命令执行超时 (超过 15 秒限制)", "exit_code": 124}
                        except Exception as e:
                            res = {"output": f"执行异常: {e}", "exit_code": 1}

                    self.send_response(200)
                    self.send_header("Content-Type", "application/json; charset=utf-8")
                    self.end_headers()
                    self.wfile.write(json.dumps(res).encode("utf-8"))

        server = http.server.ThreadingHTTPServer((host, port), Handler)
        print(f"{ANSI.GREEN}✓ iceTerm Web Terminal 服务已在端口 {port} 启动！{ANSI.RESET}")
        for ip in PortRadar.get_lan_ips():
            print(f"  👉 访问地址: {ANSI.CYAN}http://{ip}:{port}{ANSI.RESET}")
        print(f"按 Ctrl+C 停止服务。\n")
        try:
            server.serve_forever()
        except KeyboardInterrupt:
            print("\n正在停止 Web Terminal 服务...")
            server.server_close()


# ==============================================================================
# 7. Core Terminal Multiplexer & PTY Engine (多标签虚拟终端核心)
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
        self.history_buffer = bytearray()

    def spawn(self, rows: int = 24, cols: int = 80):
        master, slave = pty.openpty()
        self.master_fd = master
        self.slave_fd = slave

        # Set initial terminal size
        try:
            import struct, fcntl
            winsize = struct.pack("HHHH", rows, cols, 0, 0)
            fcntl.ioctl(slave, termios.TIOCSWINSZ, winsize)
        except Exception:
            pass

        pid = os.fork()
        if pid == 0:  # Child process
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

            # Set environment flags
            os.environ["TERM"] = "xterm-256color"
            os.environ["ICETERM_ACTIVE"] = "1"
            os.environ["ICETERM_TAB"] = str(self.tab_id)

            try:
                os.execvp(self.shell, [self.shell])
            except Exception as e:
                sys.stderr.write(f"Failed to exec {self.shell}: {e}\n")
                sys.exit(1)
        else:  # Parent process
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
    """
    Zero-dependency interactive terminal multiplexer (like a mini lightweight tmux).
    Allows running multiple tabs, switching with Ctrl+A 1..9, creating tabs with Ctrl+A c.
    """

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
        """Render a sleek, information-rich status bar at the bottom."""
        cols, rows = shutil.get_terminal_size()
        # Save cursor, move to bottom row
        sys.stdout.write(f"\033[{rows};1H{ANSI.BG_DARK}")

        tab_chunks = []
        for i, tab in enumerate(self.tabs):
            if i == self.active_idx:
                tab_chunks.append(f"{ANSI.BG_BLUE}{ANSI.BRIGHT_WHITE} [{i+1}:{tab.title}] {ANSI.RESET}{ANSI.BG_DARK}")
            else:
                tab_chunks.append(f"{ANSI.WHITE} {i+1}:{tab.title} {ANSI.BG_DARK}")

        left_side = f" 🧊 iceTerm " + "".join(tab_chunks)

        # Right side info: battery / thermal
        metrics = TermuxBridge.get_system_metrics()
        info_parts = []
        if metrics.get("cpu_temp_c"):
            info_parts.append(f"🌡️{metrics['cpu_temp_c']}°C")
        if metrics.get("battery_pct") is not None:
            info_parts.append(f"🔋{metrics['battery_pct']}%")
        info_parts.append("^A:Menu")

        right_side = " | ".join(info_parts) + " "

        left_len = len(ANSI.strip(left_side))
        right_len = len(ANSI.strip(right_side))
        spaces = max(0, cols - left_len - right_len)

        full_bar = left_side + (" " * spaces) + right_side + ANSI.RESET
        sys.stdout.write(full_bar)
        sys.stdout.flush()

    def update_terminal_size(self):
        """Propagate window resize to active PTY master."""
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
        """Interactive prompt for AI Natural Language to Shell."""
        cols, rows = shutil.get_terminal_size()
        sys.stdout.write(f"\033[{rows};1H{ANSI.CLEAR_LINE}{ANSI.BG_BLUE}{ANSI.WHITE} 🤖 AI Copilot (输入自然语言需求，按回车生成): {ANSI.RESET} ")
        sys.stdout.flush()

        # Temporarily restore cooked mode for line input
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
                # Type the generated command into active tab's PTY so user can review and hit Enter
                os.write(tab.master_fd, cmd.encode("utf-8"))

        self.draw_status_bar()

    def handle_help_overlay(self):
        """Show shortcut help screen."""
        help_text = (
            f"\r\n{ANSI.CYAN}╭─────────────────── 🧊 iceTerm 快捷键助手 ───────────────────╮{ANSI.RESET}\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A c{ANSI.RESET}     : 新建标签页 (New Tab)                          │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A 1..9{ANSI.RESET}  : 快速切换到指定标签页 (Switch Tab)             │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A n / p{ANSI.RESET} : 下一个 / 上一个标签页 (Next / Prev Tab)        │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A x{ANSI.RESET}     : 关闭当前标签页 (Close Active Tab)             │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A a{ANSI.RESET}     : 呼出 AI Copilot 自然语言转命令 (AI Assist)    │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A s{ANSI.RESET}     : 显示端口雷达与系统指标 (System Radar)         │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A Ctrl+A{ANSI.RESET}: 发送原生 Ctrl+A 给终端程序 (Send literal ^A) │\r\n"
            f"│  {ANSI.YELLOW}Ctrl+A d{ANSI.RESET}     : 退出 iceTerm 并关闭所有会话 (Exit)            │\r\n"
            f"╰───────────────────────────────────────────────────────────────╯\r\n"
            f"按任意键返回终端...\r\n"
        )
        sys.stdout.write(help_text)
        sys.stdout.flush()
        # Read one key
        os.read(sys.stdin.fileno(), 1)
        self.draw_status_bar()

    def run(self):
        """Main event loop."""
        if not sys.stdin.isatty():
            print("iceTerm multiplexer requires an interactive TTY.")
            return

        self.orig_termios = termios.tcgetattr(sys.stdin.fileno())
        tty.setraw(sys.stdin.fileno())
        self.running = True

        # Initial tab
        self.add_tab()
        self.update_terminal_size()

        # Handle SIGWINCH (window resize)
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

                # 1. Output from child PTY to stdout
                if active_tab.master_fd in r:
                    try:
                        data = os.read(active_tab.master_fd, 4096)
                        if data:
                            os.write(sys.stdout.fileno(), data)
                        else:
                            # Child closed
                            self.close_current_tab()
                            continue
                    except OSError:
                        self.close_current_tab()
                        continue

                # 2. Input from keyboard (stdin)
                if sys.stdin.fileno() in r:
                    try:
                        char = os.read(sys.stdin.fileno(), 1)
                    except OSError:
                        break

                    if not char:
                        break

                    # Check for prefix: Ctrl+A (ASCII \x01)
                    if not self.in_prefix:
                        if char == b"\x01":
                            self.in_prefix = True
                            continue
                        else:
                            # Pass-through to PTY
                            os.write(active_tab.master_fd, char)
                    else:
                        self.in_prefix = False
                        if char == b"\x01":  # Double Ctrl+A -> send literal Ctrl+A
                            os.write(active_tab.master_fd, b"\x01")
                        elif char in [b"c", b"C"]:  # New tab
                            self.add_tab()
                            self.draw_status_bar()
                        elif char in [b"x", b"X"]:  # Close tab
                            self.close_current_tab()
                            self.draw_status_bar()
                        elif char in [b"n", b"N"]:  # Next tab
                            self.active_idx = (self.active_idx + 1) % len(self.tabs)
                            self.draw_status_bar()
                        elif char in [b"p", b"P"]:  # Prev tab
                            self.active_idx = (self.active_idx - 1 + len(self.tabs)) % len(self.tabs)
                            self.draw_status_bar()
                        elif b"1" <= char <= b"9":  # Numbered tab switch
                            target = int(char.decode("ascii")) - 1
                            if target < len(self.tabs):
                                self.active_idx = target
                                self.draw_status_bar()
                        elif char in [b"a", b"A"]:  # AI Prompt
                            self.handle_ai_prompt()
                        elif char in [b"h", b"H", b"?"]:  # Help
                            self.handle_help_overlay()
                        elif char in [b"d", b"D", b"q", b"Q"]:  # Exit
                            self.running = False
                            break
                        else:
                            # Unknown command after prefix, ignore or pass
                            pass

                # Periodically redraw status bar
                self.draw_status_bar()

        finally:
            if self.orig_termios:
                termios.tcsetattr(sys.stdin.fileno(), termios.TCSADRAIN, self.orig_termios)
            for tab in self.tabs:
                tab.close()
            print(f"\n{ANSI.GREEN}iceTerm 会话已正常退出。{ANSI.RESET}")


# ==============================================================================
# 8. Command Runner with Safety Guard & Error Analysis
# ==============================================================================

def run_protected_command(cmd_args: List[str]):
    """Runs a command with SafetyGuard check and auto ErrorAnalyzer on failure."""
    cmd = " ".join(cmd_args)
    if not cmd.strip():
        print("未指定要执行的命令。")
        return

    # 1. Safety check
    risk, reason = SafetyGuard.evaluate(cmd)
    if risk == "DANGEROUS":
        print(f"\n{ANSI.BG_RED}{ANSI.BRIGHT_WHITE} 🚨 [危险操作警告] {ANSI.RESET}")
        print(f"{ANSI.RED}命令: {cmd}{ANSI.RESET}")
        print(f"{ANSI.RED}风险原因: {reason}{ANSI.RESET}")
        print(f"\n此命令可能导致系统崩溃、重要数据丢失或不可逆损坏！")
        try:
            confirm = input(f"若确认仍要强制执行，请输入 '{ANSI.BOLD}I_KNOW_WHAT_I_AM_DOING{ANSI.RESET}': ")
            if confirm.strip() != "I_KNOW_WHAT_I_AM_DOING":
                print(f"{ANSI.YELLOW}操作已取消。{ANSI.RESET}")
                return
        except KeyboardInterrupt:
            print(f"\n{ANSI.YELLOW}操作已取消。{ANSI.RESET}")
            return
    elif risk == "WARN":
        print(f"{ANSI.YELLOW}⚠️  [风险提示] {reason}: {cmd}{ANSI.RESET}")

    # 2. Run command and stream output while capturing
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
        print(f"\n{ANSI.YELLOW}命令已被用户中断。{ANSI.RESET}")
        return

    proc.wait()
    duration = time.time() - start_t
    exit_code = proc.returncode

    # 3. Analyze failure if exit code != 0
    if exit_code != 0:
        tail = "".join(output_lines[-30:])
        analysis = ErrorAnalyzer.analyze(cmd, exit_code, tail)
        print(f"\n{ANSI.BG_RED}{ANSI.BRIGHT_WHITE} ❌ 诊断报告: {analysis['title']} {ANSI.RESET}")
        print(f"{ANSI.DIM}退出码: {exit_code} | 耗时: {duration:.2f}s{ANSI.RESET}")
        print(f"原因: {ANSI.YELLOW}{analysis['cause']}{ANSI.RESET}")
        if analysis["fix"]:
            print(f"\n{ANSI.GREEN}💡 建议修复命令:{ANSI.RESET}")
            for fix_line in analysis["fix"].split("\n"):
                print(f"  {ANSI.BOLD}{ANSI.CYAN}{fix_line}{ANSI.RESET}")
        print()


# ==============================================================================
# 9. CLI Subcommands & Main Entrypoint
# ==============================================================================

def print_banner():
    banner = f"""{ANSI.CYAN}
   ___               __                           
  / (_)______       / /____  ______ _             
 / / / __/ -_)_    / __/ -_)/ __/  ' \            
/_/_/\__/\__/( )  /__/\__//_/ /_/_/_/  v{VERSION}  
             |/   极客与移动端强大终端增强环境
{ANSI.RESET}"""
    print(banner)


def cmd_radar():
    """Print network listening ports and LAN addresses."""
    print(f"\n{ANSI.BOLD}📡 [iceTerm 端口与网络雷达]{ANSI.RESET}")
    ips = PortRadar.get_lan_ips()
    print(f"本机网络接口 IP: {ANSI.CYAN}{', '.join(ips)}{ANSI.RESET}\n")

    listeners = PortRadar.scan_listening_ports()
    if not listeners:
        print("当前未探测到活动的 TCP 监听端口。")
        return

    print(f"{'协议':<6} {'本地监听地址':<20} {'端口':<8} {'外部访问预览'}")
    print("─" * 60)
    for l in listeners:
        ip_display = f"{l['ip']}:{l['port']}"
        preview = ""
        if l["is_all_interfaces"]:
            preview = f"http://{ips[-1]}:{l['port']}"
        elif l["is_local_only"]:
            preview = f"http://127.0.0.1:{l['port']} (仅本机)"
        else:
            preview = f"http://{l['ip']}:{l['port']}"

        print(f"{'TCP':<6} {ip_display:<20} {l['port']:<8} {ANSI.GREEN}{preview}{ANSI.RESET}")
    print()


def cmd_status():
    """Print hardware, thermal, and battery metrics."""
    metrics = TermuxBridge.get_system_metrics()
    print(f"\n{ANSI.BOLD}📊 [iceTerm 系统指标监控]{ANSI.RESET}")
    print(f"  • 系统环境: {'Android (Termux)' if metrics['is_android'] else 'Standard Linux'}")
    if metrics["battery_pct"] is not None:
        print(f"  • 电池电量: {metrics['battery_pct']}% ({metrics['battery_status']})")
    if metrics["battery_temp_c"] is not None:
        print(f"  • 电池温度: {metrics['battery_temp_c']} °C")
    if metrics["cpu_temp_c"] is not None:
        print(f"  • CPU 温度 : {metrics['cpu_temp_c']} °C")
    if metrics["mem_total_mb"]:
        used = metrics["mem_total_mb"] - (metrics["mem_avail_mb"] or 0)
        print(f"  • 物理内存: {used} MB / {metrics['mem_total_mb']} MB")
    print()


def cmd_ai(prompt: str):
    """Query AI copilot from CLI."""
    copilot = AICopilot()
    cmd, source = copilot.ask(prompt)
    print(f"\n{ANSI.BOLD}🤖 [AI Copilot]{ANSI.RESET} {ANSI.DIM}({source}){ANSI.RESET}")
    print(f"需求: {prompt}")
    print(f"推荐命令:\n  {ANSI.BOLD}{ANSI.GREEN}{cmd}{ANSI.RESET}\n")


def cmd_explain(output_file: Optional[str] = None):
    """Diagnose an error output log."""
    if output_file and os.path.exists(output_file):
        with open(output_file, "r") as f:
            content = f.read()
    else:
        # Read from stdin
        if not sys.stdin.isatty():
            content = sys.stdin.read()
        else:
            print("请输入或粘贴错误日志，以 Ctrl+D 结束:")
            content = sys.stdin.read()

    analysis = ErrorAnalyzer.analyze("command", 1, content)
    print(f"\n{ANSI.BG_RED}{ANSI.BRIGHT_WHITE} ❌ 诊断结果: {analysis['title']} {ANSI.RESET}")
    print(f"原因: {ANSI.YELLOW}{analysis['cause']}{ANSI.RESET}")
    if analysis["fix"]:
        print(f"\n{ANSI.GREEN}💡 修复命令:{ANSI.RESET}")
        for fix_line in analysis["fix"].split("\n"):
            print(f"  {ANSI.BOLD}{ANSI.CYAN}{fix_line}{ANSI.RESET}")
    print()


def cmd_fix_phantom():
    """Print or run Android 12+ phantom process fix."""
    print(f"\n{ANSI.BOLD}🛡️ [Android 12+ 幽灵进程守卫]{ANSI.RESET}")
    print(TermuxBridge.phantom_process_fix_script())


def main():
    parser = argparse.ArgumentParser(
        prog="iceterm",
        description="🧊 iceTerm (冰端) · 极其强大的移动端与 Linux 终端增强环境"
    )
    parser.add_argument("-v", "--version", action="version", version=f"iceTerm {VERSION}")

    subparsers = parser.add_subparsers(dest="subcommand", help="子命令")

    # 1. run command
    run_parser = subparsers.add_parser("run", help="在安全防护与错误诊断沙箱中运行指定命令")
    run_parser.add_argument("cmd", nargs=argparse.REMAINDER, help="要执行的 Shell 命令")

    # 2. ai prompt
    ai_parser = subparsers.add_parser("ai", help="通过自然语言生成 Shell 命令")
    ai_parser.add_argument("prompt", help="需求描述 (例如: '查找大于100MB的文件')")

    # 3. explain
    explain_parser = subparsers.add_parser("explain", help="智能分析终端错误输出并给出修复建议")
    explain_parser.add_argument("file", nargs="?", help="错误日志文件路径 (不填则从标准输入读取)")

    # 4. serve (web terminal)
    serve_parser = subparsers.add_parser("serve", help="启动免依赖的 Web 终端服务端")
    serve_parser.add_argument("--port", type=int, default=7681, help="监听端口 (默认: 7681)")
    serve_parser.add_argument("--host", default="0.0.0.0", help="监听地址 (默认: 0.0.0.0)")

    # 5. radar
    subparsers.add_parser("radar", help="扫描并展示当前活跃监听端口与局域网访问地址")

    # 6. status
    subparsers.add_parser("status", help="查看硬件状态、电池、CPU 温度与系统健康指标")

    # 7. fix-phantom
    subparsers.add_parser("fix-phantom", help="生成或运行解除 Android 12+ 幽灵进程杀手的方案")

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
        # Default: if run in terminal without arguments, start interactive multiplexer!
        if sys.stdin.isatty():
            mux = Multiplexer()
            mux.run()
        else:
            print_banner()
            parser.print_help()


if __name__ == "__main__":
    main()
