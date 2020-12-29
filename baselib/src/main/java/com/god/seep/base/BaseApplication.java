package com.god.seep.base;

import android.content.Context;
import android.content.res.Configuration;
import android.os.StrictMode;

import com.alibaba.android.arouter.launcher.ARouter;
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

    static {
        SmartRefreshLayout.setDefaultRefreshHeaderCreator((context, layout) -> {
            layout.setPrimaryColorsId(R.color.colorPrimary, R.color.black_text);//全局设置主题颜色
            return new ClassicsHeader(context);//.setTimeFormat(new DynamicTimeFormat("更新于 %s"));//指定为经典Header，默认是 贝塞尔雷达Header
        });
        SmartRefreshLayout.setDefaultRefreshFooterCreator((context, layout) -> {
            //指定为经典Footer，默认是 BallPulseFooter
            return new ClassicsFooter(context).setDrawableSize(20);
        });
    }

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
        if (BuildConfig.DEBUG) {           // 这两行必须写在init之前，否则这些配置在init过程中将无效
            ARouter.openLog();     // 打印日志
            ARouter.openDebug();   // 开启调试模式(如果在InstantRun模式下运行，必须开启调试模式！线上版本需要关闭,否则有安全风险)
        }
        ARouter.init(this);
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
