package com.god.seep.base.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Process;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

public class AppUtil {
    /**
     * 生成设备唯一标识码
     */
    public static String getDeviceId(Context context) {
        return buildDeviceUUID(context);
    }

    private static String getBuildInfo() {
        return "BOARD:" + Build.BOARD +
                ",BOOTLOADER:" + Build.BOOTLOADER +
                ",BRAND:" + Build.BRAND +
                ",DEVICE:" + Build.DEVICE +
                ",DISPLAY:" + Build.DISPLAY +
                ",FINGERPRINT:" + Build.FINGERPRINT +
                ",HARDWARE:" + Build.HARDWARE +
                ",HOST:" + Build.HOST +
                ",ID:" + Build.ID +
                ",MANUFACTURER:" + Build.MANUFACTURER +
                ",MODEL:" + Build.MODEL +
                ",PRODUCT:" + Build.PRODUCT +
                ",TIME:" + Build.TIME +
                ",TYPE:" + Build.TYPE +
                ",USER:" + Build.USER +
                ",TAG:" + Build.TAGS +
                ",SERIAL:" + Build.SERIAL +
                ",RELEASE:" + Build.VERSION.RELEASE +
                ",CODENAME:" + Build.VERSION.CODENAME +
                ",SDK_INT:" + Build.VERSION.SDK_INT;
    }

    private static String buildDeviceUUID(Context context) {
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (TextUtils.isEmpty(androidId) || "9774d56d682e549c".equals(androidId)) {
            Random random = new Random();
            androidId = Integer.toHexString(random.nextInt())
                    + Integer.toHexString(random.nextInt())
                    + Integer.toHexString(random.nextInt());
        }
//        Logger.e("system info androidId--" + androidId);
//        Logger.e("system info--" + getBuildInfo());
        return new UUID(androidId.hashCode(), getBuildInfo().hashCode()).toString().replaceAll("-", "");
    }

    /**
     * 获取App版本名称
     *
     * @return 返回App版本名
     */
    public static String getVersionName(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return "V" + info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /**
     * 获取App版本号
     *
     * @return 返回App版本号
     */
    public static int getVersionCode(Context context) {
        PackageManager manager = context.getPackageManager();
        try {
            PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
            return info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /**
     * 拨打电话
     *
     * @param context     上下文
     * @param phoneNumber 电话号码
     */
    public static void callPhone(Context context, String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) return;
        try {
            Intent intent = new Intent();
            // 系统默认的action，用来打开默认的电话界面
            intent.setAction(Intent.ACTION_DIAL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Uri data = Uri.parse("tel:" + phoneNumber);
            intent.setData(data);
            context.getApplicationContext().startActivity(intent);
        } catch (Exception e) {
            ToastHelper.showToast(context.getApplicationContext(), "未获取到电话号码");
            e.printStackTrace();
        }
    }

    /**
     * 设备中是否安装某应用
     *
     * @param context     上下文
     * @param packageName 应用包名
     */
    public static boolean isAppInstalled(@NonNull Context context, @NonNull String packageName) {
        PackageManager manager = context.getApplicationContext().getPackageManager();
        List<PackageInfo> infoList = manager.getInstalledPackages(PackageManager.GET_INSTRUMENTATION);
        for (PackageInfo info : infoList) {
            if (packageName.equals(info.packageName))
                return true;
        }
        return false;
    }

    public static boolean isWXInstalled(Context context) {
        try {
            return context.getPackageManager().getPackageInfo("com.tencent.mm", PackageManager.GET_INSTRUMENTATION) != null;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static void launchActivity(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        context.startActivity(intent);
    }

    public static void hideKeyboardIfNeed(View view) {
        InputMethodManager methodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (methodManager.isActive()) {
            methodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    public static void showKeyboardIfNeed(View view) {
        InputMethodManager methodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        methodManager.showSoftInput(view, 1);

    }

    public static void launchSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    public static void installApp(Context context, Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //适配 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            //适配 8.0 相关
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.getPackageManager().canRequestPackageInstalls()) {
//                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, //跳转设置页
//                    ToastHelper.showToast(context, "请开启安装未知来源权限");
//                    return;
                }
            }
        } else {
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    public static void installApp(Context context, File file) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        //适配 7.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            //适配 8.0 相关
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.getPackageManager().canRequestPackageInstalls()) {
//                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, //跳转设置页
//                    ToastHelper.showToast(context, "请开启安装未知来源权限");
//                    return;
                }
            }
        } else {
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        }
        context.startActivity(intent);
    }

    //激活相机操作
    public static File openCamera(Activity context) {
        File cameraSavePath = new File(Environment.getExternalStorageDirectory().getPath()
                + File.separator + Environment.DIRECTORY_DCIM + "/Camera/" + System.currentTimeMillis() + ".jpg");
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", cameraSavePath);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            uri = Uri.fromFile(cameraSavePath);
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        context.startActivityForResult(intent, 1);
        return cameraSavePath;
    }

    public static void openAlbum(Activity context) {
        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        context.startActivityForResult(intent, 2);
    }

    public static Uri startCrop(Activity context, Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("circleCrop", true);
        intent.putExtra("scale", false);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", ScreenHelper.dp2Px(context, 60));
        intent.putExtra("outputY", ScreenHelper.dp2Px(context, 60));
        intent.putExtra("return-data", false);//是否以 Bitmap形式返回（存在内存中，过大时可能会 OOM）
        File outFile = new File(Environment.getExternalStorageDirectory().getPath()
                + File.separator + Environment.DIRECTORY_DCIM + "/Camera/crop_" + System.currentTimeMillis() + ".jpg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        Uri out = Uri.fromFile(outFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, out);//裁剪后输出路径（保存）
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        context.startActivityForResult(intent, 3);
        return out;
    }

    /**
     * 当前是否为Debug模式
     */
    public static boolean isDebug(Context context) {
        return context.getApplicationInfo() != null
                && (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    public static boolean isMainProcess(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> list = am.getRunningAppProcesses();
        if (list != null) {
            for (ActivityManager.RunningAppProcessInfo info : list) {
                if (Process.myPid() == info.pid && context.getPackageName().equals(info.processName))
                    return true;
            }
        }
        return false;
    }

    public static boolean isRoot() {
        boolean bool;
        if ((!new File("/system/bin/su").exists()) && (!new File("/system/xbin/su").exists())) {
            bool = false;
        } else {
            bool = true;
        }
        return bool;
    }

    public static boolean isLocationEnabled(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm != null)
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        else
            return false;
    }

    public static void settingLocation(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    /**
     * 错误码转换
     *
     * @param errorCode 错误码
     */
    public static String errorCode2String(Context context, String errorCode, String errorMessage) {
        if (TextUtils.isEmpty(errorCode)) {
            return null;
        }
        int id = context.getResources().getIdentifier(errorCode, "string", context.getPackageName());
        if (id != 0) {
            String msg = context.getString(id);
            return TextUtils.isEmpty(msg) ? errorMessage : msg;
        }
        return errorMessage;
    }
}
