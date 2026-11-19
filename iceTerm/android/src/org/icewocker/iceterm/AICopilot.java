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

    public static final String DEFAULT_CODING_MODEL = "deepseek-coder";
    public static final String DEFAULT_ENDPOINT = "https://api.deepseek.com/v1";

    public interface Callback {
        void onResult(String command, String source, String error);
    }

    public static String matchOffline(String prompt) {
        if (prompt == null) return null;
        String p = prompt.trim();

        // 1. Find large files
        Pattern p1 = Pattern.compile("(?:find|search|查找|搜索).*?(?:大于|>)\\s*(\\d+)\\s*(m|mb|g|gb)?", Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(p);
        if (m1.find()) {
            String unit = m1.group(2) != null && m1.group(2).toLowerCase().startsWith("g") ? "G" : "M";
            return "find . -type f -size +" + m1.group(1) + unit + " -exec ls -lh {} +";
        }

        // 2. Port lookup
        Pattern p2 = Pattern.compile("(?:port|端口).*?(\\d{2,5})");
        Matcher m2 = p2.matcher(p);
        if (m2.find()) {
            if (p.contains("kill") || p.contains("杀") || p.contains("关闭")) {
                return "fuser -k " + m2.group(1) + "/tcp";
            }
            return "lsof -i :" + m2.group(1) + " || fuser " + m2.group(1) + "/tcp";
        }

        // 3. Disk & Memory
        if (p.contains("磁盘") || p.contains("disk") || p.contains("剩余空间")) {
            return "df -h";
        }
        if (p.contains("内存") || p.contains("memory") || p.contains("ram")) {
            return "free -m || cat /proc/meminfo | head -n 5";
        }
        if (p.contains("cpu") || p.contains("进程")) {
            return "top -n 1 -m 10 || ps -ef | head -n 15";
        }

        // 4. Archives
        Pattern p3 = Pattern.compile("(?:解压|extract|unzip|tar)\\s+([a-zA-Z0-9_\\-\\.]+\\.tar\\.gz)");
        Matcher m3 = p3.matcher(p);
        if (m3.find()) {
            return "tar -zxvf " + m3.group(1);
        }

        // 5. Android Phantom Process
        if (p.contains("幽灵进程") || p.contains("phantom")) {
            return "settings put global settings_enable_monitor_phantom_procs false";
        }

        // 6. Network IP
        if (p.contains("ip") || p.contains("网络地址")) {
            return "ip -br a || ifconfig";
        }

        return null;
    }

    public static void query(final String prompt, final String endpoint, final String apiKey, final String model, final Callback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String offlineCmd = matchOffline(prompt);
                if (offlineCmd != null) {
                    callback.onResult(offlineCmd, "Offline Engine (0-Latency)", null);
                    return;
                }

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

                    String systemPrompt = "You are iceTerm Code Assistant. Convert user natural language query into the most accurate, secure, production-grade Shell command on Linux/Android. Return ONLY the shell command inside markdown code block ```bash ... ``` without markdown explanation.";
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
                        callback.onResult(content, "Routed Model: " + targetModel, null);
                    } else {
                        callback.onResult(null, null, "HTTP " + code + ": Failed to query " + targetModel);
                    }
                } catch (Exception e) {
                    callback.onResult(null, null, "Inference error: " + e.getMessage());
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
