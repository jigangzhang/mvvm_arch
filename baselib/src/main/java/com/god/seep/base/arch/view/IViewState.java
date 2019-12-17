package com.god.seep.base.arch.view;

/**
 * ViewState：根据数据状态作UI操作，如：无数据，无网络等
 */
public interface IViewState {
    /**
     * 无数据，包括：接口返回无数据、网络错误导致无数据等
     */
    void noData();

    void netState(boolean hasNet);
}
