package com.god.seep.base.arch.view;

import com.god.seep.base.arch.viewmodel.BaseViewModel;

import androidx.annotation.LayoutRes;

/**
 * View类：页面相关操作，数据初始化等
 */
public interface IView<VM extends BaseViewModel> {
    @LayoutRes
    int getLayoutId();

    /**
     * 创建 ViewModel
     */
    VM createViewModel();

    /**
     * 初始化数据
     */
    void initData();

    /**
     * 是否注册 EventBus
     */
    boolean isBusEnabled();
}
