package com.god.seep.base.net;

import android.util.SparseArray;

public class Api {

    public static final int HOST_COUNT = 2;

    public static final String BaseUrl_MaoYan = "http://m.maoyan.com/";
    public static final int Url_MaoYan = 0x0001;

    public static final String BaseUrl_WanAndroid = "https://www.wanandroid.com/";
    public static final int Url_WanAndroid = 0x0010;

    public static final SparseArray<String> BaseUrls = new SparseArray<>(HOST_COUNT);

    static {
        BaseUrls.put(Url_MaoYan, BaseUrl_MaoYan);
        BaseUrls.put(Url_WanAndroid, BaseUrl_WanAndroid);
    }

    public static final String Wan_Chapters = "wxarticle/chapters/json";
}
