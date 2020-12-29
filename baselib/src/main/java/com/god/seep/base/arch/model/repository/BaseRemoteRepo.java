package com.god.seep.base.arch.model.repository;

import com.god.seep.base.arch.model.datasource.HttpState;

import androidx.lifecycle.MutableLiveData;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class BaseRemoteRepo implements CommonRepository.IRemoteRepository {
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
