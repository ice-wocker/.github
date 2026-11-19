package org.icewocker.iceterm;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private TextView termOutput;
    private EditText termInput;
    private ScrollView scrollView;
    private LinearLayout tabContainer;
    private PowerManager.WakeLock wakeLock;

    private SessionManager sessionManager;
    private SharedPreferences prefs;

    private final List<String> history = new ArrayList<String>();
    private int historyIdx = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("iceterm_prefs", MODE_PRIVATE);

        termOutput = (TextView) findViewById(R.id.term_output);
        termInput = (EditText) findViewById(R.id.term_input);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        tabContainer = (LinearLayout) findViewById(R.id.tab_container);

        applySavedTheme();

        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "iceTerm:WakeLock");
                wakeLock.acquire(15 * 60 * 1000L);
            }
        } catch (Exception ignored) {}

        initSessionManager();
        initActionButtons();
        initExtraKeys();
        initInputHandler();
    }

    private void applySavedTheme() {
        int textSize = prefs.getInt("term_text_size", 12);
        termOutput.setTextSize(textSize);

        String theme = prefs.getString("term_theme", "github_dark");
        if ("hacker_green".equals(theme)) {
            termOutput.setTextColor(Color.rgb(63, 185, 80));
            scrollView.setBackgroundColor(Color.rgb(5, 10, 5));
        } else if ("monokai".equals(theme)) {
            termOutput.setTextColor(Color.rgb(248, 248, 242));
            scrollView.setBackgroundColor(Color.rgb(39, 40, 34));
        } else {
            // Default: GitHub Dark
            termOutput.setTextColor(Color.rgb(230, 237, 243));
            scrollView.setBackgroundColor(Color.rgb(9, 13, 19));
        }
    }

    private void initSessionManager() {
        sessionManager = new SessionManager(new SessionManager.SessionChangeListener() {
            @Override
            public void onTabSwitched(final SessionManager.TabSession active) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderTabs();
                        if (active != null) {
                            termOutput.setText(active.buffer.render());
                            scrollToBottom();
                        }
                    }
                });
            }

            @Override
            public void onTabAdded(SessionManager.TabSession tab) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderTabs();
                    }
                });
            }

            @Override
            public void onTabRemoved(int tabId) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        renderTabs();
                    }
                });
            }
        });

        // Create initial session
        addNewTab("Shell 1");
    }

    private void addNewTab(String title) {
        final SessionManager.TabSession[] tabHolder = new SessionManager.TabSession[1];
        tabHolder[0] = sessionManager.createSession(title, new ShellSession.OutputListener() {
            @Override
            public void onOutput(final String text) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tabHolder[0].buffer.append(text);
                        SessionManager.TabSession active = sessionManager.getActiveSession();
                        if (active != null && active.id == tabHolder[0].id) {
                            termOutput.setText(active.buffer.render());
                            scrollToBottom();
                        }
                    }
                });
            }

            @Override
            public void onProcessExit(final int exitCode) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tabHolder[0].buffer.append("\n[进程已退出，状态码: " + exitCode + "]\n");
                        SessionManager.TabSession active = sessionManager.getActiveSession();
                        if (active != null && active.id == tabHolder[0].id) {
                            termOutput.setText(active.buffer.render());
                        }
                    }
                });
            }
        });

        tabHolder[0].buffer.append(getString(R.string.welcome_msg));
    }

    private void renderTabs() {
        tabContainer.removeAllViews();
        List<SessionManager.TabSession> sessions = sessionManager.getSessions();
        int activeIdx = sessionManager.getActiveIndex();

        for (int i = 0; i < sessions.size(); i++) {
            final int idx = i;
            final SessionManager.TabSession s = sessions.get(i);

            Button btn = new Button(this);
            btn.setText((i + 1) + ":" + s.title);
            btn.setTextSize(11);
            btn.setMinHeight(0);
            btn.setMinimumHeight(0);
            btn.setPadding(16, 4, 16, 4);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.setMargins(4, 0, 4, 0);
            btn.setLayoutParams(lp);

            if (i == activeIdx) {
                btn.setBackgroundColor(Color.parseColor("#1F6FEB"));
                btn.setTextColor(Color.WHITE);
            } else {
                btn.setBackgroundColor(Color.parseColor("#21262D"));
                btn.setTextColor(Color.parseColor("#C9D1D9"));
            }

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sessionManager.switchToTab(idx);
                }
            });

            btn.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (sessionManager.getSessions().size() > 1) {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("关闭标签页")
                                .setMessage("确认关闭会话 [" + s.title + "] 吗？")
                                .setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        sessionManager.closeSession(idx);
                                    }
                                })
                                .setNegativeButton("取消", null)
                                .show();
                    } else {
                        Toast.makeText(MainActivity.this, "至少保留一个活动标签页", Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });

            tabContainer.addView(btn);
        }
    }

    private void initActionButtons() {
        findViewById(R.id.btn_add_tab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int count = sessionManager.getSessions().size() + 1;
                addNewTab("Shell " + count);
            }
        });

        findViewById(R.id.btn_clear).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SessionManager.TabSession active = sessionManager.getActiveSession();
                if (active != null) {
                    active.buffer.clear();
                    termOutput.setText("");
                }
            }
        });

        findViewById(R.id.btn_toolbox).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showToolboxDialog();
            }
        });

        findViewById(R.id.btn_radar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRadarDialog();
            }
        });

        findViewById(R.id.btn_status).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSysInfoDialog();
            }
        });

        findViewById(R.id.btn_ai).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAIDialog();
            }
        });

        findViewById(R.id.btn_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });
    }

    private void initExtraKeys() {
        findViewById(R.id.btn_key_ctrl_c).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToActiveSession("\u0003");
            }
        });

        findViewById(R.id.btn_key_ctrl_d).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                writeToActiveSession("\u0004");
            }
        });

        findViewById(R.id.btn_key_tab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("  ");
            }
        });

        findViewById(R.id.btn_key_esc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                termInput.setText("");
            }
        });

        findViewById(R.id.btn_key_pipe).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("|");
            }
        });

        findViewById(R.id.btn_key_amp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("&");
            }
        });

        findViewById(R.id.btn_key_tilde).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("~");
            }
        });

        findViewById(R.id.btn_key_slash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("/");
            }
        });

        findViewById(R.id.btn_key_dash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("-");
            }
        });

        findViewById(R.id.btn_key_gt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText(">");
            }
        });

        findViewById(R.id.btn_key_quote).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insertText("\"");
            }
        });

        findViewById(R.id.btn_key_up).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!history.isEmpty()) {
                    if (historyIdx == -1) historyIdx = history.size() - 1;
                    else if (historyIdx > 0) historyIdx--;
                    termInput.setText(history.get(historyIdx));
                    termInput.setSelection(termInput.getText().length());
                }
            }
        });

        findViewById(R.id.btn_key_down).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (historyIdx >= 0 && historyIdx < history.size() - 1) {
                    historyIdx++;
                    termInput.setText(history.get(historyIdx));
                    termInput.setSelection(termInput.getText().length());
                } else {
                    historyIdx = -1;
                    termInput.setText("");
                }
            }
        });
    }

    private void initInputHandler() {
        findViewById(R.id.btn_send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitCommand();
            }
        });

        termInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    submitCommand();
                    return true;
                }
                return false;
            }
        });
    }

    private void submitCommand() {
        String cmd = termInput.getText().toString().trim();
        if (cmd.isEmpty()) return;

        termInput.setText("");
        history.add(cmd);
        historyIdx = -1;

        String blockReason = ShellSession.checkSafety(cmd);
        if (blockReason != null) {
            SessionManager.TabSession active = sessionManager.getActiveSession();
            if (active != null) {
                active.buffer.append("\n" + blockReason + "\n");
                termOutput.setText(active.buffer.render());
            }
            return;
        }

        SessionManager.TabSession active = sessionManager.getActiveSession();
        if (active != null) {
            active.buffer.append("$ " + cmd + "\n");
            termOutput.setText(active.buffer.render());
            active.shell.write(cmd + "\n");
            scrollToBottom();
        }
    }

    private void writeToActiveSession(String str) {
        SessionManager.TabSession active = sessionManager.getActiveSession();
        if (active != null) {
            active.shell.write(str);
        }
    }

    private void insertText(String str) {
        int start = Math.max(termInput.getSelectionStart(), 0);
        int end = Math.max(termInput.getSelectionEnd(), 0);
        termInput.getText().replace(Math.min(start, end), Math.max(start, end), str, 0, str.length());
    }

    private void scrollToBottom() {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    // =========================================================================
    // Dialogs: Toolbox, Radar, SysInfo, AI, Settings
    // =========================================================================

    private void showToolboxDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(16, 16, 16, 16);

        final Spinner spinner = new Spinner(this);
        List<String> categories = new ArrayList<String>();
        categories.add("全部");
        categories.addAll(CommandToolbox.getCategories());
        ArrayAdapter<String> catAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinner.setAdapter(catAdapter);
        layout.addView(spinner);

        final ListView listView = new ListView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 800);
        listView.setLayoutParams(lp);
        layout.addView(listView);

        final List<CommandToolbox.CommandItem>[] currentItems = new List[]{CommandToolbox.getItems()};
        final ArrayAdapter<String> listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_2, android.R.id.text1, new ArrayList<String>()) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                CommandToolbox.CommandItem item = currentItems[0].get(position);
                text1.setText(item.name + " (" + item.category + ")");
                text1.setTextColor(Color.parseColor("#58A6FF"));
                text2.setText(item.command + "\n" + item.description);
                text2.setTextColor(Color.parseColor("#8B949E"));
                return view;
            }

            @Override
            public int getCount() {
                return currentItems[0].size();
            }
        };
        listView.setAdapter(listAdapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String cat = (String) spinner.getSelectedItem();
                currentItems[0] = CommandToolbox.filterByCategory(cat);
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("常用运维与开发工具箱 (150+ 指令)")
                .setView(layout)
                .setNegativeButton("关闭", null)
                .create();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CommandToolbox.CommandItem item = currentItems[0].get(position);
                termInput.setText(item.command);
                termInput.setSelection(item.command.length());
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showRadarDialog() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<String> ips = PortRadar.getLocalIpAddresses();
                List<PortRadar.PortEntry> ports = PortRadar.scanListeningPorts();

                StringBuilder sb = new StringBuilder();
                sb.append("【本地网络接口 IP】\n");
                for (String ip : ips) sb.append("  • ").append(ip).append("\n");
                sb.append("\n【活动 TCP 监听端口与进程】\n");
                if (ports.isEmpty()) {
                    sb.append("  (未探测到监听端口)\n");
                } else {
                    for (PortRadar.PortEntry pe : ports) {
                        sb.append("  • ").append(pe.proto).append(" ").append(pe.ip).append(":").append(pe.port);
                        if (pe.pid > 0) {
                            sb.append(" [PID: ").append(pe.pid).append(" | ").append(pe.processName).append("]");
                        }
                        sb.append("\n");
                    }
                }

                final String msg = sb.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("网络与端口感知雷达")
                                .setMessage(msg)
                                .setPositiveButton("关闭", null)
                                .show();
                    }
                });
            }
        }).start();
    }

    private void showSysInfoDialog() {
        String report = SystemMonitor.getFullReport(this);
        new AlertDialog.Builder(this)
                .setTitle("系统健康与硬件指标监控")
                .setMessage(report)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void showAIDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText input = new EditText(this);
        input.setHint("输入自然语言需求 (例如: 查找大于100MB的视频)");
        input.setSingleLine(false);
        input.setLines(2);
        layout.addView(input);

        final TextView hint = new TextView(this);
        String currentModel = prefs.getString("ai_model", AICopilot.DEFAULT_CODING_MODEL);
        hint.setText("当前代码路由模型: " + currentModel + " (支持离线规则毫秒级匹配)");
        hint.setTextSize(11);
        hint.setTextColor(Color.parseColor("#8B949E"));
        hint.setPadding(0, 10, 0, 0);
        layout.addView(hint);

        new AlertDialog.Builder(this)
                .setTitle("AI 终端副驾驶 (顶尖代码模型)")
                .setView(layout)
                .setPositiveButton("生成命令", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String query = input.getText().toString().trim();
                        if (query.isEmpty()) return;

                        Toast.makeText(MainActivity.this, "正在推导最优 Shell 命令...", Toast.LENGTH_SHORT).show();

                        String ep = prefs.getString("ai_endpoint", AICopilot.DEFAULT_ENDPOINT);
                        String key = prefs.getString("ai_key", "");
                        String model = prefs.getString("ai_model", AICopilot.DEFAULT_CODING_MODEL);

                        AICopilot.query(query, ep, key, model, new AICopilot.Callback() {
                            @Override
                            public void onResult(final String command, final String source, final String error) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (error != null) {
                                            Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                                        } else if (command != null) {
                                            termInput.setText(command);
                                            termInput.setSelection(command.length());
                                            Toast.makeText(MainActivity.this, "生成自: " + source, Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }
                        });
                    }
                })
                .setNeutralButton("模型配置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showAIConfigDialog();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showAIConfigDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText etEndpoint = new EditText(this);
        etEndpoint.setHint("API Endpoint (默认: https://api.deepseek.com/v1)");
        etEndpoint.setText(prefs.getString("ai_endpoint", AICopilot.DEFAULT_ENDPOINT));
        layout.addView(etEndpoint);

        final EditText etKey = new EditText(this);
        etKey.setHint("API Key (sk-...)");
        etKey.setText(prefs.getString("ai_key", ""));
        layout.addView(etKey);

        final EditText etModel = new EditText(this);
        etModel.setHint("模型名称 (如: deepseek-coder, qwen2.5-coder-32b)");
        etModel.setText(prefs.getString("ai_model", AICopilot.DEFAULT_CODING_MODEL));
        layout.addView(etModel);

        new AlertDialog.Builder(this)
                .setTitle("AI 大模型服务配置")
                .setView(layout)
                .setPositiveButton("保存配置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit()
                                .putString("ai_endpoint", etEndpoint.getText().toString().trim())
                                .putString("ai_key", etKey.getText().toString().trim())
                                .putString("ai_model", etModel.getText().toString().trim())
                                .apply();
                        Toast.makeText(MainActivity.this, "AI 模型配置已更新", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showSettingsDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        TextView tvFont = new TextView(this);
        tvFont.setText("终端字体大小:");
        layout.addView(tvFont);

        final Spinner spFont = new Spinner(this);
        String[] fontSizes = new String[]{"11 sp", "12 sp (默认)", "14 sp", "16 sp", "18 sp"};
        spFont.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, fontSizes));
        spFont.setSelection(1);
        layout.addView(spFont);

        TextView tvTheme = new TextView(this);
        tvTheme.setText("\n终端配色方案:");
        layout.addView(tvTheme);

        final Spinner spTheme = new Spinner(this);
        String[] themes = new String[]{"GitHub Dark (极客黑)", "Hacker Green (黑客绿)", "Monokai (经典复古)"};
        spTheme.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, themes));
        layout.addView(spTheme);

        new AlertDialog.Builder(this)
                .setTitle("终端与外观设置")
                .setView(layout)
                .setPositiveButton("应用", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int[] sizes = new int[]{11, 12, 14, 16, 18};
                        int selectedSize = sizes[spFont.getSelectedItemPosition()];
                        String[] themeKeys = new String[]{"github_dark", "hacker_green", "monokai"};
                        String selectedTheme = themeKeys[spTheme.getSelectedItemPosition()];

                        prefs.edit()
                                .putInt("term_text_size", selectedSize)
                                .putString("term_theme", selectedTheme)
                                .apply();

                        applySavedTheme();
                        Toast.makeText(MainActivity.this, "设置已保存并生效", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (sessionManager != null) {
            sessionManager.destroyAll();
        }
    }
}
