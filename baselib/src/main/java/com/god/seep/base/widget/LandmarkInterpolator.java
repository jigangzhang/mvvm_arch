package com.god.seep.base.widget;

import android.view.animation.Interpolator;

/**
 * @company: 甘肃诚诚网络技术有限公司
 * @project: ymyc_customer_4.0
 * @author: zhangjigang
 * @date: 2018/10/29 11:12
 * @description:
 */
public class LandmarkInterpolator implements Interpolator {
    @Override
    public float getInterpolation(float input) {
        return (float) (Math.sin((input) * Math.PI) / 2.0f);
    }
}
