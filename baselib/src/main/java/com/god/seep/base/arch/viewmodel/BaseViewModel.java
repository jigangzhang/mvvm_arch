package com.god.seep.base.arch.viewmodel;

import android.app.Application;

import com.god.seep.base.arch.model.datasource.HttpState;
import com.god.seep.base.arch.model.repository.IRepository;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;

/**
 * ViewModel 处理业务数据， 一个 ViewModel 持有多个数据源
 * ViewModel与 View 作双向绑定，所有的业务操作都在此处执行 （View 的一些事件处理也在此处）
 * 具体的数据获取在 Repo 中执行，ViewModel 只对数据进行业务相关操作
 * <p>
 * 在Repo中返回流，在ViewModel中对数据处理后，再使用 LiveData 将数据通知到View
 * <p>
 * 参考：https://listenzz.github.io/android-lifecyle-works-perfectly-with-rxjava.html
 * <p>
 * ViewModel: 页面销毁重建会触发 onDestroy ？所以 回收资源应该放到 onCleared 中？
 */
public class BaseViewModel extends AndroidViewModel implements IViewModel {
    private MutableLiveData<Boolean> loadingEvent = new MutableLiveData<>();
    protected MutableLiveData<HttpState> httpState = new MutableLiveData<>();
    private List<IRepository> repositories;
    private CompositeDisposable compositeDisposable;

    public BaseViewModel(@NonNull Application application) {
        this(application, null);
    }

    public BaseViewModel(@NonNull Application application, IRepository repository) {
        super(application);
        addRepository(repository);
    }

    public MutableLiveData<Boolean> getLoadingEvent() {
        return loadingEvent;
    }

    public MutableLiveData<HttpState> getHttpState() {
        return httpState;
    }

    /**
     * 在ViewModel中使用Repository时，必须将Repo放入到这个List中，以便统一管理、回收其资源
     */
    protected void addRepository(IRepository repository) {
        if (repository == null) return;
        if (repositories == null)
            repositories = new ArrayList<>();
        repositories.add(repository);
    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onDestroy() {
        if (repositories != null)
            for (IRepository repo : repositories) {
                repo.onDestroy();
            }
        if (compositeDisposable != null) {
            compositeDisposable.clear();
            compositeDisposable = null;
        }
    }

    @Override
    public void onAny() {

    }

    /**
     * 当 Activity 或 Fragment 真正 finish 的时候，框架会调用 ViewModel 中的 onCleared 方法，我们需要在这个方法里面清除不再使用的资源。
     */
    @Override
    protected void onCleared() {
        super.onCleared();
    }

    @Override
    public void addDisposable(Disposable disposable) {
        if (compositeDisposable == null)
            compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(disposable);
    }

    @Override
    public void removeDisposable(Disposable disposable) {
        if (compositeDisposable != null)
            compositeDisposable.remove(disposable);
    }

    protected void showLoading() {
        loadingEvent.postValue(true);
    }

    protected void hideLoading() {
        loadingEvent.postValue(false);
    }

    /**
     * 更新数据网络请求状态
     */
    protected void applyState(HttpState httpState) {
        this.httpState.postValue(httpState);
    }
}
