package com.god.seep.base.arch.model.repository;

import com.god.seep.base.net.HttpManager;

/**
 * 统一的数据层，给ViewModel提供数据，比如：网络接口数据、本地缓存数据等
 * 子类根据需要可单独实现 Remote或Local 接口，也可以两者都实现
 * 考虑由Repository直接返回LiveData？在ViewModel层对数据做处理？再通知到View层，可通过DataBinding绑定ViewModel的方式？
 */
public interface CommonRepository {

    /**
     * 回收仓库中的资源
     */
    void onDestroy();

    /**
     * 远程数据仓库--一般是接口数据
     */
    interface IRemoteRepository extends IRepository {

        /**
         * 远程接口数据
         */
        default <T> T getWebService(Class<T> service, String baseUrl) {
            return HttpManager.getRetrofit(baseUrl).create(service);
        }
    }

    /**
     * 本地数据仓库--数据库、文件等
     */
    interface ILocalRepository extends IRepository {

        /**
         * 做数据库的初始化、或是其他本地存储的初始化
         */
        void init();
    }
}
