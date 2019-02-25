package com.god.seep.base.arch.model.repository;

import com.god.seep.base.arch.model.datasource.HttpState;

import androidx.lifecycle.LiveData;

public class BaseRemoteRepo implements IRemoteRepository {
    private LiveData<HttpState> httpState;

    public BaseRemoteRepo(LiveData<HttpState> httpState) {
        this.httpState = httpState;
    }

    public LiveData<HttpState> getHttpState() {
        return httpState;
    }

    @Override
    public void onDestroy() {

    }
}
