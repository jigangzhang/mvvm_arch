package com.god.seep.base.arch.view;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.viewmodel.BaseViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

public abstract class BaseFragment<D extends ViewDataBinding, VM extends BaseViewModel> extends Fragment implements IView<VM> {
    protected D mBinding;
    protected VM mViewModel;
    protected View mRootView;

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
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutId(), container, false);
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
        registerEvent();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initData();
    }

    private void registerEvent() {
        mViewModel.getLoadingEvent().observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean showLoading) {
                if (showLoading) {
                    showLoading();
                } else {
                    hideLoading();
                }
            }
        });
        mViewModel.getHttpState().observe(this, new Observer<HttpState>() {
            @Override
            public void onChanged(HttpState state) {

            }
        });
    }

    protected void showLoading() {

    }

    protected void hideLoading() {

    }

    public <T extends ViewModel> T getViewModel(@NonNull Class<T> clz) {
        return getViewModel(clz, null);
    }

    public <T extends ViewModel> T getViewModel(@NonNull Class<T> clz, ViewModelProvider.Factory factory) {
        return ViewModelProviders.of(this, factory).get(clz);
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
        mRootView = null;
    }
}
