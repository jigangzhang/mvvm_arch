package com.god.seep.base.arch.model.datasource;

/**
 * 网络状态相关
 */
public class HttpState {
    public enum State {

        /**
         * 开始加载：展示loading
         */
        OnLoading,

        /**
         * 加载完毕：隐藏loading
         */
        OnLoadComplete,

        /**
         * 仅仅只是接口请求成功，接口请求成功包括：接口返回false--无数据等，登录失效
         */
        Success,

        /**
         * 登录失效，需要重新登录
         */
        LoginInvalid,

        /**
         * 接口请求失败，包括：网络请求失败等，包含以下错误之一
         */
        Failed,

        NetError,

        ClientError,

        ServerError,

        UnexpectedError,
    }

    private State state;
    private String msg;
    private boolean showLoading;

    public HttpState(State state, String msg) {
        this.state = state;
        this.msg = msg;
    }

    public HttpState(State state, boolean showLoading) {
        this.state = state;
        this.showLoading = showLoading;
    }

    //数据加载中
    public static HttpState loading(boolean showLoading) {
        return new HttpState(State.OnLoading, showLoading);
    }

    //数据加载完毕，包括请求失败
    public static HttpState LoadComplete(boolean showLoading) {
        return new HttpState(State.OnLoadComplete, showLoading);
    }

    public static HttpState SUCCESS = new HttpState(State.Success, null);   //接口请求成功

    public static HttpState error(String errorMsg) {
        return error(State.Failed, errorMsg);
    }

    public static HttpState error(State state, String errorMsg) {
        return new HttpState(state, errorMsg);
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getMsg() {
        return msg == null ? "" : msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isShowLoading() {
        return showLoading;
    }

    public void setShowLoading(boolean showLoading) {
        this.showLoading = showLoading;
    }
}
