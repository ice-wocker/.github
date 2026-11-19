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
        // =====================================================================
        // 1. 存储与文件管理 (Storage & Filesystem)
        // =====================================================================
        add("查看磁盘分区空间", "df -h", "存储管理", "以人类易读的格式展示所有挂载分区剩余容量");
        add("查看当前目录大小排行", "du -sh * | sort -hr | head -n 15", "存储管理", "按占用空间降序统计前 15 个大文件与目录");
        add("查找大于 100MB 的文件", "find . -type f -size +100M -exec ls -lh {} +", "存储管理", "递归扫描当前目录下所有超过 100MB 的大文件");
        add("查找特定扩展名文件", "find . -type f -name '*.log' -o -name '*.txt'", "存储管理", "递归搜索所有指定后缀的文件");
        add("清理缓存临时文件", "rm -rf /data/local/tmp/* ~/.cache/* 2>/dev/null", "存储管理", "安全清空用户与本地临时缓存文件");
        add("查看文件详细属性与 inode", "stat .", "存储管理", "查看文件状态、权限、修改时间与底层 inode");
        add("查看已挂载文件系统", "mount | grep -E 'ext4|f2fs|vfat|fuse'", "存储管理", "列出主要存储分区的挂载类型与挂载参数");
        add("刷新脏页数据落盘", "sync", "存储管理", "强制将系统内核缓存的写缓冲区刷入存储介质");
        add("统计当前目录总文件数", "find . -type f | wc -l", "存储管理", "计算当前目录及其子目录下包含的文件总数");
        add("查找最近 24 小时内修改过的文件", "find . -type f -mtime -1", "存储管理", "快速定位最近一天之内产生变动的文件");
        add("查找空文件并删除", "find . -type f -empty -delete", "存储管理", "检索当前工作区中的 0 字节空文件并批量移除");
        add("查看当前目录树状结构", "tree -L 2 2>/dev/null || find . -maxdepth 2 -not -path '*/.*'", "存储管理", "以树状或受限层级结构清晰预览目录树");
        add("创建多级深层嵌套目录", "mkdir -p path/to/nested/directory", "存储管理", "递归自动补全并创建深层不存在的父子目录");
        add("计算文件 SHA256 校验和", "sha256sum <文件>", "存储管理", "计算并核对文件的 SHA256 完整性哈希值");
        add("安全覆盖粉碎删除文件", "shred -u -z -n 3 <文件> 2>/dev/null || rm -f <文件>", "存储管理", "多次伪随机覆写文件数据防止数据恢复");

        // =====================================================================
        // 2. 进程管理与性能监控 (Process & Performance)
        // =====================================================================
        add("查看实时系统负载与前台进程", "top -m 10 -d 2", "系统性能", "每2秒刷新一次前10个最消耗资源的进程");
        add("查看 CPU 架构与核心信息", "cat /proc/cpuinfo | grep -E 'processor|model name|Hardware|BogoMIPS'", "系统性能", "输出处理器核心数与硬件平台架构");
        add("查看物理内存详细分配", "cat /proc/meminfo | head -n 12", "系统性能", "查看物理内存、空闲内存、缓存与可用内存");
        add("按 CPU 占用排序前 15 进程", "ps -ef --sort=-%cpu 2>/dev/null || ps aux | head -n 16", "系统性能", "定位系统中最为耗费算力的进程列表");
        add("按内存占用排序前 15 进程", "ps -ef --sort=-%mem 2>/dev/null || ps aux | sort -k4 -nr | head -n 16", "系统性能", "定位占用物理内存最高的进程列表");
        add("查看当前系统开机时间与负载", "uptime", "系统性能", "展示系统连续运行时间以及 1/5/15 分钟平均负载");
        add("强制杀死指定名称的所有进程", "pkill -f '<进程名>'", "系统性能", "根据命令名称模糊匹配并批量强杀进程");
        add("查看虚拟内存与IO状态统计", "vmstat 1 5", "系统性能", "每隔 1 秒输出一次系统调度、内存与 IO 读写统计");
        add("查看指定进程占用的打开句柄", "ls -la /proc/<PID>/fd", "系统性能", "排查进程句柄泄漏与正在读写的文件/Socket");
        add("查看进程真实执行路径", "readlink -f /proc/<PID>/exe", "系统性能", "获取指定 PID 对应可执行文件的绝对物理路径");
        add("查看进程环境变量", "tr '\\0' '\\n' < /proc/<PID>/environ", "系统性能", "读取正在运行进程的完整环境变量配置");
        add("查看系统全部进程树", "pstree -p 2>/dev/null || ps -ef", "系统性能", "层次化树形展示进程父子派生血缘关系");
        add("向进程发送友好终止信号", "kill -15 <PID>", "系统性能", "发送 SIGTERM 允许应用执行正常清理退出逻辑");

        // =====================================================================
        // 3. 网络与端口探测 (Network & Port Radar)
        // =====================================================================
        add("查看本机所有监听端口", "ss -tulpn || netstat -tulpn", "网络调试", "列出 TCP 与 UDP 所有正在监听的端口及所属进程");
        add("查看网络接口与 IP 地址", "ip -br a || ifconfig", "网络调试", "列出 wlan0、lo 等网卡的简要 IPv4/IPv6 地址");
        add("查看当前 TCP 连接状态汇总", "netstat -nat | awk '{print $6}' | sort | uniq -c | sort -n", "网络调试", "统计 ESTABLISHED、TIME_WAIT 等状态数量");
        add("测试网络连通性", "ping -c 4 223.5.5.5", "网络调试", "向公共 DNS 发送 4 个 ICMP 探测包测试网络延迟");
        add("路由追踪 (Traceroute)", "traceroute -n 223.5.5.5", "网络调试", "探测到目标主机的路由跃点跳数与网络延迟");
        add("HTTP 请求并打印响应头", "curl -I https://www.baidu.com", "网络调试", "仅抓取目标 URL 的 HTTP 状态码与响应头");
        add("抓取并保存大文件 (支持断点续传)", "curl -C - -O <URL>", "网络调试", "通过 curl 断点续传下载网络资源文件");
        add("测试远程主机指定端口是否开放", "nc -zv <主机> <端口> 2>/dev/null || timeout 2 bash -c \"</dev/tcp/<主机>/<端口>\" && echo '开放' || echo '关闭'", "网络调试", "快速判断目标 TCP 端口是否畅通且服务在运行");
        add("查看本机 DNS 域名解析配置", "cat /etc/resolv.conf", "网络调试", "检查当前系统配置的上游 DNS Nameserver");
        add("查询域名的 A 记录解析", "nslookup <域名> || host <域名>", "网络调试", "使用公共 DNS 服务器解析目标域名的 IPv4 地址");
        add("获取本机出网公网 IP 地址", "curl -s https://api.ipify.org && echo", "网络调试", "通过权威外部接口获取出口公网 IP");
        add("显示当前内核路由表", "ip route show || route -n", "网络调试", "查看默认网关与各网络接口子网路由策略");
        add("强杀占用指定 TCP 端口的进程", "fuser -k <端口>/tcp", "网络调试", "一键终止占用特定开发端口的进程释放端口冲突");

        // =====================================================================
        // 4. 文本处理与三剑客 (Text & Utilities)
        // =====================================================================
        add("在目录中递归搜索文本", "grep -rn --exclude-dir=.git '<关键词>' .", "文本处理", "在当前工程中高效模糊检索指定代码或文本");
        add("统计文本文件行数、词数与字节", "wc -l -w -c <文件>", "文本处理", "快速统计划定文本或源码文件的体积指标");
        add("查看文件首尾内容", "head -n 20 <文件> && tail -n 20 <文件>", "文本处理", "分别预览目标文件的前 20 行与最后 20 行");
        add("动态实时追踪日志末尾", "tail -f <日志文件>", "文本处理", "类似 logcat 持续监听追加写入的新日志行");
        add("去重并统计重复出现频次", "sort <文件> | uniq -c | sort -nr", "文本处理", "对文本行排序并计算每行重复出现的频次");
        add("正则替换文件中的文本", "sed -i 's/旧内容/新内容/g' <文件>", "文本处理", "原地执行全局字符串正则替换");
        add("提取特定列数据", "awk -F':' '{print $1, $3}' /etc/passwd", "文本处理", "以冒号为分隔符输出第一列用户名和第三列 UID");
        add("删除文本文件中的所有空行", "sed -i '/^[[:space:]]*$/d' <文件>", "文本处理", "高效清除文档中所有空白空行");
        add("将制表符 Tab 转换为 4 个空格", "sed -i 's/\\t/    /g' <文件>", "文本处理", "规范代码文件中的缩进对齐格式");
        add("比对两个文本文件的差异", "diff -u <文件1> <文件2>", "文本处理", "输出标准 unified 格式的文件逐行增删比对");

        // =====================================================================
        // 5. 归档与解压 (Archives & Compression)
        // =====================================================================
        add("解压 tar.gz 归档", "tar -zxvf <文件>.tar.gz", "归档解压", "解包并解压缩 gzip 格式 tar 归档文件");
        add("解压 tar.xz 归档", "tar -Jxvf <文件>.tar.xz", "归档解压", "解包并解压缩 xz 高压缩比归档文件");
        add("打包当前目录为 tar.gz", "tar -zcvf archive.tar.gz --exclude=archive.tar.gz .", "归档解压", "将当前所有文件压缩归档为 tar.gz 包");
        add("解压 zip 压缩包", "unzip <文件>.zip", "归档解压", "解压缩标准 zip 文件到当前目录");
        add("创建包含当前目录的高压 zip 包", "zip -r -9 archive.zip . -x '*.git*'", "归档解压", "以最高压缩比归档文件并排除版本控制目录");
        add("不解压直接列出 tar 内部清单", "tar -ztvf <文件>.tar.gz", "归档解压", "预览压缩包内的文件目录树与权限详情");

        // =====================================================================
        // 6. Android 底层调试与 Shizuku (Android Internals)
        // =====================================================================
        add("获取所有系统属性 (getprop)", "getprop | grep -E 'ro.build.version|ro.product.model'", "安卓底层", "过滤查询设备型号、品牌与 Android 系统版本");
        add("解除 Android 12+ 幽灵进程杀手", "settings put global settings_enable_monitor_phantom_procs false", "安卓底层", "关闭系统单应用超过 32 子进程自动 SIGKILL 限制");
        add("查询幽灵进程监控当前状态", "settings get global settings_enable_monitor_phantom_procs", "安卓底层", "查询结果为 false 表示已成功关闭后台杀手");
        add("查看电池详细底层状态", "dumpsys battery", "安卓底层", "获取充电状态、电压、电池健康度与电芯温度");
        add("导出系统日志 (Logcat 过滤错误)", "logcat -d *:E | tail -n 50", "安卓底层", "抓取系统最新发生的 50 条 Error 级别崩溃日志");
        add("列出第三方已安装应用包名", "pm list packages -3", "安卓底层", "获取设备上安装的所有用户第三方应用 PackageName");
        add("查看当前运行前台应用 Activity", "dumpsys window | grep -E 'mCurrentFocus|mFocusedApp'", "安卓底层", "获取当前屏幕处于活动顶层的 Activity 窗口");
        add("查看当前屏幕分辨率与密度", "wm size && wm density", "安卓底层", "获取物理屏幕宽高像素与当前 DPI 屏幕密度");
        add("强制停止指定第三方应用", "am force-stop <包名>", "安卓底层", "强制杀停后台持续耗电或卡死的用户应用");
        add("截取当前手机屏幕保存为 PNG", "screencap -p /sdcard/screenshot.png", "安卓底层", "免第三方工具直接获取屏幕无损截屏");

        // =====================================================================
        // 7. 开发环境与 Git (Developer Tools)
        // =====================================================================
        add("查看 Git 当前状态与精简日志", "git status -s && git log -n 5 --oneline", "开发工具", "快速确认本地分支修改与最近五条提交记录");
        add("图形化查看 Git 提交分支树", "git log --graph --oneline --decorate --all -n 12", "开发工具", "直观展示多分支合并、分叉与演进历史");
        add("暂存并提交所有更改", "git add -A && git commit -m \"update\"", "开发工具", "一键暂存所有修改并生成提交记录");
        add("强制撤销本地所有未提交改动", "git restore . && git clean -fd", "开发工具", "危险：彻底抛弃所有未提交的修改与新增文件");
        add("查看工作区未暂存的具体差异", "git diff", "开发工具", "高亮显示工作区文件相对于暂存区的详细修改点");
        add("启动本地 Python HTTP 文件服务器", "python3 -m http.server 8080", "开发工具", "在 8080 端口启动轻量级局域网文件共享站点");
        add("查看环境变量与 PATH 路径", "echo -e \"PATH: $PATH\\nHOME: $HOME\\nUSER: $USER\"", "开发工具", "检查当前终端环境的基础路径与运行配置");
        add("统计项目各编程语言代码总行数", "find . -type f \\( -name '*.java' -o -name '*.py' -o -name '*.c' -o -name '*.sh' \\) -exec wc -l {} +", "开发工具", "扫描统计源码文件行数");
        add("临时保存未提交修改 (Stash)", "git stash push -m 'temp_save' && git stash list", "开发工具", "安全贮藏工作现场以便切换分支作业");
        add("恢复最近一次暂存的现场", "git stash pop", "开发工具", "弹出并还原最近一次缓存的工作区修改");

        // =====================================================================
        // 8. 容器与云原生/Proot (Containers & Linux Distro)
        // =====================================================================
        add("查看运行中的 Docker 容器", "docker ps --format 'table {{.ID}}\\t{{.Image}}\\t{{.Status}}\\t{{.Ports}}'", "容器环境", "格式化查看当前活动的容器列表");
        add("查看 Docker 镜像占用总空间", "docker system df", "容器环境", "分析镜像、容器、数据卷与构建缓存的空间占用");
        add("清理停止运行的无用容器与镜像", "docker system prune -f", "容器环境", "安全移除悬空镜像与已退出的停止容器");
        add("查看 Proot Linux 当前发行版", "cat /etc/os-release 2>/dev/null || cat /etc/issue", "容器环境", "确认当前 chroot/proot 容器的 Linux 详细版本");

        // =====================================================================
        // 9. 安全审计与网络加固 (Security & SSL)
        // =====================================================================
        add("检查远程 SSL 证书过期时间", "echo | openssl s_client -servername <域名> -connect <域名>:443 2>/dev/null | openssl x509 -noout -dates", "安全运维", "查询 HTTPS 站点的证书起止有效期");
        add("生成 2048 位 RSA 私钥与公钥", "openssl genrsa -out private.pem 2048 && openssl rsa -in private.pem -pubout -out public.pem", "安全运维", "快速生成标准 RSA 非对称加解密密钥对");
        add("生成 32 字符高强度强密码", "head -c 32 /dev/urandom | base64 | head -c 32 && echo", "安全运维", "利用系统真随机熵池生成强随机密码");
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
