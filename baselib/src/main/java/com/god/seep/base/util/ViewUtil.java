package com.god.seep.base.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.god.seep.base.R;

public class ViewUtil {
    private static final String TAG_NET_CONTENT = "TagNetContent";

    /**
     * 获取状态栏高度
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public static TextView generateFooterView(Context context, String text) {
        TextView footer = new TextView(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footer.setLayoutParams(lp);
        footer.setPadding(0, ScreenHelper.dp2Px(context, 20), 0, ScreenHelper.dp2Px(context, 20));
        footer.setGravity(Gravity.CENTER);
        footer.setText(text);
        footer.setTextSize(12);
        footer.setBackgroundResource(R.color.gray_bg);
        footer.setTextColor(context.getResources().getColor(R.color.gray_light));
        return footer;
    }

    /**
     * 给图片加圆角
     */
    public static Bitmap convertDtawable(Context context, Bitmap b, int cornerDp) {
        float corner = ScreenHelper.dp2Px(context, cornerDp);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Bitmap bitmap = Bitmap.createBitmap(b.getWidth(), b.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        RectF rectF = new RectF(0, 0, b.getWidth(), b.getHeight());
        //画4个圆角
        canvas.drawRoundRect(rectF, corner, corner, paint);
        //去掉圆角 下方左右两个
        Rect rect = new Rect(0, (int) corner, b.getWidth(), b.getHeight());
        canvas.drawRect(rect, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Rect src = new Rect(0, 0, b.getWidth(), b.getHeight());
        //可通过dst 对bitmap进行缩放
        canvas.drawBitmap(b, src, src, paint);
        return bitmap;
    }

    public static LinearLayout generateEmptyView(Context context, int resId, String desc) {
        return generateEmptyView(context, resId, desc, "", null);
    }

    /**
     * empty view
     */
    public static LinearLayout generateEmptyView(Context context, int resId, String desc, String btnText, View.OnClickListener listener) {
        LinearLayout layout = new LinearLayout(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        layout.setLayoutParams(lp);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setTag(TAG_NET_CONTENT);

        ImageView image = new ImageView(context);
        lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        image.setLayoutParams(lp);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImageResource(resId);
        layout.addView(image);

        TextView footer = new TextView(context);
        footer.setLayoutParams(lp);
        footer.setPadding(0, ScreenHelper.dp2Px(context, 15), 0, ScreenHelper.dp2Px(context, 20));
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setText(desc);
        footer.setTextSize(12);
        footer.setTextColor(context.getResources().getColor(R.color.gray_light));
        layout.addView(footer);

        if (!TextUtils.isEmpty(btnText)) {
            TextView button = new TextView(context);
            lp = new LinearLayout.LayoutParams(ScreenHelper.dp2Px(context, 143), ScreenHelper.dp2Px(context, 41));
            button.setLayoutParams(lp);
            button.setGravity(Gravity.CENTER);
            button.setText(btnText);
            button.setTextSize(16);
//            button.setBackground(context.getResources().getDrawable(R.drawable.bg_btn));
            button.setTextColor(context.getResources().getColor(R.color.white));
            button.setOnClickListener(listener);
            layout.addView(button);
        }
        return layout;
    }

    public static void handleNet(Context context, final ViewGroup viewGroup, Consumer consumer) {
        final Context mContext = context.getApplicationContext();
        View view = viewGroup.getChildAt(0);
        if (view != null && !TAG_NET_CONTENT.equals(view.getTag())) {
            viewGroup.addView(generateEmptyView(mContext, 0,
                    "", "", v -> {
                        if (NetUtil.isNetAvailable(mContext)) {
                            viewGroup.removeViewAt(0);
                            consumer.accept();
                        } else
                            ToastHelper.showToast(mContext, "");
                    }), 0);
        }
    }

    public static void hideView(ViewGroup viewGroup) {
        if (viewGroup == null) return;
        for (int i = 0; i < viewGroup.getChildCount(); i++)
            viewGroup.getChildAt(i).setVisibility(View.GONE);
    }

    public static void showView(ViewGroup viewGroup) {
        if (viewGroup == null) return;
        for (int i = 0; i < viewGroup.getChildCount(); i++)
            viewGroup.getChildAt(i).setVisibility(View.VISIBLE);
    }
}
