package com.god.seep.mvvm;

import com.god.seep.base.arch.model.datasource.NetResource;
import com.god.seep.base.net.Api;

import java.util.List;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.http.GET;

public interface WebService {

    @GET(Api.Wan_Chapters)
    Observable<NetResource<List<Chapter>>> getChapters();
}
