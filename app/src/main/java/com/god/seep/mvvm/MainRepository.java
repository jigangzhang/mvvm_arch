package com.god.seep.mvvm;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.model.datasource.NetResource;
import com.god.seep.base.arch.model.repository.BaseRemoteRepo;
import com.god.seep.base.net.Api;

import java.util.List;

import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;

public class MainRepository extends BaseRemoteRepo {

    public MainRepository(MutableLiveData<HttpState> httpState) {
        super(httpState);
    }

    public Observable<NetResource<List<Chapter>>> getChapters() {
        Observable execute = execute(getWebService(WebService.class, Api.Url_WanAndroid)
                .getChapters(), true);
        return execute;
//        return getWebService(WebService.class, Api.Url_WanAndroid)
//                .getChapters();
    }
}
