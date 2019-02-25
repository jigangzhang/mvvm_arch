package com.god.seep.base;

import android.content.Context;
import android.content.res.Configuration;

import com.facebook.stetho.Stetho;
import com.squareup.leakcanary.LeakCanary;

import androidx.multidex.MultiDexApplication;
import timber.log.Timber;

public class BaseApplication extends MultiDexApplication {
    private static BaseApplication INSTANCE;

    @Override
    public void onCreate() {
        super.onCreate();
        INSTANCE = this;

        if (BuildConfig.DEBUG) {
            Stetho.initializeWithDefaults(this);
            Timber.plant(new Timber.DebugTree());
        }
        if (!LeakCanary.isInAnalyzerProcess(this))
            LeakCanary.install(this);
    }

    public Context getContext() {
        return INSTANCE.getApplicationContext();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
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
