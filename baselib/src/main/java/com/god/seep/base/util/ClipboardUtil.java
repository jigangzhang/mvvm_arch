package com.god.seep.base.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardUtil {
    /**
     * 复制文字至剪切板
     */
    public static void copyText2Clipboard(Context context, String label, String text) {
        ClipboardManager manager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(label, text);
        manager.setPrimaryClip(clipData);
    }
}
