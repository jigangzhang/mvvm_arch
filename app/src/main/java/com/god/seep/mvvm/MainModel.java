package com.god.seep.mvvm;

import com.god.seep.base.arch.model.BaseModel;
import com.god.seep.base.net.BaseObserver;

public class MainModel extends BaseModel {

    public MainModel() {
    }

    public void getChapters(BaseObserver observer) {
        execute(getWebService(WebService.class).getChapters(), observer);
    }
}
