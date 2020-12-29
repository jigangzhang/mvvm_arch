package com.god.seep.base.util;

import android.annotation.SuppressLint;
import android.os.Build;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 *
 */
public class WebUtil {
    private WebUtil() {
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void settingRichTextWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        //支持自动适配
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        // 把图片加载放在最后来加载渲染
        settings.setBlockNetworkImage(true);
        //放大缩小
        settings.setSupportZoom(false);
        //缩放按钮
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowFileAccess(false);
        settings.setSaveFormData(false);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        //过时算法，可能不起作用
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        //设置不让其跳转浏览器
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                settings.setBlockNetworkImage(false);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        webView.setWebChromeClient(new WebChromeClient());
        //不加这个图片显示不出来
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    public static void settings(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        //支持自动适配
        settings.setUseWideViewPort(true);
//        settings.setLoadWithOverviewMode(true);
        //放大缩小
        settings.setSupportZoom(false);
        //缩放按钮
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setDefaultTextEncodingName("UTF-8");
        settings.setAllowFileAccess(false);
        settings.setSaveFormData(false);
        settings.setDomStorageEnabled(true);
        //过时算法，可能不起作用
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
    }

    /**
     * 使富文本适配屏幕，优化富文本显示
     */
    public static String formatHtml(String html) {
        if (html == null)
            html = "";
        String head = "<head>"
                + "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\"> "
                + "<style>img{max-width: 100%; width:100%; height:auto;}</style>"
                + "<style>video{max-width: 100%; width:100%; height:auto;}</style>"
                + "</head>";
        return "<html>" + head + "<body>" + html + "</body></html>";
    }
}
