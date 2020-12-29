package com.god.seep.base.net;

import android.text.TextUtils;

import com.facebook.stetho.okhttp3.StethoInterceptor;
import com.god.seep.base.BuildConfig;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

public class HttpManager {
    private static final int READ_TIMEOUT = 10;//单位 秒
    private static final int WRITE_TIMEOUT = 10;
    private static final int CONNECT_TIMEOUT = 60;

    private static final int CACHE_STALE_TIME = 60 * 60 * 24 * 2;//缓存失效 2 days

    private final static HashMap<String, HttpManager> sHttpManager = new HashMap<>();//考虑多域名情况

    private Retrofit mRetrofit;

    public static Retrofit getRetrofit(String baseUrl) {
        HttpManager retrofitManager = sHttpManager.get(baseUrl);
        if (retrofitManager == null) {
            retrofitManager = new HttpManager(baseUrl);
            sHttpManager.put(baseUrl, retrofitManager);
        }
        return retrofitManager.getRetrofit();
    }

    private HttpManager() {
    }

    private HttpManager(String baseUrl) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
//                .sslSocketFactory()//https证书
//                .connectionPool(new ConnectionPool(8, 2, TimeUnit.MINUTES))//连接池，内部有默认实现
//                .cookieJar(new CookieJarImpl())
                .retryOnConnectionFailure(true);    //false会报很多错，重试时报错

        if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor((String message) -> {
                Timber.tag("http");
                Timber.e(message);
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addNetworkInterceptor(loggingInterceptor);
            builder.addNetworkInterceptor(new StethoInterceptor());
        }
        builder.addInterceptor(chain -> {
            //统一处理token，header等
            Request request = chain.request();
            String header = request.header("Authorization");
            String token = "";
            if (TextUtils.isEmpty(header) && !TextUtils.isEmpty(token))
                request = request
                        .newBuilder()
                        .addHeader("Authorization", token)
                        .build();
            return chain.proceed(request);
        });

        mRetrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(baseUrl)
                .client(builder.build())
                .build();
    }

    public Retrofit getRetrofit() {
        return mRetrofit;
    }
}
