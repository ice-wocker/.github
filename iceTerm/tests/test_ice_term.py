"""
Unit tests for iceTerm (纯标准库无依赖单测).
"""

import unittest
import os
import sys
import json
import threading
import time
import urllib.request

# Ensure iceTerm package is in sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))
import ice_term as it


class TestSafetyGuard(unittest.TestCase):
    """Test dangerous command detection and safety warnings."""

    def test_dangerous_commands(self):
        dangerous_cases = [
            "rm -rf /",
            "rm -rf /*",
            "rm -rf ~",
            "mkfs.ext4 /dev/sda1",
            "dd if=/dev/zero of=/dev/sda bs=1M",
            ":(){ :|:& };:",
            "chmod -R 777 /",
            "chmod 777 /",
        ]
        for cmd in dangerous_cases:
            risk, reason = it.SafetyGuard.evaluate(cmd)
            self.assertEqual(risk, "DANGEROUS", f"Command should be dangerous: {cmd}")
            self.assertIsNotNone(reason)

    def test_warning_commands(self):
        warn_cases = [
            "rm -r ./build",
            "killall -9 python",
            "pkill -9 node",
            "kill -9 1",
        ]
        for cmd in warn_cases:
            risk, reason = it.SafetyGuard.evaluate(cmd)
            self.assertEqual(risk, "WARN", f"Command should be warn: {cmd}")
            self.assertIsNotNone(reason)

    def test_safe_commands(self):
        safe_cases = [
            "ls -la",
            "git status",
            "python3 app.py",
            "cat /etc/os-release",
            "mkdir -p src/test",
            "echo 'hello world'",
        ]
        for cmd in safe_cases:
            risk, reason = it.SafetyGuard.evaluate(cmd)
            self.assertEqual(risk, "SAFE", f"Command should be safe: {cmd}")
            self.assertIsNone(reason)


class TestErrorAnalyzer(unittest.TestCase):
    """Test intelligent error diagnostics and suggested fixes."""

    def test_command_not_found(self):
        res = it.ErrorAnalyzer.analyze("htop", 127, "bash: htop: command not found")
        self.assertEqual(res["category"], "missing_binary")
        self.assertIn("htop", res["title"])
        self.assertTrue("install" in res["fix"])

    def test_port_in_use(self):
        sample = "Error: listen EADDRINUSE: address already in use :::8080"
        res = it.ErrorAnalyzer.analyze("npm start", 1, sample)
        self.assertEqual(res["category"], "port_conflict")
        self.assertIn("8080", res["title"])
        self.assertIn("fuser -k", res["fix"])

    def test_permission_denied(self):
        sample = "bash: ./run.sh: Permission denied"
        res = it.ErrorAnalyzer.analyze("./run.sh", 126, sample)
        self.assertEqual(res["category"], "permission")
        self.assertIn("chmod +x", res["fix"])

    def test_python_missing_module(self):
        sample = "ModuleNotFoundError: No module named 'requests'"
        res = it.ErrorAnalyzer.analyze("python app.py", 1, sample)
        self.assertEqual(res["category"], "python_dependency")
        self.assertIn("requests", res["fix"])

    def test_git_rejected(self):
        sample = "! [rejected] main -> main (fetch first) error: failed to push some refs"
        res = it.ErrorAnalyzer.analyze("git push origin main", 1, sample)
        self.assertEqual(res["category"], "git_conflict")
        self.assertIn("git pull --rebase", res["fix"])


class TestAICopilotOffline(unittest.TestCase):
    """Test AI Copilot offline heuristic pattern matcher."""

    def setUp(self):
        self.copilot = it.AICopilot()

    def test_search_large_files(self):
        cmd = self.copilot.query_offline("查找大于 200MB 的文件")
        self.assertIsNotNone(cmd)
        self.assertIn("find .", cmd)
        self.assertIn("200M", cmd)

    def test_search_port(self):
        cmd = self.copilot.query_offline("查找占用端口 3000 的进程")
        self.assertIsNotNone(cmd)
        self.assertIn("3000", cmd)
        self.assertTrue("lsof" in cmd or "fuser" in cmd)

    def test_kill_port(self):
        cmd = self.copilot.query_offline("杀死占用端口 8080 的进程")
        self.assertIsNotNone(cmd)
        self.assertIn("fuser -k 8080/tcp", cmd)

    def test_archive_extract(self):
        cmd = self.copilot.query_offline("解压 test.tar.gz")
        self.assertEqual(cmd, "tar -zxvf test.tar.gz")

    def test_android_phantom_process(self):
        cmd = self.copilot.query_offline("关闭幽灵进程限制")
        self.assertIn("settings_enable_monitor_phantom_procs false", cmd)

    def test_fallback_unrecognized(self):
        cmd, source = self.copilot.ask("完全无法理解的乱码提示词 xyz123!@#")
        self.assertIn("未能识别意图", cmd)


class TestPortRadar(unittest.TestCase):
    """Test PortRadar IP and port parsing logic."""

    def test_parse_hex_endpoint(self):
        # 0100007F:1F90 is 127.0.0.1:8080
        ip, port = it.PortRadar.parse_hex_endpoint("0100007F:1F90")
        self.assertEqual(ip, "127.0.0.1")
        self.assertEqual(port, 8080)

    def test_get_lan_ips(self):
        ips = it.PortRadar.get_lan_ips()
        self.assertIn("127.0.0.1", ips)


class TestTermuxBridge(unittest.TestCase):
    """Test system metrics and phantom process scripts."""

    def test_get_system_metrics(self):
        metrics = it.TermuxBridge.get_system_metrics()
        self.assertIn("is_android", metrics)
        self.assertIn("mem_total_mb", metrics)

    def test_phantom_process_script(self):
        script = it.TermuxBridge.phantom_process_fix_script()
        self.assertIn("settings_enable_monitor_phantom_procs false", script)
        self.assertIn("Shizuku", script)


class TestWebTerminalServer(unittest.TestCase):
    """Test built-in zero-dependency HTTP Web Terminal."""

    @classmethod
    def setUpClass(cls):
        cls.port = 9876
        cls.thread = threading.Thread(
            target=it.WebTerminalServer.start,
            kwargs={"host": "127.0.0.1", "port": cls.port},
            daemon=True
        )
        cls.thread.start()
        time.sleep(0.5)

    def test_web_page_get(self):
        req = urllib.request.urlopen(f"http://127.0.0.1:{self.port}/")
        html = req.read().decode("utf-8")
        self.assertIn("iceTerm Web Workstation", html)

    def test_status_endpoint(self):
        req = urllib.request.urlopen(f"http://127.0.0.1:{self.port}/api/status")
        data = json.loads(req.read().decode("utf-8"))
        self.assertIn("is_android", data)

    def test_exec_safe(self):
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/exec",
            data=json.dumps({"command": "echo TEST_OK_123"}).encode("utf-8"),
            headers={"Content-Type": "application/json"}
        )
        resp = json.loads(urllib.request.urlopen(req).read().decode("utf-8"))
        self.assertEqual(resp["exit_code"], 0)
        self.assertIn("TEST_OK_123", resp["output"])

    def test_exec_dangerous_intercept(self):
        req = urllib.request.Request(
            f"http://127.0.0.1:{self.port}/api/exec",
            data=json.dumps({"command": "rm -rf /"}).encode("utf-8"),
            headers={"Content-Type": "application/json"}
        )
        resp = json.loads(urllib.request.urlopen(req).read().decode("utf-8"))
        self.assertEqual(resp["exit_code"], 126)
        self.assertIn("安全防护拦截", resp["output"])


if __name__ == "__main__":
    unittest.main()
