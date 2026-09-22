package com.fastkeys1;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Fast_Keys_1 - Reset 20 fresh UI.
 * Persian/English layouts, cream default, compact symbols, larger Backspace/Enter,
 * reduced Space with wider navigation arrows, and a color palette in the drawer.
 */
public class FastKeysView extends android.view.View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final InputConnection ic;
    private final float density;
    private final ArrayList<String> history = new ArrayList<>();
    private final ArrayList<KeyHit> hits = new ArrayList<>();
    private final SharedPreferences prefs;

    private final int BLUE = Color.rgb(18, 120, 220);
    private int keyColor;
    private boolean hideTop = false;
    private boolean persian = true;
    private float transparency = 1f;
    private KeyHit pressedKey;
    private final ArrayList<String> suggestions = new ArrayList<>();
    private final Handler repeatHandler = new Handler();
    private boolean repeatingBackspace = false;
    private boolean repeatingSymbol = false;
    private boolean wasSymbolRepeated = false;
    private String repeatSymbol = null;

    private static class KeyHit {
        RectF r;
        String action;
        KeyHit(RectF r, String action) { this.r = r; this.action = action; }
    }

    public FastKeysView(Context context, InputConnection connection) {
        super(context);
        ic = connection;
        density = getResources().getDisplayMetrics().density;
        prefs = context.getSharedPreferences("keyboard_prefs", Context.MODE_PRIVATE);
        keyColor = prefs.getInt("key_color", Color.rgb(255, 253, 245));
        transparency = prefs.getInt("transparency", 100) / 100f;
        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    private float dp(float v) { return v * density; }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        c.drawColor(keyColor);
        hits.clear();

        float w = getWidth();
        float h = getHeight();
        float gap = dp(1f);
        float y = 0f;

        // Suggestions are an overlay: they never change the geometry of the keyboard.
        // This keeps every key in the same place and prevents accidental taps caused by movement.
        refreshSuggestions();

        // Reset 20: keyboard starts directly at the first key row.
        // No search/status/header strip and no visible outer frame.
        float numberH = h * 0.125f;
        drawNumberRow(c, y, numberH, w, gap);
        if (!suggestions.isEmpty()) {
            // Suggestions are drawn over the top row without changing keyboard geometry.
            // Backspace is always redrawn on top and remains visible and tappable.
            drawSuggestions(c, y, numberH, w, gap);
            drawBackspaceOverlay(c, y, numberH, w, gap);
        }
        y += numberH;

        float rowH = (h - y) / 4f;
        drawLetterRow(c, y, rowH, w, gap, 0); y += rowH;
        drawLetterRow(c, y, rowH, w, gap, 1); y += rowH;
        drawLetterRow(c, y, rowH, w, gap, 2); y += rowH;
        drawBottom(c, y, h - y, w, gap);
    }

    private void refreshSuggestions() {
        suggestions.clear();
        if (ic == null) return;
        CharSequence cs = ic.getTextBeforeCursor(64, 0);
        if (cs == null) return;
        String text = cs.toString();
        String[] parts = text.split("\\s+");
        if (parts.length == 0) return;
        String prefix = parts[parts.length - 1].trim();
        if (prefix.isEmpty()) return;

        String[] dictionary = persian ? PERSIAN_WORDS : ENGLISH_WORDS;
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String word : dictionary) {
            if (word.startsWith(prefix) && !word.equals(prefix)) out.add(word);
            if (out.size() >= 3) break;
        }
        suggestions.addAll(out);
    }

    private static final String[] ENGLISH_WORDS = {
            "the","this","that","there","their","then","they","them","these","those",
            "what","when","where","which","while","with","will","would","could","should",
            "have","has","had","having","hello","help","here","home","how","however",
            "from","for","good","great","going","give","get","just","know","like",
            "make","more","most","much","need","never","new","next","only","please",
            "really","right","same","some","something","start","still","take","than","thank",
            "thanks","their","time","today","tomorrow","very","want","well","welcome","work",
            "yes","you","your","about","after","again","also","always","because","before"
    };

    private static final String[] PERSIAN_WORDS = {
            "این","آن","اینجا","آنجا","اگر","امروز","فردا","الان","برای","باید",
            "بیشتر","بهتر","بعد","با","بدون","بود","باشد","بوده","چطور","چرا",
            "چه","چیزی","چون","در","درباره","دوست","دوباره","را","روی","روز",
            "زمان","زیاد","شما","شاید","شد","شده","شود","شما","سلام","صبح",
            "شب","هم","همین","همه","هنوز","هیچ","یک","یکی","میخواهم","می‌خواهم",
            "ممنون","متشکرم","لطفا","لطفاً","کمک","کار","کردن","کردم","کنم","کنید",
            "کجا","کی","کدام","گفت","گفته","گفتن","آمد","آمده","است","هست","هستم",
            "خواهیم","خواهد","خواهدشد","خوب","خیلی","درست","تازه","تمام","تایپ","کیبورد"
    };

    private void drawSuggestions(Canvas c, float y, float rh, float w, float gap) {
        if (suggestions.isEmpty()) return;
        int n = Math.min(3, suggestions.size());
        float backW = (w - gap * 12) * 1.8f / 13.8f;
        float usable = w - gap * 3 - backW;
        float cw = (usable - gap * (n - 1)) / n;
        for (int i = 0; i < n; i++) {
            float l = i * (cw + gap);
            addKey(c, l, y, l + cw, y + rh, suggestions.get(i), 17f, "suggest:" + suggestions.get(i));
        }
    }

    private void drawBackspaceOverlay(Canvas c, float y, float rh, float w, float gap) {
        float backW = (w - gap * 12) * 1.8f / 13.8f;
        float l = w - backW;
        addKey(c, l, y, w, y + rh, "⌫", 20f, "number:12");
    }

    private void drawToolbar(Canvas c, float y, float rh, float w, float gap) {
        String[] a = persian
                ? new String[]{"کپی همه","کپی","چسباندن","Esc","بازگردانی","انجام مجدد","کپی صفحه","تنظیمات","12:30","مخفی کردن"}
                : new String[]{"Copy All","Copy","Paste","Esc","Undo","Redo","Copy Screen","Settings","12:30","Hide"};
        drawEqual(c, a, y, rh, w, 14f, gap, "toolbar:");
    }

    private void drawNumberRow(Canvas c, float y, float rh, float w, float gap) {
        String[] digits = persian
                ? new String[]{"۱","۲","۳","۴","۵","۶","۷","۸","۹","۰"}
                : new String[]{"1","2","3","4","5","6","7","8","9","0"};
        ArrayList<String> a = new ArrayList<>();
        for (String d : digits) a.add(d);
        a.add("-");
        a.add("=");
        a.add("⌫");

        float[] weights = new float[a.size()];
        for (int i = 0; i < 12; i++) weights[i] = 1f;
        weights[12] = 1.8f; // Backspace is always larger than a normal key.
        drawWeighted(c, a.toArray(new String[0]), weights, y, rh, w, gap, 20f, "number:");
    }

    private void drawLetterRow(Canvas c, float y, float rh, float w, float gap, int row) {
        String[] a;
        String[] actions;
        if (persian) {
            if (row == 0) {
                a = new String[]{"ض","ص","ث","ق","ف","غ","ع","ه","خ","ح","ج","چ"};
                actions = a.clone();
            } else if (row == 1) {
                a = new String[]{"ش","س","ی","ب","ل","ا","ت","ن","م","ک","گ","پ"};
                actions = a.clone();
            } else {
                a = new String[]{"Shift","ظ","ط","ز","ر","ذ","د","و","ژ","؟","!","Shift"};
                actions = new String[]{"shift","ظ","ط","ز","ر","ذ","د","و","ژ","؟","!","shift"};
            }
        } else {
            if (row == 0) {
                a = new String[]{"Q","W","E","R","T","Y","U","I","O","P","{","}"};
                actions = a.clone();
            } else if (row == 1) {
                a = new String[]{"A","S","D","F","G","H","J","K","L",";","\"","Enter"};
                actions = new String[]{"A","S","D","F","G","H","J","K","L",";","\"","enter"};
            } else {
                a = new String[]{"Shift","Z","X","C","V","B","N","M",",",".","?","Shift"};
                actions = new String[]{"shift","Z","X","C","V","B","N","M",",",".","?","shift"};
            }
        }
        drawEqualActions(c, a, actions, y, rh, w, 1f, gap, 23f, "letters:" + row);
    }

    private void drawBottom(Canvas c, float y, float rh, float w, float gap) {
        String[] a = persian
                ? new String[]{"#۱","🌐","_","-","~","،","؟","فاصله","←","→","↓","↑"}
                : new String[]{"#1","🌐","_","-","~",",","?","Space","←","→","↓","↑"};
        String[] actions = new String[]{"symbols","language","_","-","~",persian ? "،" : ",",persian ? "؟" : "?","space","left","right","down","up"};

        // Symbols are half-width; ?/؟ is normal-width; Space is reduced; arrows are wider.
        float[] weights = {1.2f,1.2f,0.7f,0.7f,0.7f,0.7f,1.4f,3.4f,1.55f,1.55f,1.55f,1.55f};
        drawWeightedActions(c, a, actions, weights, y, rh, w, gap, 19f, "bottom:");
    }

    private void drawEqual(Canvas c, String[] a, float y, float rh, float w, float size, float gap, String prefix) {
        String[] actions = new String[a.length];
        for (int i = 0; i < a.length; i++) actions[i] = prefix + i;
        drawEqualActions(c, a, actions, y, rh, w, size, gap, size, prefix);
    }

    private void drawEqualActions(Canvas c, String[] labels, String[] actions, float y, float rh, float w, float size, float gap, float textSize, String prefix) {
        float cw = (w - gap * (labels.length - 1)) / labels.length;
        for (int i = 0; i < labels.length; i++) {
            float l = i * (cw + gap);
            addKey(c, l, y, l + cw, y + rh, labels[i], textSize, actions[i]);
        }
    }

    private void drawWeighted(Canvas c, String[] labels, float[] weights, float y, float rh, float w, float gap, float textSize, String prefix) {
        String[] actions = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            actions[i] = prefix + i;
        }
        drawWeightedActions(c, labels, actions, weights, y, rh, w, gap, textSize, prefix);
    }

    private void drawWeightedActions(Canvas c, String[] labels, String[] actions, float[] weights, float y, float rh, float w, float gap, float textSize, String prefix) {
        float total = 0f;
        for (float v : weights) total += v;
        float usable = w - gap * (labels.length - 1);
        float x = 0f;
        for (int i = 0; i < labels.length; i++) {
            float cw = usable * weights[i] / total;
            addKey(c, x, y, x + cw, y + rh, labels[i], textSize, actions[i]);
            x += cw + gap;
        }
    }

    private void addKey(Canvas c, float l, float t, float r, float b, String label, float size, String action) {
        RectF rect = new RectF(l, t, r, b);
        p.setStyle(Paint.Style.FILL);
        p.setColor(keyColor);
        p.setShadowLayer(dp(2.0f), 0f, dp(1f), 0x30000000);
        c.drawRoundRect(rect, dp(8), dp(8), p);
        p.clearShadowLayer();

        if (pressedKey != null && pressedKey.r.equals(rect)) {
            p.setColor(0x28FFD54F);
            c.drawRoundRect(rect, dp(8), dp(8), p);
        }

        boolean strong = action.startsWith("toolbar:") || action.equals("enter") || action.equals("shift")
                || action.equals("left") || action.equals("right") || action.equals("up") || action.equals("down")
                || action.startsWith("number:12");
        if (strong) {
            p.setColor(BLUE);
            p.setStyle(Paint.Style.FILL);
            c.drawRoundRect(rect, dp(8), dp(8), p);
        }
        text(c, label, (l + r) / 2f, (t + b) / 2f, size, strong ? Color.WHITE : BLUE);
        hits.add(new KeyHit(rect, action));
    }

    private void text(Canvas c, String s, float x, float y, float size, int color) {
        p.setColor(color);
        p.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        p.setTextSize(size * getResources().getDisplayMetrics().scaledDensity);
        p.setTextAlign(Paint.Align.CENTER);
        p.setAlpha(Math.max(1, Math.min(255, Math.round(255f * transparency))));
        String[] lines = s.split("\\n");
        float line = p.getTextSize() * 0.78f;
        float start = y - ((lines.length - 1) * line / 2f) - (p.ascent() + p.descent()) / 2f;
        for (String v : lines) {
            c.drawText(v, x, start, p);
            start += line;
        }
        p.setAlpha(255);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            pressedKey = findKey(x, y);
            if (pressedKey != null && pressedKey.action.equals("number:12")) {
                startBackspaceRepeat();
            } else if (pressedKey != null && isFastRepeatSymbol(pressedKey.action)) {
                startSymbolRepeat(pressedKey.action);
            }
            invalidate();
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_MOVE) {
            // Keep the pressed key stable while the finger remains on it.
            pressedKey = findKey(x, y);
            invalidate();
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
            KeyHit k = pressedKey;
            stopBackspaceRepeat();
            pressedKey = null;
            invalidate();
            if (e.getAction() == MotionEvent.ACTION_UP && k != null && k.r.contains(x, y)) {
                // Long-press backspace already performed deletions; a normal tap performs one.
                if ((!k.action.equals("number:12") || !wasBackspaceRepeated) && (!isFastRepeatSymbol(k.action) || !wasSymbolRepeated)) perform(k.action);
            }
            wasBackspaceRepeated = false;
            wasSymbolRepeated = false;
            return true;
        }
        return true;
    }

    private boolean wasBackspaceRepeated = false;

    private void startBackspaceRepeat() {
        stopBackspaceRepeat();
        wasBackspaceRepeated = false;
        repeatHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!repeatingBackspace) return;
                backspace();
                wasBackspaceRepeated = true;
                repeatHandler.postDelayed(this, 75);
            }
        }, 350);
        repeatingBackspace = true;
    }

    private void stopBackspaceRepeat() {
        repeatingBackspace = false;
        repeatingSymbol = false;
        repeatSymbol = null;
        repeatHandler.removeCallbacksAndMessages(null);
    }

    private boolean isFastRepeatSymbol(String a) {
        return "=".equals(a) || "+".equals(a) || "_".equals(a) || "#".equals(a) || "|".equals(a) || "*".equals(a) || "∆".equals(a);
    }

    private void startSymbolRepeat(final String symbol) {
        stopBackspaceRepeat();
        wasSymbolRepeated = false;
        repeatSymbol = symbol;
        repeatingSymbol = true;
        repeatHandler.postDelayed(new Runnable() {
            @Override public void run() {
                if (!repeatingSymbol || repeatSymbol == null) return;
                type(repeatSymbol);
                wasSymbolRepeated = true;
                repeatHandler.postDelayed(this, 55);
            }
        }, 180);
    }

    private KeyHit findKey(float x, float y) {
        for (KeyHit k : hits) if (k.r.contains(x, y)) return k;
        return null;
    }

    private void perform(String action) {
        if (action.startsWith("suggest:")) {
            String suggestion = action.substring(action.indexOf(':') + 1);
            replaceCurrentWord(suggestion);
            return;
        }
        if (action.startsWith("toolbar:")) {
            int i = Integer.parseInt(action.substring(action.indexOf(':') + 1));
            switch (i) {
                case 0: copyAll(); break;
                case 1: copy(); break;
                case 2: paste(); break;
                case 3: toast("Esc"); break;
                case 4: undo(); break;
                case 5: redo(); break;
                case 6: toast(persian ? "کپی صفحه" : "Copy Screen"); break;
                case 7: openDrawer(); break;
                case 9: hideTop = true; invalidate(); break;
                default: break;
            }
            return;
        }
        if (action.startsWith("number:")) {
            int i = Integer.parseInt(action.substring(action.indexOf(':') + 1));
            if (i == 12) { backspace(); return; }
            String[] d = persian ? new String[]{"۱","۲","۳","۴","۵","۶","۷","۸","۹","۰","-","="}
                    : new String[]{"1","2","3","4","5","6","7","8","9","0","-","="};
            type(d[i]);
            return;
        }
        if (action.equals("enter")) { enter(); return; }
        if (action.equals("shift")) { toast(persian ? "شیفت" : "Shift"); return; }
        if (action.equals("space")) { type(" "); return; }
        if (action.equals("language")) {
            persian = !persian;
            invalidate();
            return;
        }
        if (action.equals("symbols")) { toast(persian ? "علائم" : "Symbols"); return; }
        if (action.equals("left")) { sendDpad(KeyEvent.KEYCODE_DPAD_LEFT); return; }
        if (action.equals("right")) { sendDpad(KeyEvent.KEYCODE_DPAD_RIGHT); return; }
        if (action.equals("up")) { sendDpad(KeyEvent.KEYCODE_DPAD_UP); return; }
        if (action.equals("down")) { sendDpad(KeyEvent.KEYCODE_DPAD_DOWN); return; }
        if (action.equals("_") || action.equals("-") || action.equals("~") || action.equals("،") || action.equals(",") || action.equals("؟") || action.equals("?") || action.equals("=") || action.equals("+") || action.equals("#") || action.equals("|") || action.equals("*") || action.equals("∆")) {
            type(action); return;
        }
        type(action);
    }

    private void replaceCurrentWord(String suggestion) {
        if (ic == null) return;
        CharSequence cs = ic.getTextBeforeCursor(64, 0);
        if (cs == null) return;
        String text = cs.toString();
        int i = text.length() - 1;
        while (i >= 0 && !Character.isWhitespace(text.charAt(i))) i--;
        int count = text.length() - (i + 1);
        if (count > 0) ic.deleteSurroundingText(count, 0);
        ic.commitText(suggestion + " ", 1);
        invalidate();
    }

    private void sendDpad(int code) {
        if (ic == null) return;
        // First use the real editor navigation event. This works in editors that
        // implement cursor navigation themselves.
        try {
            long now = System.currentTimeMillis();
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0));
            ic.sendKeyEvent(new KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0));
        } catch (Throwable ignored) {
        }

        // Then use selection as a fallback for editors that ignore DPAD events.
        try {
            ExtractedText et = ic.getExtractedText(new ExtractedTextRequest(), 0);
            if (et != null && et.text != null && et.selectionStart >= 0) {
                int pos = Math.max(0, Math.min(et.selectionStart, et.text.length()));
                int target = pos;
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) target = Math.max(0, pos - 1);
                else if (code == KeyEvent.KEYCODE_DPAD_RIGHT) target = Math.min(et.text.length(), pos + 1);
                else if (code == KeyEvent.KEYCODE_DPAD_UP || code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    String t = et.text.toString();
                    int lineStart = t.lastIndexOf('\n', Math.max(0, pos - 1)) + 1;
                    int column = pos - lineStart;
                    if (code == KeyEvent.KEYCODE_DPAD_UP && lineStart > 0) {
                        int prevEnd = lineStart - 1;
                        int prevStart = t.lastIndexOf('\n', Math.max(0, prevEnd - 1)) + 1;
                        target = Math.min(prevStart + column, prevEnd);
                    } else if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                        int nextStart = t.indexOf('\n', pos);
                        if (nextStart >= 0) {
                            nextStart++;
                            int nextEnd = t.indexOf('\n', nextStart);
                            if (nextEnd < 0) nextEnd = t.length();
                            target = Math.min(nextStart + column, nextEnd);
                        }
                    }
                }
                if (target != pos) ic.setSelection(target, target);
            }
        } catch (Throwable ignored) {
        }
        invalidate();
    }

    public void setPersianLanguage(boolean value) {
        persian = value;
        invalidate();
    }

    private void type(String s) { if (ic != null) { ic.finishComposingText(); ic.commitText(s, 1); invalidate(); } }
    private void enter() { if (ic != null) { ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)); invalidate(); } }
    private void backspace() { if (ic != null) { ic.deleteSurroundingText(1, 0); invalidate(); } }
    private void undo() { if (ic != null) ic.performContextMenuAction(android.R.id.undo); }
    private void redo() { if (ic != null) ic.performContextMenuAction(android.R.id.redo); }

    private void copy() {
        if (ic == null) return;
        CharSequence s = ic.getSelectedText(0);
        if (s != null) saveHistory(s.toString());
    }

    private void copyAll() {
        if (ic == null) return;
        ic.performContextMenuAction(android.R.id.selectAll);
        CharSequence s = ic.getSelectedText(0);
        if (s != null) saveHistory(s.toString());
    }

    private void paste() {
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (ic != null && cm != null && cm.hasPrimaryClip()) {
            ic.commitText(cm.getPrimaryClip().getItemAt(0).coerceToText(getContext()), 1);
        }
    }

    private void saveHistory(String s) {
        if (s == null || s.isEmpty()) return;
        history.remove(s);
        history.add(0, s);
        while (history.size() > 100) history.remove(100);
        ClipboardManager cm = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("Fast_Keys_1", s));
    }

    private void openDrawer() {
        LinearLayout box = new LinearLayout(getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(16, 8, 16, 8);

        TextView title = new TextView(getContext());
        title.setText("گزینه‌های کشویی / Drawer");
        title.setTextColor(BLUE);
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        box.addView(title);

        Button palette = new Button(getContext());
        palette.setText("پالت رنگ صفحه و کیبورد");
        box.addView(palette);
        palette.setOnClickListener(v -> showPalette());

        Button transparencyButton = new Button(getContext());
        transparencyButton.setText("Transparency 1–100%");
        box.addView(transparencyButton);
        transparencyButton.setOnClickListener(v -> showTransparency());

        Button historyButton = new Button(getContext());
        historyButton.setText("Copy History — 100 آخر");
        box.addView(historyButton);
        historyButton.setOnClickListener(v -> showHistory());


        Button carButton = new Button(getContext());
        carButton.setText("فرمان ماشین — Widget");
        box.addView(carButton);
        carButton.setOnClickListener(v -> toast("فرمان ماشین از Widget اجرا می‌شود"));

        new AlertDialog.Builder(getContext()).setView(box).setNegativeButton("بستن", null).show();
    }

    private void showPalette() {
        final int[] colors = {
                Color.rgb(255,253,245), // cream default
                Color.WHITE,
                Color.rgb(242,248,255),
                Color.rgb(242,255,246),
                Color.rgb(255,248,238),
                Color.rgb(250,244,255),
                Color.rgb(245,245,245),
                Color.rgb(255,245,248)
        };
        final String[] names = {"سفید شیری (پیش‌فرض)","سفید","آبی خیلی روشن","سبز خیلی روشن","کرم گرم","یاسی روشن","خاکستری روشن","صورتی خیلی روشن"};
        new AlertDialog.Builder(getContext())
                .setTitle("پالت رنگ صفحه و کیبورد")
                .setItems(names, (d, which) -> {
                    keyColor = colors[which];
                    prefs.edit().putInt("key_color", keyColor).apply();
                    invalidate();
                })
                .setNegativeButton("بستن", null)
                .show();
    }

    private void showTransparency() {
        SeekBar sb = new SeekBar(getContext());
        sb.setMax(99);
        sb.setProgress(Math.max(0, Math.min(99, Math.round(transparency * 99f))));
        new AlertDialog.Builder(getContext()).setTitle("Transparency 1–100%")
                .setMessage("رول را بالا و پایین حرکت دهید")
                .setView(sb)
                .setPositiveButton("ثبت", (d, which) -> {
                    transparency = (sb.getProgress() + 1) / 100f;
                    prefs.edit().putInt("transparency", Math.round(transparency * 100f)).apply();
                    invalidate();
                }).setNegativeButton("لغو", null).show();
    }

    private void showHistory() {
        String[] a = history.toArray(new String[0]);
        if (a.length == 0) a = new String[]{"(خالی)"};
        new AlertDialog.Builder(getContext()).setTitle("Copy History — 100 آخر")
                .setItems(a, (d, which) -> { if (which < history.size()) type(history.get(which)); })
                .setNegativeButton("بستن", null).show();
    }

    private void toast(String s) { Toast.makeText(getContext(), s, Toast.LENGTH_SHORT).show(); }
}
