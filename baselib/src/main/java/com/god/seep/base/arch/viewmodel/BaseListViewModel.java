package com.god.seep.base.arch.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;

/**
 * 使用DataBinding 在 xml中调用页面刷新与加载方法
 */
public abstract class BaseListViewModel extends BaseViewModel {
    private int pageIndex = 1;
    private int pageSize = 20;

    public BaseListViewModel(@NonNull Application application) {
        super(application);
    }

    /**
     * 加载更多， smartRefreshLayout
     */
    private void loadMore() {
        //一些操作放到 bindingAdapter中，pageIndex, pageSize等
        // e.g:mBinding.smartRefresh.finishLoadMore(true);
        //      mBinding.smartRefresh.setEnableLoadMore(true);
        getListData(++pageIndex, pageSize);
    }

    /**
     * 刷新
     */
    private void refresh() {
        pageIndex = 1;
        getListData(pageIndex, pageSize);
    }

    abstract void getListData(int pageIndex, int pageSize);
}
