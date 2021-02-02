package com.god.seep.media

import com.god.seep.base.BaseApplication
import timber.log.Timber

/**
 * Create Time：2021/1/27 on 17:26
 * Description:
 * Author     :zhangjigang 123
 */
class App : BaseApplication() {
    override fun initInMainProcess() {
        Timber.e("init app")
    }
}