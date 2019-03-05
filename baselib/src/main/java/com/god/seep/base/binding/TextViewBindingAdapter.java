package com.god.seep.base.binding;

import android.graphics.Typeface;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.god.seep.base.util.ScreenHelper;

import androidx.databinding.BindingAdapter;


public class TextViewBindingAdapter {

    @BindingAdapter("layoutWeight")
    public static void setLayoutWeight(TextView view, int weight) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(view.getLayoutParams());
        lp.weight = weight;
        lp.setMargins(ScreenHelper.dp2Px(view.getContext(), 15), 0, ScreenHelper.dp2Px(view.getContext(), 15), 0);
        view.setLayoutParams(lp);
    }

    @BindingAdapter("textStyle")
    public static void setTextStyle(TextView view, int style) {
        /*style：0 对应normal， 1 对应 bold*/
        view.setTypeface(Typeface.defaultFromStyle(style));
    }
}
