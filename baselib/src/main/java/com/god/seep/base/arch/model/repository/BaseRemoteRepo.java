package com.god.seep.base.arch.model.repository;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.net.BaseObserver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class BaseRemoteRepo implements IRepository.IRemoteRepository {
    private MutableLiveData<HttpState> httpState;

    public BaseRemoteRepo(MutableLiveData<HttpState> httpState) {
        this.httpState = httpState;
    }

    public MutableLiveData<HttpState> getHttpState() {
        return httpState;
    }

    protected <T> Observable execute(Observable observable, boolean showLoading) {
        //showLoading
        return observable
//                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
//                .unsubscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(result -> {
                    Timber.e("result--- %s", result.toString());
                });
    }

    @Override
    public void onDestroy() {
        Timber.e("Repository destroy");
    }
}
