package com.god.seep.base.util;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;

public class NetUtil {
    private static ConnectivityManager getManager(Context context) {
        return (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    /**
     * 判断网络是否可用
     */
    public static boolean isNetAvailable(Context context) {
        ConnectivityManager manager = getManager(context);
        NetworkInfo info = manager.getActiveNetworkInfo();
        if (info != null) {
            int type = info.getType();
            if (type == ConnectivityManager.TYPE_WIFI)
                return isWifiConnected(context);
            else if (type == ConnectivityManager.TYPE_MOBILE)
                return isMobileConnected(context);
            else
                return info.isAvailable() && info.isConnected() && info.getState() == NetworkInfo.State.CONNECTED;
        } else
            return false;
    }

    /**
     * 判断WiFi网络是否可用
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager manager = getManager(context);
        NetworkInfo wifiInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (null != wifiInfo && wifiInfo.isAvailable()) {
            return wifiInfo.isConnected() && wifiInfo.getState() == NetworkInfo.State.CONNECTED;
        }
        return false;
    }

    /**
     * 判断移动网络是否可用
     */
    public static boolean isMobileConnected(Context context) {
        ConnectivityManager manager = getManager(context);
        NetworkInfo mobileInfo = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (null != mobileInfo && mobileInfo.isAvailable()) {
            return mobileInfo.isConnected() && mobileInfo.getState() == NetworkInfo.State.CONNECTED;
        }
        return false;
    }

    /**
     * 打开设置界面
     */
    public static void openNetSettingActivity(Context context) {
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
