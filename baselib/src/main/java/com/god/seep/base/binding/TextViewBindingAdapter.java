package com.god.seep.base.binding;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.text.Spanned;
import android.text.TextPaint;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.god.seep.base.BaseApplication;
import com.god.seep.base.util.ScreenHelper;

import androidx.databinding.BindingAdapter;

import java.util.concurrent.ExecutionException;


public class TextViewBindingAdapter {

    @BindingAdapter(value = {"topMargin"}, requireAll = false)
    public static void setMargin(TextView view, float top) {
        ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof LinearLayout.LayoutParams) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) lp;
            layoutParams.setMargins(layoutParams.leftMargin, ScreenHelper.dp2Px(view.getContext(), top), layoutParams.rightMargin, layoutParams.bottomMargin);
            view.setLayoutParams(layoutParams);
        }
    }

    @BindingAdapter("textStyle")
    public static void setTextStyle(TextView view, int style) {
        /*style：0 对应normal， 1 对应 bold*/
        view.setTypeface(Typeface.defaultFromStyle(style));
        view.getPaint().setStrokeWidth(1);
    }

    @BindingAdapter(value = {"fontWeight", "fakeBold"}, requireAll = false)
    public static void setFontWeight(TextView view, float value, boolean fakeBold) {
        TextPaint tp = view.getPaint();
        tp.setFakeBoldText(fakeBold);
//        tp.setStyle(Paint.Style.FILL_AND_STROKE);
//        tp.setStrokeWidth(value);
    }

    @BindingAdapter(value = "middleLine")
    public static void setLines(TextView view, boolean hasMiddleLine) {
        if (hasMiddleLine)
            view.getPaint().setFlags(Paint.ANTI_ALIAS_FLAG | Paint.STRIKE_THRU_TEXT_FLAG);

    }

    @BindingAdapter(value = {"startDrawable", "drawableEnd"}, requireAll = false)
    public static void setCompoundDrawables(TextView view, Drawable resStart, Drawable resEnd) {
        view.setCompoundDrawablesWithIntrinsicBounds(resStart, null, resEnd, null);
    }

    @BindingAdapter(value = "useHtml")
    public static void setHtml(TextView view, String content) {
        if (content != null)
            BaseApplication.getInstance().getThreadPool().execute(() -> {
                Spanned fromHtml = Html.fromHtml(content, source -> {
                    Drawable drawable = null;
                    try {
                        drawable = Glide.with(view).asDrawable().load(source).submit().get();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return drawable;
                }, null);
                if (view != null)
                    new Handler(Looper.getMainLooper()).post(() -> view.setText(fromHtml));
            });
    }
}

