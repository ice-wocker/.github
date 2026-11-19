package org.icewocker.iceterm;

import java.util.ArrayList;
import java.util.List;

public class DevCheatsheet {

    public static class CheatItem {
        public final String title;
        public final String category;
        public final String content;

        public CheatItem(String title, String category, String content) {
            this.title = title;
            this.category = category;
            this.content = content;
        }
    }

    private static final List<CheatItem> ITEMS = new ArrayList<CheatItem>();

    static {
        ITEMS.add(new CheatItem(
                "Vim 核心编辑与常用快捷键",
                "编辑器",
                "【基础模式切换】\n" +
                "i / a : 光标前/后进入插入模式\n" +
                "o / O : 在当前行下/上方新建一行并进入插入模式\n" +
                "ESC   : 退出任何模式回到正常模式\n" +
                ":w    : 保存文件内容\n" +
                ":q    : 退出 (:q! 强制不保存退出)\n" +
                ":wq   : 保存并退出 (等同于 ZZ)\n\n" +
                "【移动与导航】\n" +
                "h, j, k, l : 左、下、上、右\n" +
                "0 / $       : 移动到行首 / 行尾\n" +
                "^           : 移动到行首第一个非空字符\n" +
                "gg / G      : 跳至文件首行 / 尾行\n" +
                ":<行号>     : 直接跳至指定行号\n" +
                "w / b       : 向后/向前跳一个单词\n\n" +
                "【编辑与撤销】\n" +
                "x     : 删除光标处单个字符\n" +
                "dd    : 剪切/删除当前整行\n" +
                "yy    : 复制当前整行\n" +
                "p / P : 在光标后/前粘贴剪贴板内容\n" +
                "u     : 撤销上一步操作 (Undo)\n" +
                "Ctrl+r: 重做被撤销的操作 (Redo)\n" +
                "/关键词: 向下搜索关键词 (n 下一个, N 上一个)\n" +
                ":%s/旧/新/g: 全文正则替换"
        ));

        ITEMS.add(new CheatItem(
                "正则表达式 (Regex) 语法速查",
                "语法速查",
                "【元字符与边界】\n" +
                "^ : 匹配字符串或行首\n" +
                "$ : 匹配字符串或行尾\n" +
                ". : 匹配除换行符外任意单个字符\n" +
                "\\b: 匹配单词边界\n" +
                "\\d / \\D: 匹配数字 / 非数字 [0-9]\n" +
                "\\w / \\W: 匹配字母数字下划线 / 非单词字符\n" +
                "\\s / \\S: 匹配空白字符 / 非空白字符\n\n" +
                "【量词与分组】\n" +
                "* : 匹配 0 次或多次 (贪婪)\n" +
                "+ : 匹配 1 次或多次 (贪婪)\n" +
                "? : 匹配 0 次或 1 次 (或用于非贪婪 *?, +?)\n" +
                "{n,m}: 匹配至少 n 次、最多 m 次\n" +
                "(...): 捕获分组\n" +
                "(?:...): 非捕获分组\n\n" +
                "【常用模式】\n" +
                "IPv4地址: ((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\n" +
                "URL地址 : https?://[a-zA-Z0-9.-]+(?:\\:[0-9]+)?(?:/[^\\s]*)?\n" +
                "手机号码: 1[3-9]\\d{9}\n" +
                "电子邮箱: [a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        ));

        ITEMS.add(new CheatItem(
                "Linux POSIX 信号量与进程控制",
                "系统底层",
                "【常用信号表】\n" +
                " 1) SIGHUP   : 终端断开挂起，常用于重载守护进程配置 (kill -1 <PID>)\n" +
                " 2) SIGINT   : 中断信号，键盘触发 Ctrl+C 终止当前前台进程\n" +
                " 3) SIGQUIT  : 退出信号，键盘触发 Ctrl+\\ 并生成 Core Dump 调试文件\n" +
                " 9) SIGKILL  : 强制终止信号，内核直接杀死且不可被应用捕获或忽略 (kill -9)\n" +
                "15) SIGTERM  : 友好终止信号，kill 默认信号，允许应用正常清理退出资源\n" +
                "18) SIGCONT  : 继续运行先前被挂起的进程 (bg/fg)\n" +
                "19) SIGSTOP  : 暂停进程运行，内核直接挂起且不可被忽略 (Ctrl+Z)\n" +
                "20) SIGTSTP  : 终端暂停信号，键盘触发 Ctrl+Z，可被应用捕获处理\n\n" +
                "【进程控制常用指令】\n" +
                "kill -l       : 列出系统支持的全部 64 个信号名称\n" +
                "kill -9 <PID> : 强杀顽固死锁或失去响应的进程\n" +
                "pkill -f <名> : 根据名称正则模糊批量终止进程\n" +
                "fuser -k <端口>/tcp : 强杀占用指定 TCP 端口的进程"
        ));

        ITEMS.add(new CheatItem(
                "Linux 文件权限与八进制掩码计算",
                "权限安全",
                "【权限位与字符定义】\n" +
                "r (Read 读)       = 4\n" +
                "w (Write 写)      = 2\n" +
                "x (Execute 执行)  = 1\n" +
                "- (无权限)        = 0\n\n" +
                "【常见权限组合】\n" +
                "777 : rwxrwxrwx (所有人拥有全部读写执行权限，极高安全风险)\n" +
                "755 : rwxr-xr-x (属主读写执行，组用户与其他人只读可执行，脚本/目录标配)\n" +
                "644 : rw-r--r-- (属主读写，组用户与其他人只读，普通配置文件与源码标配)\n" +
                "600 : rw------- (仅属主读写，SSH 私钥 ~/.ssh/id_rsa 严格强制权限)\n" +
                "700 : rwx------ (仅属主完全控制，SSH 目录 ~/.ssh 推荐权限)\n\n" +
                "【常用修改指令】\n" +
                "chmod +x script.sh       : 为当前脚本添加可执行权限\n" +
                "chmod 755 -R /path/to    : 递归设置目录及其下文件为 755\n" +
                "chown user:group file    : 变更文件的属主与属组"
        ));

        ITEMS.add(new CheatItem(
                "HTTP 状态码与响应语义速查",
                "网络协议",
                "【2xx 成功】\n" +
                "200 OK           : 请求成功并正常返回数据\n" +
                "201 Created      : 资源已成功创建 (常用于 POST 新建)\n" +
                "204 No Content   : 请求成功但无响应体返回 (常用于 DELETE)\n\n" +
                "【3xx 重定向】\n" +
                "301 Moved Permanently : 永久重定向，搜索引擎与客户端应更新缓存 URL\n" +
                "302 Found             : 临时重定向\n" +
                "304 Not Modified      : 资源未修改，客户端可直接使用本地协商缓存\n\n" +
                "【4xx 客户端错误】\n" +
                "400 Bad Request       : 请求语法错误或参数不合法\n" +
                "401 Unauthorized      : 未经过身份认证或凭据无效 (缺失 Bearer Token)\n" +
                "403 Forbidden         : 服务器理解请求但拒绝访问 (权限不足)\n" +
                "404 Not Found         : 请求的端点或资源路径不存在\n" +
                "405 Method Not Allowed: 请求方法不支持 (例如对只读 GET 端点发送 POST)\n" +
                "429 Too Many Requests : 请求频率超限 (触发限流)\n\n" +
                "【5xx 服务端错误】\n" +
                "500 Internal Error    : 服务端内部发生未捕获异常\n" +
                "502 Bad Gateway       : 网关或反向代理向上游服务器请求时收到无效响应\n" +
                "503 Service Unavailable: 服务器过载或正在进行维护，暂时无法提供服务\n" +
                "504 Gateway Timeout   : 网关等待上游服务响应超时"
        ));

        ITEMS.add(new CheatItem(
                "Android ADB 调试与底层命令速查",
                "安卓开发",
                "【常用 ADB 调试指令】\n" +
                "adb devices -l                   : 列出所有连接的设备及架构型号\n" +
                "adb tcpip 5555                   : 开启 5555 端口的无线 ADB 调试\n" +
                "adb connect <IP>:5555            : 无线连接到局域网设备\n" +
                "adb shell pm list packages -3    : 列出所有已安装的第三方用户应用\n" +
                "adb shell pm clear <包名>        : 清空指定应用的数据与缓存\n" +
                "adb shell am start -n <组件名>   : 启动指定的 Activity 界面\n" +
                "adb shell input keyevent 26      : 发送电源键事件 (点亮/息屏)\n" +
                "adb shell screencap -p /sdcard/s.png : 截取当前屏幕为图片\n" +
                "adb shell dumpsys battery        : 获取底层电芯温度与充电状态\n" +
                "adb shell dumpsys meminfo <包名> : 查看指定应用的 PSS/RSS 内存分布\n" +
                "adb logcat -v time *:E           : 持续抓取并过滤 Error 级别日志\n" +
                "adb push <本地> <远端>           : 上传文件至手机存储\n" +
                "adb pull <远端> <本地>           : 从手机拉取文件至电脑"
        ));
    }

    public static List<CheatItem> getItems() {
        return ITEMS;
    }
}
