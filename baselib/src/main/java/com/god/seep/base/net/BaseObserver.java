package com.god.seep.base.net;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.model.datasource.NetResource;

import java.io.IOException;

import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.observers.ResourceObserver;
import retrofit2.HttpException;
import timber.log.Timber;

/**
 * <p>
 * There are three configurations supported for
 * the Observable, Flowable, Single, Completable and Maybe type parameter:
 * Direct body (e.g., Observable<User>) calls onNext with the deserialized body for 2XX responses
 * and calls onError with HttpException for non-2XX responses and IOException for network errors.
 * <p>
 * Response wrapped body (e.g., Observable<Response<User>>) calls onNext with a Response object for all HTTP responses
 * and calls onError with IOException for network errors
 * <p>
 * Result wrapped body (e.g., Observable<Result<User>>) calls onNext with a Result object for all HTTP responses
 * and errors.
 * <p>
 * cancel 后不会触发 onNext onError onComplete 等
 * 暂只考虑了 Direct body 情况
 * <p>
 * 必须放在UI线程执行，Flowable可实现FlowableSubscriber接口使用
 * </p>
 */
public abstract class BaseObserver<D> extends ResourceObserver<NetResource<D>> {
    private CompositeDisposable disposable;
    private MutableLiveData<HttpState> httpState;
    private boolean showLoading;

    public BaseObserver(CompositeDisposable disposable, MutableLiveData<HttpState> httpState, boolean showLoading) {
        this.disposable = disposable;
        this.httpState = httpState;
        this.showLoading = showLoading;
    }

    @Override
    public void onNext(@NotNull NetResource<D> t) {
        int code = t.getErrorCode();
        switch (code) {
            case 0:
                //
                httpState.setValue(HttpState.SUCCESS);
                onSuccess(t.getData());
                break;
            case 401:
//                LoginManager.getInstance().loginOut();
                httpState.setValue(HttpState.error(HttpState.State.LoginInvalid, "登录失效，请重新登录"));
                break;
            default:
                httpState.setValue(HttpState.error(HttpState.State.Failed, t.getErrorMsg()));
                onFailure(code, t.getErrorMsg());
        }
    }

    public abstract void onSuccess(D data);

    public void onFailure(int code, String message) {
    }

    @Override
    protected void onStart() {
        super.onStart();
        httpState.setValue(HttpState.loading(showLoading));
        if (disposable != null)
            disposable.add(this);
    }

    /**
     * 触发 onError 后不会再触发 onComplete, 所以在此主动调用
     */
    @Override
    public void onError(Throwable e) {
        httpState.setValue(HttpState.error(e.getMessage()));
        if (e instanceof IOException) {
//            if (TextUtils.equals(e.getMessage(), "Canceled")) return;
//            if (TextUtils.equals(e.getMessage(), "Socket closed")) return;
//            if (TextUtils.equals(e.getMessage(), "stream was reset: CANCEL")) return;
            httpState.setValue(HttpState.error(HttpState.State.NetError, e.getMessage()));
            onNetError(e);
            Timber.e(e, "IO 错误，error message：%s", e.getMessage());
        } else if (e instanceof HttpException) {
            int code = ((HttpException) e).code();
            if (code == 401) {
                Timber.e("401 authentication");
            } else if (code >= 400 && code < 500) {
                httpState.setValue(HttpState.error(HttpState.State.ClientError, e.getMessage()));
            } else if (code >= 500 && code < 600) {
                httpState.setValue(HttpState.error(HttpState.State.ServerError, e.getMessage()));
            } else {
                httpState.setValue(HttpState.error(HttpState.State.UnexpectedError, e.getMessage()));
            }
            Timber.e(e, "http code = %s，error message：%s", code, e.getMessage());
        } else {
            httpState.setValue(HttpState.error(HttpState.State.UnexpectedError, e.getMessage()));
            Timber.e(e, "未知错误");
        }
        onComplete();
    }

    protected void onNetError(Throwable e) {
    }

    /**
     * onComplete 在 onNext 之后执行，即是在数据业务处理之后才会隐藏loading
     */
    @Override
    public void onComplete() {
        httpState.setValue(HttpState.LoadComplete(showLoading));
        if (disposable != null)
            disposable.remove(this);
    }
}
