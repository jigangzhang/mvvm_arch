package com.god.seep.mvvm;

import android.app.Application;
import android.view.View;

import com.god.seep.base.arch.model.datasource.NetResource;
import com.god.seep.base.arch.viewmodel.BaseViewModel;
import com.god.seep.base.net.BaseObserver;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import timber.log.Timber;

public class MainViewModel extends BaseViewModel<MainModel> {
    //可将LiveData 传至 Model，直接在 Model 层通知数据获取结果？？
    private MutableLiveData<List<Chapter>> chapterListEvent = new MutableLiveData<>();

    public MainViewModel(@NonNull Application application) {
        super(application, new MainModel());
    }

    public MutableLiveData<List<Chapter>> getChapterListEvent() {
        return chapterListEvent;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        getChapters();
    }

    public void getChapters() {
        showLoading();
        mModel.getChapters(new BaseObserver<NetResource<List<Chapter>>>(httpState) {
            @Override
            public void onNext(NetResource<List<Chapter>> listNetResource) {
                super.onNext(listNetResource);
                chapterListEvent.setValue(listNetResource.getData());
                Timber.e("result -- " + listNetResource);
                hideLoading();
            }
        });
    }

    public void show(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}
