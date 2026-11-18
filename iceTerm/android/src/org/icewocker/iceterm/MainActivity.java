package org.icewocker.iceterm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView termOutput;
    private EditText termInput;
    private ScrollView scrollView;
    private ShellSession shell;
    private PowerManager.WakeLock wakeLock;

    private final List<String> history = new ArrayList<>();
    private int historyIdx = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        termOutput = findViewById(R.id.term_output);
        termInput = findViewById(R.id.term_input);
        scrollView = findViewById(R.id.scroll_view);

        // Keep CPU awake while terminal is active
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "iceTerm:WakeLock");
                wakeLock.acquire(10 * 60 * 1000L); // 10 minutes default
            }
        } catch (Exception ignored) {}

        // Setup Shell Session
        shell = new ShellSession(new ShellSession.OutputListener() {
            @Override
            public void onOutput(final String text) {
                runOnUiThread(() -> {
                    termOutput.append(text);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
            }

            @Override
            public void onProcessExit(final int exitCode) {
                runOnUiThread(() -> {
                    termOutput.append("\n[Process exited with code " + exitCode + "]\n");
                });
            }
        });
        shell.start();
        termOutput.setText(getString(R.string.welcome_msg));

        initActionButtons();
        initExtraKeys();
        initInputHandler();
    }

    private void initActionButtons() {
        findViewById(R.id.btn_clear).setOnClickListener(v -> termOutput.setText(""));

        findViewById(R.id.btn_radar).setOnClickListener(v -> showRadarDialog());

        findViewById(R.id.btn_status).setOnClickListener(v -> showSysInfoDialog());

        findViewById(R.id.btn_ai).setOnClickListener(v -> showAIDialog());
    }

    private void initExtraKeys() {
        findViewById(R.id.btn_key_ctrl_c).setOnClickListener(v -> {
            if (shell != null) shell.write("\u0003");
        });

        findViewById(R.id.btn_key_tab).setOnClickListener(v -> insertText("  "));

        findViewById(R.id.btn_key_esc).setOnClickListener(v -> termInput.setText(""));

        findViewById(R.id.btn_key_pipe).setOnClickListener(v -> insertText("|"));

        findViewById(R.id.btn_key_tilde).setOnClickListener(v -> insertText("~"));

        findViewById(R.id.btn_key_slash).setOnClickListener(v -> insertText("/"));

        findViewById(R.id.btn_key_dash).setOnClickListener(v -> insertText("-"));

        findViewById(R.id.btn_key_up).setOnClickListener(v -> {
            if (!history.isEmpty()) {
                if (historyIdx == -1) historyIdx = history.size() - 1;
                else if (historyIdx > 0) historyIdx--;
                termInput.setText(history.get(historyIdx));
                termInput.setSelection(termInput.getText().length());
            }
        });

        findViewById(R.id.btn_key_down).setOnClickListener(v -> {
            if (historyIdx >= 0 && historyIdx < history.size() - 1) {
                historyIdx++;
                termInput.setText(history.get(historyIdx));
                termInput.setSelection(termInput.getText().length());
            } else {
                historyIdx = -1;
                termInput.setText("");
            }
        });
    }

    private void initInputHandler() {
        findViewById(R.id.btn_send).setOnClickListener(v -> submitCommand());

        termInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                submitCommand();
                return true;
            }
            return false;
        });
    }

    private void submitCommand() {
        String cmd = termInput.getText().toString().trim();
        if (cmd.isEmpty()) return;

        termInput.setText("");
        history.add(cmd);
        historyIdx = -1;

        // Safety verification
        String blockReason = ShellSession.checkSafety(cmd);
        if (blockReason != null) {
            termOutput.append("\n" + blockReason + "\n");
            return;
        }

        termOutput.append("$ " + cmd + "\n");
        if (shell != null) {
            shell.write(cmd + "\n");
        }
    }

    private void insertText(String str) {
        int start = Math.max(termInput.getSelectionStart(), 0);
        int end = Math.max(termInput.getSelectionEnd(), 0);
        termInput.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

    private void showRadarDialog() {
        new Thread(() -> {
            List<String> ips = PortRadar.getLocalIpAddresses();
            List<PortRadar.PortEntry> ports = PortRadar.scanListeningPorts();

            StringBuilder sb = new StringBuilder();
            sb.append("Host Network Interfaces:\n");
            for (String ip : ips) sb.append("  ").append(ip).append("\n");
            sb.append("\nActive Listening TCP Ports:\n");
            if (ports.isEmpty()) {
                sb.append("  (No listening ports detected in /proc/net/tcp)\n");
            } else {
                for (PortRadar.PortEntry pe : ports) {
                    sb.append("  ").append(pe.ip).append(":").append(pe.port)
                            .append(pe.isLoopback ? " [Local Only]" : " [Exposed]")
                            .append("\n");
                }
            }

            final String msg = sb.toString();
            runOnUiThread(() -> {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Network & Port Radar")
                        .setMessage(msg)
                        .setPositiveButton("Close", null)
                        .show();
            });
        }).start();
    }

    private void showSysInfoDialog() {
        StringBuilder sb = new StringBuilder();
        sb.append("OS Version: Android ").append(android.os.Build.VERSION.RELEASE)
                .append(" (API ").append(android.os.Build.VERSION.SDK_INT).append(")\n");
        sb.append("Device: ").append(android.os.Build.MANUFACTURER).append(" ")
                .append(android.os.Build.MODEL).append("\n");
        sb.append("Architecture: ").append(System.getProperty("os.arch")).append("\n\n");

        // Battery level
        try {
            BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            if (bm != null) {
                int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
                sb.append("Battery: ").append(level).append("%\n");
            }
        } catch (Exception ignored) {}

        // Memory info
        long freeMem = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long totalMem = Runtime.getRuntime().totalMemory() / 1024 / 1024;
        sb.append("JVM Heap: ").append(totalMem - freeMem).append("MB / ").append(totalMem).append("MB\n");

        new AlertDialog.Builder(this)
                .setTitle("System Metrics")
                .setMessage(sb.toString())
                .setPositiveButton("Close", null)
                .show();
    }

    private void showAIDialog() {
        final EditText input = new EditText(this);
        input.setHint("Query in natural language (e.g. find files > 100MB)");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("AI Copilot (Routed Coding Model)")
                .setView(input)
                .setPositiveButton("Generate", (dialog, which) -> {
                    String query = input.getText().toString().trim();
                    if (query.isEmpty()) return;

                    Toast.makeText(MainActivity.this, "Routing to coding model...", Toast.LENGTH_SHORT).show();
                    AICopilot.query(query, null, null, null, (command, source, error) -> {
                        runOnUiThread(() -> {
                            if (error != null) {
                                Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                            } else if (command != null) {
                                termInput.setText(command);
                                termInput.setSelection(command.length());
                                Toast.makeText(MainActivity.this, "Generated via " + source, Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (shell != null) {
            shell.destroy();
        }
    }
}
