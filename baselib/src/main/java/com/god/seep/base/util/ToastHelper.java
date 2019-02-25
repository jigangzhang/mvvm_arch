package com.god.seep.base.util;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class ToastHelper {
    private static Toast mT;

    private ToastHelper() {
    }

    public static void showToast(Context context, String message) {
        showToast(context, message, Toast.LENGTH_SHORT);
    }

    public static void showToast(Context context, String message, int duration) {
        context = context.getApplicationContext();
        if (mT == null) {
            mT = new Toast(context);
            LinearLayout layout = new LinearLayout(context);
//            layout.setBackgroundResource(R.drawable.bg_toast);
            TextView text = new TextView(context);
            text.setMinHeight(ScreenHelper.dp2Px(context, 30));
            int padding = ScreenHelper.dp2Px(context, 8);
            text.setGravity(Gravity.CENTER);
            text.setPadding(padding, padding, padding, padding);
            text.setTextSize(16);
            text.setTextColor(Color.WHITE);
            text.setText(message);
            layout.addView(text);
            mT.setView(layout);
        } else {
            ((TextView) (((LinearLayout) mT.getView()).getChildAt(0))).setText(message);
        }
        mT.setDuration(duration);
        mT.setGravity(Gravity.BOTTOM, 0, ScreenHelper.dp2Px(context, 70));
        mT.show();
    }
}
