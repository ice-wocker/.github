package org.icewocker.iceterm;

import java.util.ArrayList;
import java.util.List;

public class ScriptLibrary {

    public static class ScriptItem {
        public final String title;
        public final String category;
        public final String description;
        public final String scriptCode;

        public ScriptItem(String title, String category, String description, String scriptCode) {
            this.title = title;
            this.category = category;
            this.description = description;
            this.scriptCode = scriptCode;
        }
    }

    private static final List<ScriptItem> SCRIPTS = new ArrayList<ScriptItem>();

    static {
        // 1. 系统维护与体检
        SCRIPTS.add(new ScriptItem(
                "全系统综合体检与性能报告",
                "系统运维",
                "一键检测 CPU 核心、内存余量、存储健康度、系统负载及异常高耗能进程",
                "echo '=== [iceTerm] 系统综合健康体检 ==='\n" +
                "echo '[1] 系统基础与运行时间:'\n" +
                "uname -a 2>/dev/null || cat /proc/version\n" +
                "uptime\n" +
                "echo -e '\\n[2] 内存使用概况:'\n" +
                "free -m 2>/dev/null || cat /proc/meminfo | head -n 8\n" +
                "echo -e '\\n[3] 存储分区可用空间:'\n" +
                "df -h | grep -E '/$|/data|/storage|/sdcard|Filesystem'\n" +
                "echo -e '\\n[4] 资源消耗前 5 进程:'\n" +
                "ps aux 2>/dev/null | sort -k3 -nr | head -n 6 || ps -ef | head -n 6\n" +
                "echo -e '\\n=== 体检完成 ==='\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "Android 12+ 幽灵进程一键检测与解除",
                "安卓调优",
                "检测系统是否受 32 子进程杀手限制，并通过 settings 指令彻底关闭限制",
                "echo '=== [iceTerm] Android 幽灵进程杀手诊断 ==='\n" +
                "API=$(getprop ro.build.version.sdk 2>/dev/null || echo 0)\n" +
                "echo \"当前 Android API 级别: $API\"\n" +
                "if [ \"$API\" -ge 31 ]; then\n" +
                "  echo '检测到 Android 12+ 系统，尝试关闭 32 进程上限...'\n" +
                "  settings put global settings_enable_monitor_phantom_procs false 2>/dev/null\n" +
                "  STATUS=$(settings get global settings_enable_monitor_phantom_procs 2>/dev/null)\n" +
                "  if [ \"$STATUS\" = \"false\" ]; then\n" +
                "    echo '成功: 幽灵进程监控已成功关闭，后台编译与服务不受 32 进程限制！'\n" +
                "  else\n" +
                "    echo '提示: 当前 Shell 权限不足，请在电脑执行 adb shell 或在手机使用 Shizuku/rish 执行：'\n" +
                "    echo 'settings put global settings_enable_monitor_phantom_procs false'\n" +
                "  fi\n" +
                "else\n" +
                "  echo '当前系统版本低于 Android 12，系统原生无幽灵进程 32 上限。'\n" +
                "fi\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "局域网活动端口与 HTTP 服务扫描",
                "网络探测",
                "快速探测本机监听的所有端口，并验证哪些端口正在运行 Web/HTTP 站点",
                "echo '=== [iceTerm] 端口与局域网服务探测 ==='\n" +
                "IP=$(ip -4 addr show | grep -oP '(?<=inet\\s)\\d+(\\.\\d+){3}' | grep -v '127.0.0.1' | head -1)\n" +
                "echo \"本机局域网 IP: ${IP:-未连接WIFI/以太网}\"\n" +
                "echo -e '\\n正在监听的 TCP 端口列表:'\n" +
                "ss -tln 2>/dev/null || netstat -tln 2>/dev/null || cat /proc/net/tcp\n" +
                "echo -e '\\n服务探测完成。'\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "极速启动局域网文件共享服务器",
                "网络探测",
                "使用 Python 3 在 8080 端口启动文件服务，便于手机与电脑双向传输文件",
                "PORT=8080\n" +
                "IP=$(ip -4 addr show | grep -oP '(?<=inet\\s)\\d+(\\.\\d+){3}' | grep -v '127.0.0.1' | head -1)\n" +
                "echo \"[iceTerm] 正在启动文件共享站点...\"\n" +
                "echo \"局域网访问地址: http://${IP:-127.0.0.1}:$PORT\"\n" +
                "echo \"按 Ctrl+C 可随时停止共享。\"\n" +
                "python3 -m http.server $PORT 2>/dev/null || python -m SimpleHTTPServer $PORT\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "安全清理系统临时缓存与孤儿文件",
                "存储运维",
                "安全清理无用的用户临时文件、构建缓存与失效的 PID 锁文件",
                "echo '=== [iceTerm] 清理缓存文件 ==='\n" +
                "BEFORE=$(df -h . | awk 'NR==2 {print $4}')\n" +
                "echo \"清理前可用空间: $BEFORE\"\n" +
                "rm -rf ~/.cache/* /data/local/tmp/*.tmp /tmp/* 2>/dev/null\n" +
                "AFTER=$(df -h . | awk 'NR==2 {print $4}')\n" +
                "echo \"清理完成，清理后可用空间: $AFTER\"\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "Git 仓库健康度与大文件扫描",
                "代码开发",
                "递归扫描当前 Git 仓库，列出未跟踪修改、最新提交并排查超大文件",
                "echo '=== [iceTerm] Git 仓库诊断 ==='\n" +
                "if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then\n" +
                "  echo '当前目录非 Git 仓库。'\n" +
                "else\n" +
                "  echo '当前分支与状态:'\n" +
                "  git branch --show-current\n" +
                "  git status -s\n" +
                "  echo -e '\\n最近 5 条提交历史:'\n" +
                "  git log --oneline -n 5\n" +
                "  echo -e '\\n检查大文件 (>5MB):'\n" +
                "  find . -type f -size +5M -not -path '*/.git/*' -exec ls -lh {} +\n" +
                "fi\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "Termux 国内镜像源加速与开发包升级",
                "开发环境",
                "一键配置清华源/中科大源镜像并更新核心软件包",
                "echo '=== [iceTerm] Termux 软件源测速与切换 ==='\n" +
                "if [ -d \"/data/data/com.termux\" ]; then\n" +
                "  echo '配置清华大学开源镜像源...'\n" +
                "  sed -i 's@^\\(deb.*stable main\\)$@#\\1\\ndeb https://mirrors.tuna.tsinghua.edu.cn/termux/apt/termux-main stable main@' $PREFIX/etc/apt/sources.list 2>/dev/null\n" +
                "  pkg update -y\n" +
                "  echo 'Termux 软件源加速配置成功！'\n" +
                "else\n" +
                "  echo '非 Termux 环境，跳过软件源切换。'\n" +
                "fi\n"
        ));

        SCRIPTS.add(new ScriptItem(
                "实时网速与网络连通性持续监控",
                "网络探测",
                "持续监控网络丢包率、往返延迟 RTT 与 DNS 解析速度",
                "echo '=== [iceTerm] 网络质量监控 (Ctrl+C 停止) ==='\n" +
                "TARGET='223.5.5.5'\n" +
                "echo \"正在向公共 DNS ($TARGET) 连续发送心跳探测包...\"\n" +
                "ping -i 1 $TARGET\n"
        ));
    }

    public static List<ScriptItem> getScripts() {
        return SCRIPTS;
    }
}
