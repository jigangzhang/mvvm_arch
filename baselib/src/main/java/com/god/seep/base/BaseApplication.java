package com.god.seep.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.StrictMode;

import com.facebook.stetho.Stetho;
import com.god.seep.base.util.AppUtil;
import com.god.seep.base.util.ExceptionCaught;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;
import com.tencent.bugly.crashreport.CrashReport;

import org.jetbrains.annotations.NotNull;

import androidx.annotation.Nullable;
import androidx.multidex.MultiDexApplication;

import timber.log.Timber;

public class BaseApplication extends MultiDexApplication {
    private static BaseApplication INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        if (!AppUtil.isMainProcess(this)) return;
        INSTANCE = this;
        if (BuildConfig.DEBUG) {
            initStrictMode();
            initLog();
            Stetho.initializeWithDefaults(this);
        } else {
            ExceptionCaught.getInstance(this).init();   //是否要发布到线上版本，接入 Bugly 后可以不需要此项
//            CrashReport.initCrashReport(this, "AppId", false);
        }
    }

    private void initLog() {
        Logger.addLogAdapter(new AndroidLogAdapter() {
            @Override
            public boolean isLoggable(int priority, @Nullable String tag) {
                return BuildConfig.DEBUG;
            }
        });
        Timber.plant(new Timber.DebugTree() {
            @Override
            protected void log(int priority, String tag, @NotNull String message, Throwable t) {
                Logger.log(priority, tag, message, t);
            }
        });
    }

    private void initStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }

    public static BaseApplication getInstance() {
        return INSTANCE;
    }

    public Context getContext() {
        return INSTANCE.getApplicationContext();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Timber.e("application onTerminate");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }
}
