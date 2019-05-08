package com.god.seep.base.net;

import android.util.SparseArray;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.god.seep.base.BuildConfig;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class HttpManager {
    private static final int READ_TIMEOUT = 10;//单位 秒
    private static final int WRITE_TIMEOUT = 10;
    private static final int CONNECT_TIMEOUT = 10;

    private static final int CACHE_STALE_TIME = 60 * 60 * 24 * 2;//缓存失效 2 days

    private static SparseArray<HttpManager> sHttpManager = new SparseArray<>(Api.HOST_COUNT);//考虑多域名情况

    private Retrofit mRetrofit;

    public static Retrofit getRetrofit(int hostType) {
        HttpManager retrofitManager = sHttpManager.get(hostType);
        if (retrofitManager == null) {
            retrofitManager = new HttpManager(hostType);
            sHttpManager.put(hostType, retrofitManager);
        }
        return retrofitManager.getRetrofit();
    }

    private HttpManager() {
    }

    private HttpManager(int hostType) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
//                .sslSocketFactory()//https证书
//                .connectionPool(new ConnectionPool(8, 2, TimeUnit.MINUTES))//连接池，内部有默认实现
//                .cookieJar(new CookieJarImpl())
                .retryOnConnectionFailure(false);

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor((String message) -> {
                Timber.e(message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addNetworkInterceptor(loggingInterceptor);
            builder.addNetworkInterceptor(new StethoInterceptor());
        }

        mRetrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(Api.BaseUrls.get(hostType))
                .client(builder.build())
                .build();
    }

    public Retrofit getRetrofit() {
        return mRetrofit;
    }
}
