package com.fastkeys1;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;
import java.util.ArrayList;
import java.util.List;

public class FastKeysInputMethodService extends InputMethodService {
    private FastKeysKeyboardView keyboard;

    @Override public View onCreateInputView() {
        keyboard = new FastKeysKeyboardView(this);
        return keyboard;
    }

    @Override public void onStartInputView(android.view.inputmethod.EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        if (keyboard != null) keyboard.refreshSuggestions();
    }

    public void type(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) { ic.commitText(s, 1); if (keyboard != null) keyboard.refreshSuggestions(); }
    }

    public void replaceCurrentWord(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence before = ic.getTextBeforeCursor(80, 0);
        int n = 0;
        if (before != null) {
            String b = before.toString();
            int i = b.length() - 1;
            while (i >= 0 && !Character.isWhitespace(b.charAt(i))) { n++; i--; }
        }
        if (n > 0) ic.deleteSurroundingText(n, 0);
        ic.commitText(s + " ", 1);
        if (keyboard != null) keyboard.refreshSuggestions();
    }

    public void backspace() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) { ic.deleteSurroundingText(1, 0); if (keyboard != null) keyboard.refreshSuggestions(); }
    }

    public void enter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
        if (keyboard != null) keyboard.refreshSuggestions();
    }

    public void move(int keyCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        if (keyboard != null) keyboard.refreshSuggestions();
    }

    public void copyAll() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.performContextMenuAction(android.R.id.selectAll);
        ic.performContextMenuAction(android.R.id.copy);
    }

    public void cut() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) { ic.performContextMenuAction(android.R.id.cut); refresh(); }
    }

    public void paste() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) { ic.performContextMenuAction(android.R.id.paste); refresh(); }
    }

    public void undo() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.performContextMenuAction(android.R.id.undo);
    }

    private void refresh() { if (keyboard != null) keyboard.refreshSuggestions(); }
}
