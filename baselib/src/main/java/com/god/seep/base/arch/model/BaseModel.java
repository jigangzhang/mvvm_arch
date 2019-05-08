package com.god.seep.base.arch.model;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.model.repository.IRepository;
import com.god.seep.base.net.Api;
import com.god.seep.base.net.BaseObserver;
import com.god.seep.base.net.HttpManager;

import java.util.concurrent.TimeUnit;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Model 管理 Repository， 一个 Model 可能持有多个 Repo
 * 具体的数据获取在 Model 层执行，如请求接口等，向 ViewModel 层提供数据，可做一些简单处理
 * Model 返回一个 LiveData 给 ViewModel 通知信息改变？？ Observer转化成 LiveData？？
 * <p>
 * 考虑去掉 Repository？
 */
public abstract class BaseModel implements IModel {
    //primary repository, 有需要可在实现类中增加 repo类， 或考虑在此持有集合，统一管理
    private IRepository mRepository;
    private CompositeDisposable compositeDisposable;
    protected MutableLiveData<HttpState> httpState;

    public BaseModel(IRepository repository) {
        mRepository = repository;
    }

    public BaseModel() {
    }

    public void setHttpState(MutableLiveData<HttpState> httpState) {
        this.httpState = httpState;
    }

    public void addDisposable(Disposable disposable) {
        if (compositeDisposable == null)
            compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(disposable);
    }

    public void removeDisposable(Disposable disposable) {
        if (compositeDisposable != null)
            compositeDisposable.remove(disposable);
    }

    public void execute(Observable observable, BaseObserver observer) {
        //showLoading
        Disposable dispose = (Disposable) observable
//                .throttleFirst(500, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    //hideLoading
                })
//                .compose()
                .subscribeWith(observer);
        addDisposable(dispose);
    }

    @Override
    public void onDestroy() {
        if (compositeDisposable != null) {
            compositeDisposable.clear();
            compositeDisposable = null;
        }
        if (mRepository != null) {
            mRepository.onDestroy();
            mRepository = null;
        }
    }

    protected <T> T getWebService(Class<T> service) {
        return HttpManager.getRetrofit(Api.Url_WanAndroid).create(service);
    }
}
