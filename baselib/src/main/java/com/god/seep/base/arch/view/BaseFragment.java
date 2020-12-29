package com.god.seep.base.arch.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.god.seep.base.BaseApplication;
import com.god.seep.base.R;
import com.god.seep.base.arch.viewmodel.BaseViewModel;
import com.god.seep.base.util.ToastHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

/**
 * 同BaseActivity的处理，暂缺：网络状态变化的处理、初始化请求数据的空白页面的统一处理、是否需要注册EventBus等
 */
public abstract class BaseFragment<D extends ViewDataBinding, VM extends BaseViewModel> extends Fragment implements IView<VM> {
    protected D mBinding;
    protected VM mViewModel;
    protected Context mContext;
    private View mRootView;
    private Dialog mLoadingDialog;
    protected int requestCount = 0;

    /**
     * /**生命周期： setUserVisibleHint -> onCreate -> onCreateView -> setUserVisibleHint
     * //setUserVisibleHint在 onCreate前调用，故第一个初始化时不能在 setUserVisibleHint中获取数据
     * //fragment 懒加载--> setUserVisibleHint 时设置标识， onStart 时根据标识决定是否加载数据
     * <p>
     * 通过 getUserVisibleHint 结合业务判断是否初始化时加载数据还是启用懒加载
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && mBinding != null)
            lazyLoad();
    }

    /**
     * 懒加载--多 Fragment 切换时使用
     */
    protected void lazyLoad() {

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            //fragment可见
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutId(), container, false);
        mContext = mRootView.getContext();
        mBinding = DataBindingUtil.bind(mRootView);
        initViewModel();
        return mRootView;
    }

    private void initViewModel() {
        mViewModel = createViewModel();
        if (mViewModel == null) {
            //noinspection unchecked
            mViewModel = (VM) getViewModel(BaseViewModel.class);
        }
        getLifecycle().addObserver(mViewModel);
        initEvent();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
    }

    private void initEvent() {
        mViewModel.getLoadingEvent().observe(this, showLoading -> {
                    if (showLoading) {
                        showLoading();
                    } else {
                        hideLoading();
                    }
                }
        );
        mViewModel.getHttpState().observe(this, httpState -> {
            String msg = httpState.getMsg();
            boolean showLoading = httpState.isShowLoading();
            switch (httpState.getState()) {
                case OnLoading:
                    requestCount++;
                    if (showLoading)
                        showLoading();
                    break;
                case OnLoadComplete:
                    requestCount--;
                    if (showLoading)
                        hideLoading();
                    onLoadComplete();
                    break;
                case Success:
                    break;
                case LoginInvalid:
                    loginInvalid(msg);
                    break;
                case Failed:       //Failure包括：接口请求成功，但是返回false；接口请求失败--包括以下情况
                    ToastHelper.showToast(mContext, msg);
                    break;
                case NetError:
                    ToastHelper.showToast(mContext, "网络错误");
                    break;
                case ClientError:
                    ToastHelper.showToast(mContext, "客户端错误");
                    break;
                case ServerError:
                    ToastHelper.showToast(mContext, "服务器错误");
                    break;
                case UnexpectedError:
                    ToastHelper.showToast(mContext, "未知错误");
                    break;
            }
        });
        registerEvent();
    }

    protected void onLoadComplete() {
    }

    @Override
    public void loginInvalid(String errCode) {
        //登录失效，清除数据，跳转登录页
    }

    @Override
    public boolean isBusEnabled() {
        return false;
    }

    protected void showLoading() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new Dialog(getContext());
            mLoadingDialog.getWindow().setBackgroundDrawableResource(R.color.transparent);
//            mLoadingDialog.setContentView(R.layout.dialog_loading);
        }
        if (!mLoadingDialog.isShowing())
            mLoadingDialog.show();
    }

    protected void hideLoading() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    public <T extends ViewModel> T getViewModel(@NonNull Class<T> clz) {
        return getViewModel(clz, null);
    }

    public <T extends ViewModel> T getViewModel(@NonNull Class<T> clz, ViewModelProvider.Factory factory) {
        ViewModelProvider.Factory fac = factory;
        if (fac == null)
            fac = ViewModelProvider.AndroidViewModelFactory.getInstance(BaseApplication.getInstance());
        //内部调用了AndroidViewModelFactory，故不用重写factory，除非需要多个参数
        return new ViewModelProvider(this, fac).get(clz);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mBinding != null)
            mBinding.unbind();
        if (mViewModel != null) {
            getLifecycle().removeObserver(mViewModel);
            mViewModel = null;
        }
        hideLoading();
        mRootView = null;
        mContext = null;
    }
}
