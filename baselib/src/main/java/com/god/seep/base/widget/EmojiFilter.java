package com.god.seep.base.widget;

import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextUtils;

import com.god.seep.base.util.CheckUtil;

public class EmojiFilter implements InputFilter {
    private boolean filterSpecial;
    private boolean filterEmoji;
    private boolean filterCH;

    public EmojiFilter(boolean filterSpecial, boolean filterEmoji, boolean filterCH) {
        this.filterSpecial = filterSpecial;
        this.filterEmoji = filterEmoji;
        this.filterCH = filterCH;
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
        if (TextUtils.isEmpty(source)) return "";
        String text = source.toString();
        if (filterSpecial)
            text = CheckUtil.filterText(text);
        if (filterEmoji) {
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                if (CheckUtil.isEmoji(ch))
                    text = text.replace(ch, '\0');
            }
            text = text.replaceAll("\0", "");
        }
        if (filterCH) {
            text = CheckUtil.filterChinese(text);
        }

        return text;
    }
}
