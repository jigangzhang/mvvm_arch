package com.god.seep.base.arch.model.repository;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.net.BaseObserver;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class BaseRemoteRepo implements IRepository.IRemoteRepository {
    private MutableLiveData<HttpState> httpState;
    private CompositeDisposable compositeDisposable;

    public BaseRemoteRepo(MutableLiveData<HttpState> httpState) {
        this.httpState = httpState;
    }

    public LiveData<HttpState> getHttpState() {
        return httpState;
    }

    protected <T> Observable execute(Observable observable, boolean showLoading) {
        //showLoading
        Observable observe = observable
//                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
        Observer observer = observe.doFinally(() -> {
            //hideLoading
        })
//                .compose()
                .subscribeWith(new BaseObserver<T>(httpState, showLoading));
        addDisposable((Disposable) observer);
        return observe;
    }

    protected void addDisposable(Disposable disposable) {
        if (compositeDisposable == null)
            compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(disposable);
    }

    protected void removeDisposable(Disposable disposable) {
        if (compositeDisposable != null)
            compositeDisposable.remove(disposable);
    }

    @Override
    public void onDestroy() {
        Timber.e("Repository destroy");
        if (compositeDisposable != null) {
            compositeDisposable.clear();
            compositeDisposable = null;
        }
    }
}
