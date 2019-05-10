package com.god.seep.base.arch.viewmodel;

import android.app.Application;

import com.god.seep.base.arch.model.BaseModel;
import com.god.seep.base.arch.model.datasource.HttpState;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

/**
 * ViewModel 处理业务数据， 一个 ViewModel 持有一个 Model
 * ViewModel与 View 作双向绑定，所有的业务操作都在此处执行 （View 的一些事件处理也在此处）
 * 具体的数据获取在 Model 层执行，ViewModel 只对数据进行业务相关操作
 * <p>
 * 将 LiveData 下沉至 Model层，直接在 Model层通知数据获取结果。
 * <p>
 * 参考：https://listenzz.github.io/android-lifecyle-works-perfectly-with-rxjava.html
 * <p>
 * ViewModel: 页面销毁重建会触发 onDestroy ？所以 回收资源应该放到 onCleared 中？
 */
public class BaseViewModel<M extends BaseModel> extends AndroidViewModel implements IViewModel {
    private MutableLiveData<Boolean> loadingEvent = new MutableLiveData<>();
    protected MutableLiveData<HttpState> httpState = new MutableLiveData<>();
    protected M mModel;

    public BaseViewModel(@NonNull Application application) {
        super(application);
    }

    public BaseViewModel(@NonNull Application application, M model) {
        super(application);
        this.mModel = model;
        mModel.setHttpState(httpState);
    }

    public MutableLiveData<Boolean> getLoadingEvent() {
        return loadingEvent;
    }

    public MutableLiveData<HttpState> getHttpState() {
        return httpState;
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
        if (mModel != null) {
            mModel.onDestroy();
            mModel = null;
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
