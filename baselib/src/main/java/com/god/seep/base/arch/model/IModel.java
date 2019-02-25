package com.god.seep.base.arch.model;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * model类：数据获取操作，remote、local
 */
public interface IModel extends LifecycleObserver {
    /**
     * 生命周期结束时回收资源，如：置空操作、网络IO操作取消等
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void onDestroy();
}
