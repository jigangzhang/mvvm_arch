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
