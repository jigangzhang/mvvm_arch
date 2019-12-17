package com.god.seep.base.arch.model.datasource;

/**
 * 网络状态相关
 */
public enum HttpState {

    /**
     * 开始加载：展示loading
     */
    OnLoading,

    /**
     * 加载完毕：隐藏loading
     */
    OnLoadComplete,

    /**
     * 仅仅只是网络请求成功
     */
    Success,

    /**
     * 登录失效，需要重新登录
     */
    LoginInvalid,

    /**
     * 网络请求失败，包含以下错误之一
     */
    Failure,

    NetError,

    ClientError,

    ServerError,

    UnexpectedError,
}
