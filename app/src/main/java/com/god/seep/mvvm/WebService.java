package com.god.seep.mvvm;

import com.god.seep.base.arch.model.datasource.NetResource;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import retrofit2.http.GET;

public interface WebService {
    @GET("")
    Observable<NetResource> getHealth();
}
