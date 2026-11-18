package org.icewocker.iceterm;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PortRadar {

    public static class PortEntry {
        public final String ip;
        public final int port;
        public final boolean isLoopback;

        public PortEntry(String ip, int port, boolean isLoopback) {
            this.ip = ip;
            this.port = port;
            this.isLoopback = isLoopback;
        }
    }

    public static List<String> getLocalIpAddresses() {
        List<String> list = new ArrayList<>();
        list.add("127.0.0.1");
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    NetworkInterface nif = interfaces.nextElement();
                    if (nif.isLoopback() || !nif.isUp()) continue;
                    Enumeration<InetAddress> addrs = nif.getInetAddresses();
                    while (addrs.hasMoreElements()) {
                        InetAddress addr = addrs.nextElement();
                        String host = addr.getHostAddress();
                        if (host != null && host.indexOf(':') < 0 && !host.startsWith("127.")) {
                            list.add(host);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return list;
    }

    public static List<PortEntry> scanListeningPorts() {
        List<PortEntry> results = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        String[] procPaths = new String[]{"/proc/net/tcp", "/proc/net/tcp6"};

        for (String p : procPaths) {
            File f = new File(p);
            if (!f.exists() || !f.canRead()) continue;
            try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
                String line = reader.readLine(); // skip header
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 4) {
                        String local = parts[1];
                        String state = parts[3];
                        // 0A == TCP_LISTEN
                        if ("0A".equalsIgnoreCase(state)) {
                            String[] hp = local.split(":");
                            if (hp.length == 2) {
                                int port = Integer.parseInt(hp[1], 16);
                                if (!seen.contains(port)) {
                                    seen.add(port);
                                    String ip = parseIp(hp[0]);
                                    boolean loopback = "127.0.0.1".equals(ip) || "0.0.0.0".equals(ip);
                                    results.add(new PortEntry(ip, port, loopback));
                                }
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        Collections.sort(results, (a, b) -> Integer.compare(a.port, b.port));
        return results;
    }

    private static String parseIp(String hex) {
        if (hex.length() == 8) {
            try {
                long num = Long.parseLong(hex, 16);
                long b1 = num & 0xFF;
                long b2 = (num >> 8) & 0xFF;
                long b3 = (num >> 16) & 0xFF;
                long b4 = (num >> 24) & 0xFF;
                return b1 + "." + b2 + "." + b3 + "." + b4;
            } catch (Exception ignored) {}
        }
        return "0.0.0.0";
    }
}
