package com.god.seep.base.util;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;

public class ScreenHelper {

    /**
     * 获取屏幕宽度 px
     */
    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        manager.getDefaultDisplay().getSize(point);
        return point.x;
    }

    /**
     * 获取屏幕高度 px,不包含虚拟导航栏
     */
    public static int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        manager.getDefaultDisplay().getSize(point);
        return point.y;
    }

    /**
     * 屏幕实际高度
     */
    public static int getRealHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        if (manager != null) {
            manager.getDefaultDisplay().getRealSize(point);
        }
        return point.y;
    }

    /**
     * 是否存在虚拟导航栏，暂只考虑小米，华为全面屏待适配
     */
    public static boolean hasNavigationBar(Context context) {
        if (isXiaoMi())
            return showNavi(context);
        else
            return true;
//        boolean hasBar = false;
//        Resources res = context.getResources();
//        int id = res.getIdentifier("config_showNavigationBar", "bool", "android");
//        if (id > 0) {
//            hasBar = res.getBoolean(id);
//        }
//        try {
//            @SuppressLint("PrivateApi")
//            Class<?> aClass = Class.forName("android.os.SystemProperties");
//            Method method = aClass.getMethod("get", String.class);
//            Object value = method.invoke(aClass, "qemu.hw.mainkeys");
//            if ("1".equals(value)) {
//                hasBar = false;
//            } else if ("0".equals(value)) {
//                hasBar = true;
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return hasBar;
    }

    public static boolean isXiaoMi() {
        return Build.MANUFACTURER.equals("Xiaomi");
    }

    /**
     * 小米全面屏手势检测，虚拟导航栏存在于全面屏中，有手势无导航栏
     */
    public static boolean showNavi(Context context) {
        return Settings.Global.getInt(context.getContentResolver(), "force_fsg_nav_bar", 0) == 0;
    }

    /**
     * px 转 dp
     *
     * @param context
     * @param pxVal   像素值
     */
    public static int px2Dp(Context context, int pxVal) {
        int density = context.getResources().getDisplayMetrics().widthPixels / 360;
        return (int) (pxVal / density + 0.5f);
    }

    /**
     * dp 转 px
     *
     * @param context
     * @param dpVal   dp值
     */
    public static int dp2Px(Context context, float dpVal) {
        int density = context.getResources().getDisplayMetrics().widthPixels / 360; ////因修改了系统dpi值，此处以360为基准，自己计算density
        return (int) (dpVal * density + 0.5f);
    }

    /**
     * 将px值转换为sp值，保证文字大小不变
     *
     * @param pxValue
     * @return
     */
    public static int px2sp(Context context, float pxValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (pxValue / fontScale + 0.5f);
    }

    /**
     * 将sp值转换为px值，保证文字大小不变
     *
     * @param spValue
     * @param context （DisplayMetrics类中属性scaledDensity）
     * @return
     */
    public static int sp2px(Context context, float spValue) {
        final float fontScale = context.getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    /**
     * 用于解决部分7.0及以上版本手机设置显示大小后的布局变形问题
     * 一种UI适配方式（头条方案）
     * 以360为基准（设计稿），dp转px时也应该使用手动计算
     */
    public static void setDefaultDensity(Activity context, @NonNull Application application) {
        if (context == null) return;
        DisplayMetrics appMetrics = application.getResources().getDisplayMetrics();
        DisplayMetrics sysMetrics = Resources.getSystem().getDisplayMetrics();
        int targetDensity = appMetrics.widthPixels / 360;
        appMetrics.density = targetDensity;
        appMetrics.scaledDensity = (sysMetrics.scaledDensity / sysMetrics.density) * targetDensity;
        appMetrics.densityDpi = targetDensity * 160;

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        metrics.density = targetDensity;
        metrics.scaledDensity = (sysMetrics.scaledDensity / sysMetrics.density) * targetDensity;
        metrics.densityDpi = targetDensity * 160;
        Configuration configuration = context.getResources().getConfiguration();
        configuration.densityDpi = targetDensity * 160;
        context.getResources().updateConfiguration(configuration, metrics);
    }

    public static void setContextDensity(Context context) {
        DisplayMetrics sysMetrics = Resources.getSystem().getDisplayMetrics();
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int targetDensity = metrics.widthPixels / 360;
        metrics.density = targetDensity;
        metrics.densityDpi = targetDensity * 160;
//        metrics.scaledDensity = (sysMetrics.scaledDensity / sysMetrics.density) * targetDensity;
    }
}
