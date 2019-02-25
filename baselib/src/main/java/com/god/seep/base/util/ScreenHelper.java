package com.god.seep.base.util;

import android.content.Context;
import android.view.WindowManager;

public class ScreenHelper {

    /**
     * 获取屏幕宽度 px
     */
    public static int getScreenWidth(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
//        Point point = new Point();
//        manager.getDefaultDisplay().getSize(point);
//        return point.x;
        return manager.getDefaultDisplay().getWidth();
    }

    /**
     * 获取屏幕高度 px
     */
    public static int getScreenHeight(Context context) {
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        return manager.getDefaultDisplay().getHeight();
    }

    /**
     * px 转 dp
     *
     * @param context
     * @param pxVal   像素值
     */
    public static int px2Dp(Context context, int pxVal) {
        float density = context.getResources().getDisplayMetrics().density;
        return (int) (pxVal / density + 0.5f);
    }

    /**
     * dp 转 px
     *
     * @param context
     * @param dpVal   dp值
     */
    public static int dp2Px(Context context, float dpVal) {
        float density = context.getResources().getDisplayMetrics().density;
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
}
