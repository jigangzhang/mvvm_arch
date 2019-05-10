package com.god.seep.base.arch.model;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * model类：数据获取操作，remote、local
 * 未添加在生命周期观察者中， 去掉 LifecycleObserver
 * <p>
 * Model 是整个应用的核心。它包含数据以及业务逻辑，网络访问、数据持久化都是它的职责。
 * 用一个公式来表述是这样的：model = data + state + business logic。
 */
public interface IModel extends LifecycleObserver {
    /**
     * 生命周期结束时回收资源，如：置空操作、网络IO操作取消等
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void onDestroy();
}
