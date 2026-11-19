package org.icewocker.iceterm;

import android.text.SpannableStringBuilder;
import java.util.LinkedList;

public class TerminalBuffer {

    private final int maxLines;
    private final LinkedList<CharSequence> lines = new LinkedList<CharSequence>();

    public TerminalBuffer(int maxLines) {
        this.maxLines = Math.max(200, maxLines);
    }

    public synchronized void append(CharSequence text) {
        if (text == null || text.length() == 0) return;

        String[] split = text.toString().split("\n", -1);
        for (int i = 0; i < split.length; i++) {
            CharSequence parsed = AnsiParser.parse(split[i]);
            if (i == 0 && !lines.isEmpty()) {
                CharSequence last = lines.removeLast();
                SpannableStringBuilder combined = new SpannableStringBuilder(last);
                combined.append(parsed);
                lines.addLast(combined);
            } else {
                lines.addLast(parsed);
            }

            while (lines.size() > maxLines) {
                lines.removeFirst();
            }
        }
    }

    public synchronized CharSequence render() {
        SpannableStringBuilder sb = new SpannableStringBuilder();
        for (int i = 0; i < lines.size(); i++) {
            sb.append(lines.get(i));
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb;
    }

    public synchronized void clear() {
        lines.clear();
    }

    public synchronized int getLineCount() {
        return lines.size();
    }
}
