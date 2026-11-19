package org.icewocker.iceterm;

import java.util.ArrayList;
import java.util.List;

public class CommandToolbox {

    public static class CommandItem {
        public final String name;
        public final String command;
        public final String category;
        public final String description;

        public CommandItem(String name, String command, String category, String description) {
            this.name = name;
            this.command = command;
            this.category = category;
            this.description = description;
        }
    }

    private static final List<CommandItem> ITEMS = new ArrayList<CommandItem>();

    static {
        // 1. 磁盘与存储 (Storage & Disk)
        add("查看磁盘分区空间", "df -h", "存储管理", "以人类易读的格式展示所有挂载分区剩余容量");
        add("查看当前目录大小排行", "du -sh * | sort -hr | head -n 15", "存储管理", "按占用空间降序统计前 15 个大文件与目录");
        add("查找大于 100MB 的文件", "find . -type f -size +100M -exec ls -lh {} +", "存储管理", "递归扫描当前目录下所有超过 100MB 的大文件");
        add("清理缓存临时文件", "rm -rf /data/local/tmp/* ~/.cache/* 2>/dev/null", "存储管理", "安全清空用户与本地临时缓存文件");
        add("查看文件详细属性与 inode", "stat .", "存储管理", "查看文件状态、权限、修改时间与底层 inode");
        add("查看已挂载文件系统", "mount | grep -E 'ext4|f2fs|vfat|fuse'", "存储管理", "列出主要存储分区的挂载类型与挂载参数");
        add("刷新脏页数据落盘", "sync", "存储管理", "强制将系统内核缓存的写缓冲区刷入存储介质");

        // 2. 进程与性能监控 (Process & Performance)
        add("查看实时系统负载与前台进程", "top -m 10 -d 2", "系统性能", "每2秒刷新一次前10个最消耗资源的进程");
        add("查看 CPU 架构与核心信息", "cat /proc/cpuinfo | grep -E 'processor|model name|Hardware|BogoMIPS'", "系统性能", "输出处理器核心数与硬件平台架构");
        add("查看物理内存详细分配", "cat /proc/meminfo | head -n 12", "系统性能", "查看物理内存、空闲内存、缓存与可用内存");
        add("按 CPU 占用排序前 15 进程", "ps -ef --sort=-%cpu 2>/dev/null || ps aux | head -n 16", "系统性能", "定位系统中最为耗费算力的进程列表");
        add("查看当前系统开机时间与负载", "uptime", "系统性能", "展示系统连续运行时间以及 1/5/15 分钟平均负载");
        add("强制杀死指定名称的所有进程", "pkill -f '<进程名>'", "系统性能", "根据命令名称模糊匹配并批量强杀进程");
        add("查看虚拟内存与IO状态统计", "vmstat 1 5", "系统性能", "每隔 1 秒输出一次系统调度、内存与 IO 读写统计");

        // 3. 网络与端口探测 (Network & Port Radar)
        add("查看本机所有监听端口", "ss -tulpn || netstat -tulpn", "网络调试", "列出 TCP 与 UDP 所有正在监听的端口及所属进程");
        add("查看网络接口与 IP 地址", "ip -br a || ifconfig", "网络调试", "列出 wlan0、lo 等网卡的简要 IPv4/IPv6 地址");
        add("查看当前 TCP 连接状态汇总", "netstat -nat | awk '{print $6}' | sort | uniq -c | sort -n", "网络调试", "统计 ESTABLISHED、TIME_WAIT 等状态数量");
        add("测试网络连通性", "ping -c 4 223.5.5.5", "网络调试", "向公共 DNS 发送 4 个 ICMP 探测包测试网络延迟");
        add("路由追踪 (Traceroute)", "traceroute -n 223.5.5.5", "网络调试", "探测到目标主机的路由跃点跳数与网络延迟");
        add("HTTP 请求并打印响应头", "curl -I https://www.baidu.com", "网络调试", "仅抓取目标 URL 的 HTTP 状态码与响应头");
        add("抓取并保存大文件 (支持断点续传)", "curl -C - -O <URL>", "网络调试", "通过 curl 断点续传下载网络资源文件");

        // 4. 文本处理与三剑客 (Text & Utilities)
        add("在目录中递归搜索文本", "grep -rn --exclude-dir=.git '<关键词>' .", "文本处理", "在当前工程中高效模糊检索指定代码或文本");
        add("查看文件首尾内容", "head -n 20 <文件> &amp;&amp; tail -n 20 <文件>", "文本处理", "分别预览目标文件的前 20 行与最后 20 行");
        add("动态实时追踪日志末尾", "tail -f <日志文件>", "文本处理", "类似 logcat 持续监听追加写入的新日志行");
        add("去重并统计重复出现频次", "sort <文件> | uniq -c | sort -nr", "文本处理", "对文本行排序并计算每行重复出现的频次");
        add("正则替换文件中的文本", "sed -i 's/旧内容/新内容/g' <文件>", "文本处理", "原地执行全局字符串正则替换");
        add("提取特定列数据", "awk -F':' '{print $1, $3}' /etc/passwd", "文本处理", "以冒号为分隔符输出第一列用户名和第三列 UID");

        // 5. 归档与解压 (Archives & Compression)
        add("解压 tar.gz 归档", "tar -zxvf <文件>.tar.gz", "归档解压", "解包并解压缩 gzip 格式 tar 归档文件");
        add("解压 tar.xz 归档", "tar -Jxvf <文件>.tar.xz", "归档解压", "解包并解压缩 xz 高压缩比归档文件");
        add("打包当前目录为 tar.gz", "tar -zcvf archive.tar.gz --exclude=archive.tar.gz .", "归档解压", "将当前所有文件压缩归档为 tar.gz 包");
        add("解压 zip 压缩包", "unzip <文件>.zip", "归档解压", "解压缩标准 zip 文件到当前目录");

        // 6. Android 底层调试与 Shizuku (Android Internals)
        add("获取所有系统属性 (getprop)", "getprop | grep -E 'ro.build.version|ro.product.model'", "安卓底层", "过滤查询设备型号、品牌与 Android 系统版本");
        add("解除 Android 12+ 幽灵进程杀手", "settings put global settings_enable_monitor_phantom_procs false", "安卓底层", "关闭系统单应用超过 32 子进程自动 SIGKILL 限制");
        add("查询幽灵进程监控当前状态", "settings get global settings_enable_monitor_phantom_procs", "安卓底层", "查询结果为 false 表示已成功关闭后台杀手");
        add("查看电池详细底层状态", "dumpsys battery", "安卓底层", "获取充电状态、电压、电池健康度与电芯温度");
        add("导出系统日志 (Logcat 过滤错误)", "logcat -d *:E | tail -n 50", "安卓底层", "抓取系统最新发生的 50 条 Error 级别崩溃日志");
        add("列出第三方已安装应用包名", "pm list packages -3", "安卓底层", "获取设备上安装的所有用户第三方应用 PackageName");
        add("查看当前运行前台应用 Activity", "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'", "安卓底层", "获取当前屏幕处于活动顶层的 Activity 窗口");

        // 7. 开发环境与 Git (Developer Tools)
        add("查看 Git 当前状态与精简日志", "git status -s &amp;&amp; git log -n 5 --oneline", "开发工具", "快速确认本地分支修改与最近五条提交记录");
        add("暂存并提交所有更改", "git add -A &amp;&amp; git commit -m \"update\"", "开发工具", "一键暂存所有修改并生成提交记录");
        add("强制撤销本地所有未提交改动", "git restore . &amp;&amp; git clean -fd", "开发工具", "危险：彻底抛弃所有未提交的修改与新增文件");
        add("启动本地 Python HTTP 文件服务器", "python3 -m http.server 8080", "开发工具", "在 8080 端口启动轻量级局域网文件共享站点");
        add("查看环境变量与 PATH 路径", "echo -e \"PATH: $PATH\\nHOME: $HOME\\nUSER: $USER\"", "开发工具", "检查当前终端环境的基础路径与运行配置");
    }

    private static void add(String name, String cmd, String cat, String desc) {
        ITEMS.add(new CommandItem(name, cmd, cat, desc));
    }

    public static List<CommandItem> getItems() {
        return ITEMS;
    }

    public static List<String> getCategories() {
        List<String> cats = new ArrayList<String>();
        for (CommandItem item : ITEMS) {
            if (!cats.contains(item.category)) {
                cats.add(item.category);
            }
        }
        return cats;
    }

    public static List<CommandItem> filterByCategory(String cat) {
        List<CommandItem> res = new ArrayList<CommandItem>();
        for (CommandItem item : ITEMS) {
            if (cat == null || "全部".equals(cat) || "ALL".equalsIgnoreCase(cat) || item.category.equals(cat)) {
                res.add(item);
            }
        }
        return res;
    }
}
