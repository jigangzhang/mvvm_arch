package com.god.seep.base.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.ForegroundColorSpan;

/**
 */
public class SpUtil {
    private SpUtil() {
    }

    public static SpannableString spanTextSize(String text, int size, int start, int end) {
        SpannableString span = new SpannableString(text);
        span.setSpan(new AbsoluteSizeSpan(size, true),
                start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return span;
    }

    public static SpannableString spanTextColor(String text, int color, int start, int end) {
        SpannableString span = new SpannableString(text);
        span.setSpan(new ForegroundColorSpan(color),
                start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return span;
    }

    public static SpannableString spanTextColor(SpannableString text, int color, int start, int end) {
//        SpannableString span = new SpannableString(text);
        text.setSpan(new ForegroundColorSpan(color),
                start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        return text;
    }
}
