package org.icewocker.iceterm;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class ShellSession {

    public interface OutputListener {
        void onOutput(String text);
        void onProcessExit(int exitCode);
    }

    private Process process;
    private OutputStream stdin;
    private BufferedReader stdout;
    private OutputListener listener;
    private boolean isRunning = false;

    private static final Pattern[] DANGEROUS_PATTERNS = new Pattern[]{
            Pattern.compile("\\brm\\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|--recursive\\s+--force)\\s+/\\s*$"),
            Pattern.compile("\\brm\\s+(-[a-zA-Z]*r[a-zA-Z]*f|[a-zA-Z]*-fr|--recursive\\s+--force)\\s+/\\*$"),
            Pattern.compile("\\bmkfs(\\.[a-zA-Z0-9]+)?\\s+/dev/"),
            Pattern.compile("\\bdd\\s+.*of=/dev/(sd[a-z]|nvme[0-9]n[0-9]|mmcblk[0-9])"),
            Pattern.compile(":\\(\\)\\s*\\{\\s*:\\s*\\|\\s*:\\s*&\\s*\\}\\s*;\\s*:"),
            Pattern.compile("\\bchmod\\s+(-[a-zA-Z]*R)?\\s*777\\s+/\\s*$")
    };

    public ShellSession(OutputListener listener) {
        this.listener = listener;
    }

    public synchronized void start() {
        if (isRunning) return;
        try {
            String shell = new File("/system/bin/sh").exists() ? "/system/bin/sh" : "/bin/sh";
            ProcessBuilder pb = new ProcessBuilder(shell);
            pb.redirectErrorStream(true);
            pb.environment().put("TERM", "xterm-256color");
            pb.environment().put("PATH", "/system/bin:/system/xbin:" + System.getenv("PATH"));

            process = pb.start();
            stdin = process.getOutputStream();
            stdout = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
            isRunning = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    char[] buffer = new char[1024];
                    int read;
                    try {
                        while (isRunning && (read = stdout.read(buffer, 0, buffer.length)) != -1) {
                            String chunk = new String(buffer, 0, read);
                            if (listener != null) listener.onOutput(chunk);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        int exit = 0;
                        if (process != null) {
                            try { exit = process.waitFor(); } catch (Exception ignored) {}
                        }
                        isRunning = false;
                        if (listener != null) listener.onProcessExit(exit);
                    }
                }
            }).start();

        } catch (Exception e) {
            if (listener != null) {
                listener.onOutput("[ERROR] Failed to spawn shell: " + e.getMessage() + "\n");
            }
        }
    }

    public static String checkSafety(String cmd) {
        if (cmd == null) return null;
        String trimmed = cmd.trim();
        for (Pattern p : DANGEROUS_PATTERNS) {
            if (p.matcher(trimmed).find()) {
                return "[BLOCKED] High-risk destructive command intercepted: " + trimmed;
            }
        }
        return null;
    }

    public synchronized void write(String cmd) {
        if (!isRunning || stdin == null) {
            start();
        }
        try {
            stdin.write(cmd.getBytes(StandardCharsets.UTF_8));
            stdin.flush();
        } catch (Exception e) {
            if (listener != null) listener.onOutput("[ERROR] Write failed: " + e.getMessage() + "\n");
        }
    }

    public synchronized void destroy() {
        isRunning = false;
        if (process != null) {
            process.destroy();
            process = null;
        }
    }
}
