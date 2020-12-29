package com.god.seep.base.ui;

import android.net.http.SslError;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.god.seep.base.R;
import com.god.seep.base.arch.view.BaseActivity;
import com.god.seep.base.arch.viewmodel.BaseViewModel;
import com.god.seep.base.databinding.ActivityWebViewBinding;
import com.god.seep.base.util.WebUtil;

public class WebViewActivity extends BaseActivity<ActivityWebViewBinding, BaseViewModel> {

    @Override
    public int getLayoutId() {
        return R.layout.activity_web_view;
    }

    @Override
    public BaseViewModel createViewModel() {
        return null;
    }

    @Override
    public void initData() {
        WebUtil.settings(mBinding.webView);
        String url = getIntent().getStringExtra("KEY_WEB_URL");
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);

        cookieManager.setCookie(url, "token=xxx");
        cookieManager.setCookie(url, "memberId=xxx");
        cookieManager.setCookie(url, "mobile=xxx");
        //Domain=.7gz.com";
        //        String s1 = "Path=/
        cookieManager.flush();

        //webView 调用 js方法
//        mBinding.webView.evaluateJavascript("javascript:app()", null);
        //对象映射，js 调用 原生方法
        mBinding.webView.addJavascriptInterface(new Object() {

            @JavascriptInterface
            public void app_back() {
                if (mBinding.webView.canGoBack()) {
                    mBinding.webView.goBack();
                } else {
                    finish();
                }
            }

            @JavascriptInterface
            public void app_share(String url) {

            }
        }, "android");
        mBinding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                super.onReceivedSslError(view, handler, error);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        mBinding.webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
            }
        });
        mBinding.webView.evaluateJavascript("navigator.userAgent", value -> mBinding.webView.getSettings().setUserAgentString(value + "_agent"));
        if (!TextUtils.isEmpty(url)) {
            if (!url.startsWith("http"))
                url = "https://" + url;
            mBinding.webView.loadUrl(url);
//            mBinding.webView.loadUrl("file:///android_asset/test.html");
        }
    }

    @Override
    public void registerEvent() {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && mBinding.webView.canGoBack()) {
            mBinding.webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBinding.webView.clearCache(true);
        mBinding.webView.clearHistory();
        CookieManager.getInstance().removeAllCookies(null);
    }
}