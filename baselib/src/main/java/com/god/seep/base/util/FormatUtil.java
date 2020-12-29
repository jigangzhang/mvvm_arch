package com.god.seep.base.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 */
public class FormatUtil {
    private FormatUtil() {
    }

    public static long formatTime(String dateStr) {
        long time = 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date != null)
                time = date.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    public static String formatDisplayTime(String dateStr) {
        String time = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            sdf.applyPattern("yyyy-MM-dd");
            if (date != null)
                time = sdf.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    public static String plusDisplayTime(String dateStr, int day) {
        String time = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            if (date != null) {
                date.setTime(date.getTime() + day * 24 * 60 * 60 * 1000);
                sdf.applyPattern("yyyy-MM-dd");
                time = sdf.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    public static String formatPeriodTime(String start, String end) {
        if (start == null || end == null) return "";
        String time = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date startDate = sdf.parse(start);
            Date endDate = sdf.parse(end);
            sdf.applyPattern("yyyy.MM.dd");
            if (startDate != null) {
                time = sdf.format(startDate);
            }
            if (endDate != null) {
                time = time + "-" + sdf.format(endDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    public static String formatPeriodTime(String dateStr, int day) {
        String time = "";
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Date date = sdf.parse(dateStr);
            sdf.applyPattern("yyyy.MM.dd");
            if (date != null) {
                time = sdf.format(date);
                date.setTime(date.getTime() + day * 24 * 60 * 60 * 1000);
                time = time + "-" + sdf.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time;
    }

    /**
     * 将 分 转换为 元
     */
    public static String formatShowPrice(int money) {
        String price = money + "";
        if (price.length() >= 3) {
            String pre = price.substring(0, price.length() - 2);
            String back = price.substring(price.length() - 2);
            return pre + "." + back;
        } else {
            if (price.length() == 2)
                return "0." + price;
            else
                return "0.0" + price;
        }
    }
}
