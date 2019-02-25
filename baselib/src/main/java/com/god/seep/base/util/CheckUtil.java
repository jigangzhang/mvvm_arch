package com.god.seep.base.util;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * 手机号码检验等
 * </p>
 */

public class CheckUtil {
    public static boolean isMobileNumber(String phone) {
        if (!TextUtils.isEmpty(phone) && phone.startsWith("+86")) {
            phone = phone.replace("+86", "").trim();
        }
        if (TextUtils.isEmpty(phone) || phone.length() != 11)
            return false;
        if (!phone.startsWith("1"))
            return false;
        return true;
    }

    /**
     * 过滤非法字符
     */
    public static String filterText(String text) {
        if (TextUtils.isEmpty(text))
            return "";
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isEmoji(ch))
                text = text.replace(ch, '\0');
        }
        text = text.replaceAll("\0", "");
        String regEx = "[\\~!#$%^&*\\(\\)_|:<>`?;'·,…/+\\-=—\\[\\]]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(text);
        if (m.find()) {
            text = m.replaceAll("").trim();
        }
        return text;
    }

    public static boolean isEmoji(char ch) {
        if ((ch == 0x0) || (ch == 0x9) || (ch == 0xA) || (ch == 0xD)
                || ((ch >= 0x20) && (ch <= 0xD7FF))
                || ((ch >= 0xE000) && (ch <= 0xFFFD))
                || ((ch >= 0x10000) && (ch <= 0x10FFFF)))
            return false;
        return true;
    }

    public static String filterChinese(String text) {
        if (TextUtils.isEmpty(text)) return "";
        String regex = "[\\u4e00-\\u9fa5]";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            text = matcher.replaceAll("").trim();
        }
        return text;
    }

    public static boolean isEmail(String email) {
        String regex = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }
}
