package com.god.seep.base.util;

import android.os.Handler;
import android.os.Looper;

import com.bumptech.glide.Glide;
import com.god.seep.base.BaseApplication;

import java.io.File;
import java.util.Locale;

import timber.log.Timber;

public class CacheManager {
    public static final String VIDEO_CACHE = "video_cache";
    private static CacheManager mInstance;
    private OnClearListener clearListener;

    private CacheManager() {
    }

    public static CacheManager getInstance() {
        if (mInstance == null)
            mInstance = new CacheManager();
        return mInstance;
    }

    public String getCacheSize() {
        BaseApplication application = BaseApplication.getInstance();
        File cacheDir = application.getExternalCacheDir();
        Timber.e("cache dir: %s", cacheDir);
        double size = getFileSize(cacheDir);
        File photoCacheDir = Glide.getPhotoCacheDir(application);
        Timber.e("photo cache size: %s", photoCacheDir);
        size = size + getFileSize(photoCacheDir);
        Timber.e("cache size: %s", size);
        if (size < 1024)
            return String.format(Locale.getDefault(), "%.2fB", size);
        else {
            size = size / 1024;
            if (size < 1024)
                return String.format(Locale.getDefault(), "%.2fKB", size);
            else {
                size = size / 1024;
                return String.format(Locale.getDefault(), "%.2fMB", size);
            }
        }
    }

    public int getFileSize(File file) {
        if (file == null) return 0;
        int size = 0;
        if (file.exists()) {
            File[] files = file.listFiles();
            if (files != null)
                for (File subFile : files) {
                    if (subFile.isFile())
                        size += subFile.length();
                    else if (subFile.isDirectory())
                        size += getFileSize(subFile);
                }
        }
        return size;
    }

    public void clearCache() {
        BaseApplication.getInstance().getThreadPool().submit(() -> {
            BaseApplication application = BaseApplication.getInstance();
            deleteFile(application.getExternalCacheDir());
            Glide.get(application).clearDiskCache();
            if (clearListener != null) {
                new Handler(Looper.getMainLooper()).post(() -> clearListener.onClearFinish());
            }
        });
    }

    public void deleteFile(File file) {
        if (file == null || !file.exists()) return;
        File[] files = file.listFiles();
        if (files != null)
            for (File subFile : files) {
                if (subFile.isFile())
                    subFile.delete();
                else if (subFile.isDirectory())
                    deleteFile(subFile);
            }
    }

    public void setClearListener(OnClearListener clearListener) {
        this.clearListener = clearListener;
    }

    public void removeListener() {
        this.clearListener = null;
    }

    public interface OnClearListener {
        void onClearFinish();
    }
}
