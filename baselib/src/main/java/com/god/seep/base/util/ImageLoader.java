package com.god.seep.base.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.god.seep.base.R;

import androidx.annotation.DrawableRes;
import androidx.fragment.app.Fragment;

/**
 * @company: 甘肃诚诚网络技术有限公司
 * @project: ymyc_customer_4.0
 * @package: com.chengcheng.zhuanche.customer.utils
 * @version: V1.0
 * @author: 张吉岗
 * @date: 2018/2/1 11:34
 * @description: <p>
 * <p>
 * Glide 封装
 * </p>
 */

public class ImageLoader {
    /**
     * @param path 本地文件路径
     */
    public static Bitmap transformBitmap(Context context, String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;

//        options.inMutable = true;
        options.inScaled = true;
//        options.inSampleSize = 1080 / App.width;
        int width = ScreenHelper.getScreenWidth(context) / (1080 / 40);
        options.outWidth = width;
        options.outHeight = width * 2;
        bitmap = BitmapFactory.decodeFile(path, options);
        if (bitmap != null)
            bitmap = Bitmap.createScaledBitmap(bitmap, width, width * 2, true);
        return bitmap;
    }

    /**
     * 加载图片--跳过内存缓存，磁盘缓存只使用最终加载结果，并设置占位图、错误结果图
     *
     * @param activity 上下文
     * @param url      图片地址
     * @param view     图片容器
     */
    public static void loadImage(Activity activity, String url, ImageView view) {
//        loadImage(activity, url, view, R.drawable.gif_loading_expect, R.drawable.gif_loading_expect);
    }

    /**
     * 加载图片--跳过内存缓存，磁盘缓存只使用最终加载结果，并设置占位图、错误结果图
     *
     * @param activity    上下文
     * @param url         图片地址
     * @param view        图片容器
     * @param placeHolder 占位图Id
     * @param error       加载错误图Id
     */
    public static void loadImage(Activity activity, String url, ImageView view, int placeHolder, int error) {
        Glide.with(activity)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .skipMemoryCache(true)
                .dontAnimate()
                .centerCrop()
                .placeholder(placeHolder)
                .error(error)
                .into(view);
    }

    /**
     * 加载图片--跳过内存缓存，磁盘缓存只使用最终加载结果，并设置占位图、错误结果图
     *
     * @param fragment 上下文
     * @param url      图片地址
     * @param view     图片容器
     */
    public static void loadImage(Fragment fragment, String url, ImageView view) {
//        loadImage(fragment, url, view, R.drawable.gif_loading_expect, R.drawable.gif_loading_expect);
    }

    /**
     * 加载图片--跳过内存缓存，磁盘缓存只使用最终加载结果，并设置占位图、错误结果图
     *
     * @param fragment    上下文
     * @param url         图片地址
     * @param view        图片容器
     * @param placeHolder 占位图Id
     * @param error       加载错误图Id
     */
    public static void loadImage(Fragment fragment, String url, ImageView view, int placeHolder, int error) {
        Glide.with(fragment)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
                .skipMemoryCache(true)
                .placeholder(placeHolder)
                .error(error)
                .into(view);
    }

    /**
     * 加载图片--跳过内存缓存，磁盘缓存只使用最终加载结果，并设置占位图、错误结果图
     *
     * @param context 上下文
     * @param url     图片地址
     * @param view    图片容器
     */
    public static void loadImage(Context context, String url, ImageView view, boolean centerCrop) {
//        loadImage(context, url, view, centerCrop, null, 0, R.drawable.ic_car_economy_normal);
    }

    public static void loadImage(Context context, String url, ImageView view, BitmapTransformation transform, boolean centerCrop) {
        loadImage(context, url, view, centerCrop, transform, R.color.blue_shadow, R.color.blue_shadow);
    }

    public static void loadImage(Context context, String url, ImageView view, BitmapTransformation transform, @DrawableRes int resId) {
        loadImage(context, url, view, false, transform, resId, 0);
    }

    /**
     * 加载图片--跳过内存缓存，磁盘缓存只使用最终加载结果，并设置占位图、错误结果图
     *
     * @param context     上下文
     * @param url         图片地址
     * @param view        图片容器
     * @param placeHolder 占位图Id
     * @param error       加载错误图Id
     */
    public static void loadImage(Context context, String url, ImageView view, boolean centerCrop, BitmapTransformation transform, int placeHolder, int error) {
        RequestBuilder<Drawable> builder = Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(true);
//        if (transform != null)
//            builder.transition(transform);
        if (centerCrop)
            builder.centerCrop();
        builder.placeholder(placeHolder)
                .error(error)
                .into(view);
    }
}
