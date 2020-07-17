package com.god.seep.base.arch.view;

import android.content.res.Resources;
import android.util.DisplayMetrics;

public class ResourcesWrapper extends Resources {

    public ResourcesWrapper(Resources res) {
        super(res.getAssets(), res.getDisplayMetrics(), res.getConfiguration());
    }

    @Override
    public DisplayMetrics getDisplayMetrics() {
        DisplayMetrics metrics = super.getDisplayMetrics();
        int targetDensity = metrics.widthPixels / 360;
        metrics.density = targetDensity;
        metrics.densityDpi = targetDensity * 160;
        return metrics;
    }
}
