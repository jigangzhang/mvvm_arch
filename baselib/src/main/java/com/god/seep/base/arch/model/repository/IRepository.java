package com.god.seep.base.arch.model.repository;

/**
 * 统一的数据业务层
 */
public interface IRepository {
    void onDestroy();

    interface IRemoteRepository extends IRepository {
    }

    interface ILocalRepository extends IRepository {
    }
}
