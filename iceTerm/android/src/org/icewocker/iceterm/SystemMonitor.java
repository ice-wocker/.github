package org.icewocker.iceterm;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class SystemMonitor {

    public static String getFullReport(Context ctx) {
        StringBuilder sb = new StringBuilder();

        // 1. 系统与设备核心指标 (System & Hardware)
        sb.append("【设备与系统核心指标】\n");
        sb.append("设备型号: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append(" (").append(Build.DEVICE).append(")\n");
        sb.append("系统版本: Android ").append(Build.VERSION.RELEASE).append(" (SDK API ").append(Build.VERSION.SDK_INT).append(")\n");
        sb.append("CPU 架构: ").append(System.getProperty("os.arch")).append(" (").append(Runtime.getRuntime().availableProcessors()).append(" 核心)\n");

        // 内核版本
        String kernel = readSingleLine("/proc/version");
        if (kernel != null) {
            sb.append("内核版本: ").append(kernel.split("\\s+")[2]).append("\n");
        }

        // 开机运行时间
        String uptime = readSingleLine("/proc/uptime");
        if (uptime != null) {
            try {
                long secs = (long) Double.parseDouble(uptime.split("\\s+")[0]);
                long h = secs / 3600;
                long m = (secs % 3600) / 60;
                sb.append("持续运行: ").append(h).append(" 小时 ").append(m).append(" 分钟\n");
            } catch (Exception ignored) {}
        }
        sb.append("\n");

        // 2. 内存与存储指标 (Memory & RAM)
        sb.append("【物理内存与虚拟内存】\n");
        long totalRam = 0, availRam = 0, cachedRam = 0, swapTotal = 0, swapFree = 0;
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/meminfo"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("MemTotal:")) totalRam = parseKb(line);
                else if (line.startsWith("MemAvailable:")) availRam = parseKb(line);
                else if (line.startsWith("Cached:")) cachedRam = parseKb(line);
                else if (line.startsWith("SwapTotal:")) swapTotal = parseKb(line);
                else if (line.startsWith("SwapFree:")) swapFree = parseKb(line);
            }
        } catch (Exception ignored) {}

        if (totalRam > 0) {
            long usedRam = totalRam - availRam;
            int pct = (int) ((usedRam * 100) / totalRam);
            sb.append("物理内存: ").append(usedRam).append(" MB / ").append(totalRam).append(" MB (使用率: ").append(pct).append("%)\n");
            sb.append("系统缓存: ").append(cachedRam).append(" MB\n");
            if (swapTotal > 0) {
                sb.append("ZRAM/Swap: ").append(swapTotal - swapFree).append(" MB / ").append(swapTotal).append(" MB\n");
            }
        }
        sb.append("\n");

        // 3. 电池与温度指标 (Battery & Thermal)
        sb.append("【电池与温度指标】\n");
        try {
            BatteryManager bm = (BatteryManager) ctx.getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                sb.append("当前电量: ").append(level).append("%\n");
            }
        } catch (Exception ignored) {}

        // 从 sysfs 探测电池与 CPU 真实温度
        String battTemp = readSingleLine("/sys/class/power_supply/battery/temp");
        if (battTemp != null) {
            try {
                double t = Double.parseDouble(battTemp);
                if (t > 100) t /= 10.0;
                sb.append("电池温度: ").append(String.format("%.1f", t)).append(" °C\n");
            } catch (Exception ignored) {}
        }

        // CPU 温度探测
        double cpuTemp = getCpuTemp();
        if (cpuTemp > 0) {
            sb.append("CPU 核心温度: ").append(String.format("%.1f", cpuTemp)).append(" °C\n");
        }
        sb.append("\n");

        // 4. Android 12+ 幽灵进程守卫 (Phantom Process Killer)
        sb.append("【Android 幽灵进程限制状态】\n");
        if (Build.VERSION.SDK_INT >= 31) {
            sb.append("检测到当前为 Android 12+ 设备。\n");
            sb.append("系统机制: 单个应用子进程超过 32 个将被强制 SIGKILL 杀后台。\n");
            sb.append("解除方案 (在电脑 ADB 或手机端 Shizuku/rish 执行):\n");
            sb.append("  settings put global settings_enable_monitor_phantom_procs false\n");
        } else {
            sb.append("当前系统版本低于 Android 12，不受幽灵进程 32 上限影响。\n");
        }

        return sb.toString();
    }

    private static double getCpuTemp() {
        File dir = new File("/sys/class/thermal");
        if (dir.exists()) {
            File[] zones = dir.listFiles();
            if (zones != null) {
                for (File z : zones) {
                    if (z.getName().startsWith("thermal_zone")) {
                        String t = readSingleLine(z.getAbsolutePath() + "/temp");
                        if (t != null) {
                            try {
                                double val = Double.parseDouble(t);
                                if (val > 1000) val /= 1000.0;
                                if (val >= 15 && val <= 105) return val;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
        return -1;
    }

    private static String readSingleLine(String path) {
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            return br.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private static long parseKb(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length >= 2) {
            try {
                return Long.parseLong(parts[1]) / 1024;
            } catch (Exception ignored) {}
        }
        return 0;
    }
}
