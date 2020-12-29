package com.god.seep.base.arch.model.repository;

/**
 * 统一的数据层，给ViewModel提供数据，比如：网络接口数据、本地缓存数据等
 * 子类根据需要可单独实现 Remote或Local 接口，也可以两者都实现
 * 考虑由Repository直接返回LiveData？在ViewModel层对数据做处理？再通知到View层，可通过DataBinding绑定ViewModel的方式？
 */
public interface IRepository {

    /**
     * 回收仓库中的资源
     */
    void onDestroy();
}
