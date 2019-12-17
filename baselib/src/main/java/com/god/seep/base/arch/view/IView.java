package com.god.seep.base.arch.view;

import com.god.seep.base.arch.viewmodel.BaseViewModel;

import androidx.annotation.LayoutRes;

/**
 * View类：页面相关操作，数据初始化等
 * <p>
 * View 只做两件事情，一件是根据 ViewModel 中存储的状态渲染界面，
 * 另外一件是将用户操作转发给 ViewModel。用一个公式来表述是这样的：view = render(state) + handle(event)。
 * <p>
 * View 包含 Activity 和 Fragment，由于 Activity 和 Fragment 可以销毁后重建，
 * 因此要求 ViewModel 在这个过程中能够存活下来，并绑定到新的 Activity 或 Fragment。
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
     * 登录失效
     */
    void loginInvalid(String errCode);

    /**
     * 是否注册 EventBus
     */
    boolean isBusEnabled();
}
