package com.god.seep.base.arch.view;

import android.os.Bundle;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.viewmodel.BaseViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelProviders;

public abstract class BaseActivity<D extends ViewDataBinding, VM extends BaseViewModel> extends AppCompatActivity implements IView<VM> {
    protected D mBinding;
    protected VM mViewModel;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initBinding();
        initViewModel();
        initData();
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
        if (mBinding != null)
            mBinding.unbind();
        if (mViewModel != null) {
            getLifecycle().removeObserver(mViewModel);
            mViewModel = null;
        }
        hideLoading();
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
}
