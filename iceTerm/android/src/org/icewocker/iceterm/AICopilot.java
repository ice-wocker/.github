package org.icewocker.iceterm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AICopilot {

    // Premier coding-focused model routing
    public static final String DEFAULT_CODING_MODEL = "deepseek-coder";
    public static final String DEFAULT_ENDPOINT = "https://api.deepseek.com/v1";

    public interface Callback {
        void onResult(String command, String source, String error);
    }

    public static String matchOffline(String prompt) {
        if (prompt == null) return null;
        String p = prompt.trim();

        // 1. 查找大文件 (Find large files)
        Pattern p1 = Pattern.compile("(?:find|search|查找|搜索|列出|找出).*?(?:大于|>)\\s*(\\d+)\\s*(m|mb|g|gb)?", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(p);
        if (m1.find()) {
            String unit = m1.group(2) != null && m1.group(2).toLowerCase().startsWith("g") ? "G" : "M";
            return "find . -type f -size +" + m1.group(1) + unit + " -exec ls -lh {} +";
        }

        // 2. 按名称查找文件
        Pattern pName = Pattern.compile("(?:查找|搜索|找|find|search).*?名(?:字|称|为|包含)*\\s*([a-zA-Z0-9_\\-\\.\\*]+)");
        Matcher mName = pName.matcher(p);
        if (mName.find()) {
            return "find . -name '*" + mName.group(1) + "*'";
        }

        // 3. 端口探测与强杀
        Pattern p2 = Pattern.compile("(?:port|端口).*?(\\d{2,5})");
        Matcher m2 = p2.matcher(p);
        if (m2.find()) {
            if (p.contains("kill") || p.contains("杀") || p.contains("关闭") || p.contains("释放")) {
                return "fuser -k " + m2.group(1) + "/tcp";
            }
            return "lsof -i :" + m2.group(1) + " || fuser " + m2.group(1) + "/tcp";
        }

        // 4. 磁盘与目录空间
        if (p.contains("磁盘") || p.contains("disk") || p.contains("剩余空间") || p.contains("存储空间")) {
            return "df -h";
        }
        if (p.contains("目录大小") || p.contains("文件夹大小") || p.contains("占用排行") || p.contains("大文件")) {
            return "du -sh * | sort -hr | head -n 15";
        }

        // 5. 内存与 CPU
        if (p.contains("内存") || p.contains("memory") || p.contains("ram") || p.contains("运存")) {
            return "cat /proc/meminfo | head -n 12";
        }
        if (p.contains("cpu") || p.contains("处理器") || p.contains("系统负载") || p.contains("占用高")) {
            return "top -m 10 -d 2";
        }

        // 6. 进程强杀
        Pattern pKill = Pattern.compile("(?:杀死|杀掉|强杀|kill|pkill).*?进程\\s*([a-zA-Z0-9_\\-]+)");
        Matcher mKill = pKill.matcher(p);
        if (mKill.find()) {
            return "pkill -f '" + mKill.group(1) + "'";
        }

        // 7. 归档与解压缩
        if (p.contains(".tar.gz") || p.contains("tar.gz")) {
            Pattern pTar = Pattern.compile("([a-zA-Z0-9_\\-\\.]+\\.tar\\.gz)");
            Matcher mTar = pTar.matcher(p);
            if (mTar.find()) {
                if (p.contains("打包") || p.contains("压缩")) {
                    return "tar -zcvf " + mTar.group(1) + " .";
                }
                return "tar -zxvf " + mTar.group(1);
            }
        }
        if (p.contains(".zip") || p.contains("zip")) {
            Pattern pZip = Pattern.compile("([a-zA-Z0-9_\\-\\.]+\\.zip)");
            Matcher mZip = pZip.matcher(p);
            if (mZip.find()) {
                return "unzip " + mZip.group(1);
            }
        }

        // 8. 网络与 IP
        if (p.contains("ip") || p.contains("网络地址") || p.contains("内网地址") || p.contains("网卡")) {
            return "ip -br a || ifconfig";
        }
        if (p.contains("监听端口") || p.contains("开放端口") || p.contains("所有端口")) {
            return "ss -tulpn || netstat -tulpn";
        }
        if (p.contains("网络连接") || p.contains("连接数")) {
            return "netstat -nat | grep ESTABLISHED | wc -l";
        }

        // 9. Android 系统与幽灵进程
        if (p.contains("幽灵进程") || p.contains("phantom") || p.contains("32") || p.contains("限制")) {
            return "settings put global settings_enable_monitor_phantom_procs false";
        }
        if (p.contains("电池") || p.contains("电量") || p.contains("充电")) {
            return "dumpsys battery";
        }
        if (p.contains("日志") || p.contains("logcat") || p.contains("报错日志")) {
            return "logcat -d *:E | tail -n 50";
        }
        if (p.contains("第三方应用") || p.contains("已安装应用") || p.contains("安装包列表")) {
            return "pm list packages -3";
        }

        // 10. Git 常用操作
        if (p.contains("git") || p.contains("代码库") || p.contains("版本控制")) {
            if (p.contains("提交") || p.contains("commit")) {
                return "git add -A && git commit -m \"update\"";
            }
            if (p.contains("日志") || p.contains("历史") || p.contains("log")) {
                return "git log --oneline --graph -n 10";
            }
            if (p.contains("撤销") || p.contains("还原") || p.contains("丢弃")) {
                return "git restore . && git clean -fd";
            }
            return "git status -s";
        }

        // 11. 代码统计与搜索
        if (p.contains("行数") || p.contains("代码量") || p.contains("统计")) {
            return "find . -type f \\( -name '*.java' -o -name '*.py' -o -name '*.sh' -o -name '*.js' \\) -exec wc -l {} +";
        }

        return null;
    }

    public static void query(final String prompt, final String endpoint, final String apiKey, final String model, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 1. 优先命中本地 0 延迟离线正则引擎 (无需网络)
                String offlineCmd = matchOffline(prompt);
                if (offlineCmd != null) {
                    callback.onResult(offlineCmd, "离线规则引擎 [0延迟·免联网]", null);
                    return;
                }

                // 2. 路由至编程顶尖大模型
                String targetEndpoint = (endpoint != null && !endpoint.isEmpty()) ? endpoint : DEFAULT_ENDPOINT;
                String targetModel = (model != null && !model.isEmpty()) ? model : DEFAULT_CODING_MODEL;
                String targetKey = (apiKey != null) ? apiKey : "";

                HttpURLConnection conn = null;
                try {
                    URL url = new URL(targetEndpoint.replaceAll("/+$", "") + "/chat/completions");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Authorization", "Bearer " + targetKey);
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(15000);
                    conn.setDoOutput(true);

                    String systemPrompt = "你是由 iceTerm (冰端) 驱动的专业 Linux/Android 终端智能副驾驶。请将用户的自然语言需求直接转换为最高效、安全、标准的单行 Shell 命令。只输出包含在 ```bash ... ``` 块内的命令本身，不要附带任何闲聊或多余解释。";
                    String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", " ");
                    String jsonBody = "{"
                            + "\"model\":\"" + targetModel + "\","
                            + "\"temperature\":0.1,"
                            + "\"messages\":["
                            + "{\"role\":\"system\",\"content\":\"" + systemPrompt + "\"},"
                            + "{\"role\":\"user\",\"content\":\"" + escapedPrompt + "\"}"
                            + "]"
                            + "}";

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    int code = conn.getResponseCode();
                    if (code == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();

                        String body = sb.toString();
                        String content = extractContentFromResponse(body);
                        callback.onResult(content, "顶尖代码模型: " + targetModel, null);
                    } else {
                        callback.onResult(null, null, "模型请求失败 (HTTP " + code + ")，请检查 API Key 或网络");
                    }
                } catch (Exception e) {
                    callback.onResult(null, null, "模型连接异常: " + e.getMessage());
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }

    private static String extractContentFromResponse(String json) {
        Pattern pattern = Pattern.compile("\"content\"\\s*:\\s*\"([\\s\\S]*?)\"\\s*,?\\s*\"(?:role|function_call)");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            String raw = matcher.group(1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");
            Pattern codePat = Pattern.compile("```(?:bash|sh)?\\s*([\\s\\S]*?)\\s*```");
            Matcher cm = codePat.matcher(raw);
            if (cm.find()) return cm.group(1).trim();
            return raw.trim();
        }
        return json;
    }
}
