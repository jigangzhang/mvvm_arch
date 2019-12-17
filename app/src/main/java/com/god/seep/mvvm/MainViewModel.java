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
import io.reactivex.Observable;
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
        showLoading();
        Observable<NetResource<List<Chapter>>> observable =
                mainRepo
                        .getChapters()
                        .doOnNext((list) -> {
                            chapterListEvent.setValue(list.getData());
                            Timber.e("result -- %s", list);
                            hideLoading();
                        });
    }

    public void show(View view) {
        Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
}
