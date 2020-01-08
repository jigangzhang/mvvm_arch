package com.god.seep.base.arch.viewmodel;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import io.reactivex.disposables.Disposable;

/**
 * ViewModel类：业务相关操作，业务数据，页面相关数据等;  与页面生命周期绑定到一起处理 something
 * <p>
 * ViewModel 也只做两件事。一方面提供 observable properties 给 View 观察，一方面提供 functions 给 View 调用，
 * 通常会导致 observable properties 的改变，以及带来一些额外状态。
 * observable properties 是指 LiveData、Observable(RxJava) 这种可观察属性。
 * View 正是订阅了这些属性，实现模型驱动视图。
 * functions 是指可以用来响应用户操作的方法或其它对象，
 * ViewModel 不会也不应该自己处理业务，它通过 functions 把业务逻辑的处理委派给 Model。
 * 用一个公式来表述是这样的：viewmodel = observable properties + functions。
 */
public interface IViewModel extends LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    void onCreate();

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    void onStart();

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    void onResume();

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    void onPause();

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    void onStop();

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    void onDestroy();

    @OnLifecycleEvent(Lifecycle.Event.ON_ANY)
    void onAny();

    /**
     * 统一的网络请求处理
     */
    void addDisposable(Disposable disposable);

    void removeDisposable(Disposable disposable);
}
