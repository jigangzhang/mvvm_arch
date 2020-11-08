package com.god.seep.base.arch.view;

import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import com.god.seep.base.BaseApplication;
import com.god.seep.base.arch.viewmodel.BaseViewModel;
import com.god.seep.base.receiver.NetworkChangeReceiver;
import com.god.seep.base.util.AppManager;
import com.god.seep.base.util.AppUtil;
import com.god.seep.base.util.ToastHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import timber.log.Timber;

/**
 * 在BaseActivity中要考虑的有：
 * 请求接口时统一的loading处理；
 * 网络状态变化时的处理（弹框、刷新数据等）；
 * 是否要注册EventBus；
 * 初始化请求数据时的空白页面的处理--隐藏与显示；
 */
public abstract class BaseActivity<D extends ViewDataBinding, VM extends BaseViewModel> extends AppCompatActivity implements IView<VM> {
    private NetworkChangeReceiver mNetworkChangeReceiver;
    protected D mBinding;
    protected VM mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBinding();
        initViewModel();
        initData();
        AppManager.getInstance().register(this);
    }

    @Override
    public boolean isBusEnabled() {
        return false;
    }

    private void initBinding() {
        mBinding = DataBindingUtil.setContentView(this, getLayoutId());
    }

    private void initViewModel() {
        mViewModel = createViewModel();
        if (mViewModel == null) {
            //noinspection unchecked
            mViewModel = (VM) getViewModel(BaseViewModel.class);
        }
        getLifecycle().addObserver(mViewModel);
        registerEvent();
    }

    private void registerEvent() {
        mViewModel.getLoadingEvent().observe(this, showLoading -> {
            Timber.e("loading -- %s", showLoading);
            if (showLoading) {
                showLoading();
            } else {
                hideLoading();
            }
        });
        mViewModel.getHttpState().observe(this, httpState -> {
            switch (httpState.getState()) {
                case OnLoading:
                    showLoading();
                    break;
                case OnLoadComplete:
                    hideLoading();
                    break;
                case Success:
                    break;
                case LoginInvalid:
                    loginInvalid(null);
                    break;
                case Failed:   //Failure包括：接口请求成功，但是返回false；接口请求失败--包括以下情况
                    break;
                case NetError:
                    ToastHelper.showToast(BaseActivity.this, "网络错误");
                    break;
                case ClientError:
                    ToastHelper.showToast(BaseActivity.this, "client error");
                    break;
                case ServerError:
                    ToastHelper.showToast(BaseActivity.this, "服务器错误");
                    break;
                case UnexpectedError:
                    ToastHelper.showToast(BaseActivity.this, "未知错误");
                    break;
            }
            Timber.e("net state -- %s", httpState.getState());
        });
    }

    @Override
    public void loginInvalid(String errCode) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNetworkChangeReceiver == null)
            mNetworkChangeReceiver = new NetworkChangeReceiver(this::onNetChanged);
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        registerReceiver(mNetworkChangeReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNetworkChangeReceiver != null)
            unregisterReceiver(mNetworkChangeReceiver);
    }

    public void onNetChanged(boolean hasNet) {
        if (hasNet)
            onNetReConnected();     //暂缺 网络断开又重连的判断
    }

    /**
     * 网络断开又重连时的相关处理--显示对话框、刷新数据等
     */
    public void onNetReConnected() {
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppManager.getInstance().unregister(this);
        if (mBinding != null)
            mBinding.unbind();
        if (mViewModel != null) {
            getLifecycle().removeObserver(mViewModel);
            mViewModel = null;
        }
        hideLoading();
    }

    @Override
    public void finish() {
        AppUtil.hideKeyboardIfNeed(getWindow().getDecorView());
        super.finish();
    }

    protected void showLoading() {

    }

    protected void hideLoading() {

    }

    public <T extends ViewModel> T getViewModel(@NonNull Class<T> clz) {
        return getViewModel(clz, null);
    }

    public <T extends ViewModel> T getViewModel(@NonNull Class<T> clz, ViewModelProvider.Factory factory) {
        ViewModelProvider.Factory fac = factory;
        if (fac == null)
            fac = ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.getInstance());
        return new ViewModelProvider(this, fac).get(clz);   //内部调用了AndroidViewModelFactory，故不用重写factory，除非需要多个参数
    }

    private ResourcesWrapper resources;

    @Override
    public Resources getResources() {
        if (resources == null)
            resources = new ResourcesWrapper(super.getResources());
        if (resources.getConfiguration().fontScale != 1) {
            Configuration configuration = resources.getConfiguration();
            configuration.densityDpi = resources.getDisplayMetrics().densityDpi;
            configuration.fontScale = 1;
            resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        }
        return resources;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Resources resources = getResources();
        newConfig.densityDpi = resources.getDisplayMetrics().densityDpi;
        newConfig.fontScale = 1;
        super.onConfigurationChanged(newConfig);
    }
}
