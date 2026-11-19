package org.icewocker.iceterm;

import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnsiParser {

    private static final int DEFAULT_FG = Color.rgb(230, 237, 243); // #E6EDF3

    // Standard 16 ANSI colors
    private static final int[] ANSI_16_COLORS = new int[]{
            Color.rgb(13, 17, 23),    // 0: Black
            Color.rgb(248, 81, 73),   // 1: Red
            Color.rgb(63, 185, 80),   // 2: Green
            Color.rgb(210, 153, 34),  // 3: Yellow
            Color.rgb(88, 166, 255),  // 4: Blue
            Color.rgb(188, 140, 255), // 5: Magenta
            Color.rgb(56, 139, 253),  // 6: Cyan
            Color.rgb(201, 209, 217), // 7: White
            Color.rgb(72, 79, 88),    // 8: Bright Black (Gray)
            Color.rgb(255, 123, 114), // 9: Bright Red
            Color.rgb(86, 211, 100),  // 10: Bright Green
            Color.rgb(227, 179, 65),  // 11: Bright Yellow
            Color.rgb(121, 192, 255), // 12: Bright Blue
            Color.rgb(210, 168, 255), // 13: Bright Magenta
            Color.rgb(88, 166, 255),  // 14: Bright Cyan
            Color.rgb(240, 246, 252)  // 15: Bright White
    };

    private static final Pattern ANSI_PATTERN = Pattern.compile("\033\\[([0-9;]*)m");

    public static CharSequence parse(String rawText) {
        if (rawText == null || rawText.isEmpty()) return "";

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        Matcher matcher = ANSI_PATTERN.matcher(rawText);

        int lastIdx = 0;
        int currentFg = DEFAULT_FG;
        int currentBg = Color.TRANSPARENT;
        boolean isBold = false;
        boolean isUnderline = false;

        while (matcher.find()) {
            int matchStart = matcher.start();
            if (matchStart > lastIdx) {
                String plain = rawText.substring(lastIdx, matchStart);
                int spanStart = ssb.length();
                ssb.append(plain);
                int spanEnd = ssb.length();

                applyStyles(ssb, spanStart, spanEnd, currentFg, currentBg, isBold, isUnderline);
            }

            String params = matcher.group(1);
            if (params == null || params.isEmpty() || "0".equals(params)) {
                currentFg = DEFAULT_FG;
                currentBg = Color.TRANSPARENT;
                isBold = false;
                isUnderline = false;
            } else {
                String[] codes = params.split(";");
                for (int i = 0; i < codes.length; i++) {
                    if (codes[i].isEmpty()) continue;
                    try {
                        int code = Integer.parseInt(codes[i]);
                        if (code == 0) {
                            currentFg = DEFAULT_FG;
                            currentBg = Color.TRANSPARENT;
                            isBold = false;
                            isUnderline = false;
                        } else if (code == 1) {
                            isBold = true;
                        } else if (code == 4) {
                            isUnderline = true;
                        } else if (code >= 30 && code <= 37) {
                            currentFg = ANSI_16_COLORS[code - 30];
                        } else if (code >= 90 && code <= 97) {
                            currentFg = ANSI_16_COLORS[code - 90 + 8];
                        } else if (code == 39) {
                            currentFg = DEFAULT_FG;
                        } else if (code >= 40 && code <= 47) {
                            currentBg = ANSI_16_COLORS[code - 40];
                        } else if (code >= 100 && code <= 107) {
                            currentBg = ANSI_16_COLORS[code - 100 + 8];
                        } else if (code == 49) {
                            currentBg = Color.TRANSPARENT;
                        } else if (code == 38 && i + 2 < codes.length && "5".equals(codes[i + 1])) {
                            // 256 colors
                            int colorIdx = Integer.parseInt(codes[i + 2]);
                            currentFg = get256Color(colorIdx);
                            i += 2;
                        }
                    } catch (Exception ignored) {}
                }
            }

            lastIdx = matcher.end();
        }

        if (lastIdx < rawText.length()) {
            String remaining = rawText.substring(lastIdx);
            int spanStart = ssb.length();
            ssb.append(remaining);
            int spanEnd = ssb.length();
            applyStyles(ssb, spanStart, spanEnd, currentFg, currentBg, isBold, isUnderline);
        }

        return ssb;
    }

    private static void applyStyles(SpannableStringBuilder ssb, int start, int end, int fg, int bg, boolean bold, boolean underline) {
        if (start >= end) return;
        ssb.setSpan(new ForegroundColorSpan(fg), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        if (bg != Color.TRANSPARENT) {
            ssb.setSpan(new BackgroundColorSpan(bg), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (bold) {
            ssb.setSpan(new StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static int get256Color(int idx) {
        if (idx < 16) return ANSI_16_COLORS[idx];
        if (idx >= 232) {
            // Greyscale
            int v = (idx - 232) * 10 + 8;
            return Color.rgb(v, v, v);
        }
        // 6x6x6 color cube
        int n = idx - 16;
        int r = (n / 36) * 51;
        int g = ((n / 6) % 6) * 51;
        int b = (n % 6) * 51;
        return Color.rgb(r, g, b);
    }
}
