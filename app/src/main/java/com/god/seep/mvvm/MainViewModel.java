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
        BaseObserver<List<Chapter>> subscribe = mainRepo
                .getChapters()
                .subscribeWith(new BaseObserver<List<Chapter>>(compositeDisposable, getHttpState(), true) {
                    @Override
                    public void onSuccess(List<Chapter> data) {
                        chapterListEvent.postValue(data);
                        Timber.e("result -- %s", data);
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
