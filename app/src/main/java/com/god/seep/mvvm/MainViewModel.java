package com.god.seep.mvvm;

import android.app.Application;
import android.view.View;

import com.alibaba.android.arouter.launcher.ARouter;
import com.god.seep.base.arch.model.datasource.NetResource;
import com.god.seep.base.arch.viewmodel.BaseViewModel;
import com.god.seep.base.net.BaseObserver;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import timber.log.Timber;

public class MainViewModel extends BaseViewModel {
    private MutableLiveData<List<Chapter>> chapterListEvent = new MutableLiveData<>();
    private MainRepository mainRepo;

    public MainViewModel(@NonNull Application application) {
        super(application);
        mainRepo = new MainRepository(httpState);
        addRepository(mainRepo);
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
        BaseObserver<NetResource<List<Chapter>>> subscribe = mainRepo
                .getChapters()
                .subscribeWith(new BaseObserver<NetResource<List<Chapter>>>(compositeDisposable, getHttpState(), true) {
                    @Override
                    public void onNext(NetResource<List<Chapter>> list) {
                        super.onNext(list);
                        chapterListEvent.postValue(list.getData());
                        Timber.e("result -- %s", list);
                    }
                });
    }

    public void show(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", v ->
                        ARouter.getInstance()
                                .build("/media/activity")
                                .navigation()).show();
    }
}
