package com.god.seep.mvvm;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.model.datasource.NetResource;
import com.god.seep.base.arch.model.repository.BaseRemoteRepo;
import com.god.seep.base.net.Api;

import java.util.List;

import androidx.lifecycle.MutableLiveData;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

public class MainRepository extends BaseRemoteRepo {

    public MainRepository(MutableLiveData<HttpState> httpState) {
        super(httpState);
    }

    public Observable<NetResource<List<Chapter>>> getChapters() {
//        Observable execute = execute(getWebService(WebService.class, Api.Url_WanAndroid)
//                .getChapters(), true);
//        return execute;
        Observable<NetResource<List<Chapter>>> observable = getWebService(WebService.class, Api.Url_WanAndroid)
                .getChapters()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(result -> {
                    Timber.e("do next in repo");
                });
        return observable;
    }
}
