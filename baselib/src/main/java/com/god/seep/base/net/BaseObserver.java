package com.god.seep.base.net;

import com.god.seep.base.arch.model.datasource.HttpState;

import java.io.IOException;

import androidx.lifecycle.MutableLiveData;
import io.reactivex.observers.ResourceObserver;
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
 */
public class BaseObserver<T> extends ResourceObserver<T> {
    private MutableLiveData<HttpState> httpState;

    public BaseObserver(MutableLiveData<HttpState> httpState) {
        this.httpState = httpState;
    }

    @Override
    public void onNext(T t) {
        httpState.postValue(HttpState.Success);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //showLoading
    }

    /**
     * 触发 onError 后不会再触发 onComplete, 所以在此主动调用
     */
    @Override
    public void onError(Throwable e) {
        httpState.postValue(HttpState.Failure);
        Timber.e(e);
        if (e instanceof IOException)
            httpState.postValue(HttpState.NetError);
        else if (e instanceof HttpException)
            httpState.postValue(HttpState.ServerError);
        onComplete();
    }

    @Override
    public void onComplete() {
        //hideLoading
    }
}
