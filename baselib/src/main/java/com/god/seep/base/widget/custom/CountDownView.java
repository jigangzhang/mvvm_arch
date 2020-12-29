package com.god.seep.base.widget.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.CountDownTimer;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.god.seep.base.R;
import com.god.seep.base.util.ScreenHelper;

public class CountDownView extends LinearLayout {
    public static final int TYPE_ONE = 0x001;
    public static final int TYPE_TWO = 0x002;
    private int mainColor;
    private int minorColor;
    private TextView day;
    private TextView hour;
    private TextView minute;
    private TextView second;
    private long remainTime;
    private int currentType = TYPE_ONE;
    private TextView content;   //第二种类型
    private TextView divider1;
    private TextView divider2;
    private boolean isFinished;

    public CountDownView(Context context) {
        this(context, null);
    }

    public CountDownView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CountDownView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CountDownView, defStyleAttr, 0);
        mainColor = typedArray.getColor(R.styleable.CountDownView_mainColor, context.getResources().getColor(R.color.colorAccent));
        minorColor = typedArray.getColor(R.styleable.CountDownView_minorColor, context.getResources().getColor(R.color.white));
        currentType = typedArray.getInt(R.styleable.CountDownView_showType, TYPE_ONE);
        typedArray.recycle();
        setOrientation(LinearLayout.HORIZONTAL);
        if (currentType == TYPE_ONE) {
            init();
        } else {
            initTwo();
        }
    }

    private void init() {
        GradientDrawable minorBg = new GradientDrawable();
        minorBg.setColor(mainColor);
        minorBg.setCornerRadius(ScreenHelper.dp2Px(getContext(), 2));
        minorBg.setShape(GradientDrawable.RECTANGLE);

        setGravity(Gravity.CENTER_VERTICAL);
        day = new TextView(getContext());
        day.setTextColor(mainColor);
        day.setTextSize(13);
        day.getPaint().setFakeBoldText(true);
        addView(day);

        int timePadding = ScreenHelper.dp2Px(getContext(), 0);
        int size = ScreenHelper.dp2Px(getContext(), 17);
        hour = new TextView(getContext());
        LayoutParams lp = new LayoutParams(size, size);
        lp.leftMargin = ScreenHelper.dp2Px(getContext(), 5);
        hour.setLayoutParams(lp);
        hour.setGravity(Gravity.CENTER);
        hour.setTextSize(12);
        hour.setTextColor(minorColor);
        hour.setTypeface(Typeface.DEFAULT_BOLD);
        hour.setPadding(timePadding, 0, timePadding, 0);
        hour.setBackground(minorBg);
        addView(hour);

        LayoutParams dividerLP = new LayoutParams(ScreenHelper.dp2Px(getContext(), 6), ViewGroup.LayoutParams.WRAP_CONTENT);
        divider1 = new TextView(getContext());
        divider1.setLayoutParams(dividerLP);
        divider1.setGravity(Gravity.CENTER);
        divider1.setText(":");
        divider1.setTextSize(12);
        divider1.setTextColor(mainColor);
        divider1.setTypeface(Typeface.DEFAULT_BOLD);
        addView(divider1);

        minute = new TextView(getContext());
        minute.setWidth(size);
        minute.setHeight(size);
        minute.setGravity(Gravity.CENTER);
        minute.setTextSize(12);
        minute.setTextColor(minorColor);
        minute.setTypeface(Typeface.DEFAULT_BOLD);
        minute.setPadding(timePadding, 0, timePadding, 0);
        minute.setBackground(minorBg);
        addView(minute);

        divider2 = new TextView(getContext());
        divider2.setLayoutParams(dividerLP);
        divider2.setGravity(Gravity.CENTER);
        divider2.setText(":");
        divider2.setTextSize(12);
        divider2.setTextColor(mainColor);
        divider2.setTypeface(Typeface.DEFAULT_BOLD);
        addView(divider2);

        second = new TextView(getContext());
        second.setWidth(size);
        second.setHeight(size);
        second.setGravity(Gravity.CENTER);
        second.setTextSize(12);
        second.setTextColor(minorColor);
        second.setTypeface(Typeface.DEFAULT_BOLD);
        second.setPadding(timePadding, 0, timePadding, 0);
        second.setBackground(minorBg);
        addView(second);
    }

    private void initTwo() {
        content = new TextView(getContext());
        content.setTextSize(15);
        content.setTextColor(Color.parseColor("#24282E"));
        content.setTypeface(Typeface.DEFAULT_BOLD);
//        content.getPaint().setFakeBoldText(true);
        addView(content);
    }

    public void setDay(String day) {
        if (day != null)
            this.day.setText(day);
    }

    public void setTime(long remainTime) {
        this.remainTime = remainTime;
        isFinished = false;
        if (timer != null)
            timer.cancel();
        timer = new DownTimer(remainTime, 1000);
        timer.start();
    }

    private DownTimer timer;

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private class DownTimer extends CountDownTimer {
        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public DownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @SuppressLint("DefaultLocale")
        @Override
        public void onTick(long millisUntilFinished) {
            remainTime = millisUntilFinished;
            long seconds = millisUntilFinished / 1000;
            int day = (int) (seconds / (60 * 60 * 24));
            int hour = (int) (seconds / (60 * 60) % 24);
            int minute = (int) (seconds / 60 % 60);
            int second = (int) (seconds % 60);
            String hourToShow;
            if (hour < 10)
                hourToShow = "0" + hour;
            else
                hourToShow = hour + "";
            String minuteToShow;
            if (minute < 10)
                minuteToShow = "0" + minute;
            else
                minuteToShow = minute + "";
            String secondToShow;
            if (second < 10)
                secondToShow = "0" + second;
            else
                secondToShow = second + "";
            if (currentType == TYPE_ONE) {
                if (CountDownView.this.day == null) return;
                CountDownView.this.hour.setVisibility(VISIBLE);
                CountDownView.this.minute.setVisibility(VISIBLE);
                CountDownView.this.second.setVisibility(VISIBLE);
                divider1.setVisibility(VISIBLE);
                divider2.setVisibility(VISIBLE);
                CountDownView.this.day.setTextColor(mainColor);
                CountDownView.this.day.setTextSize(13);
                CountDownView.this.day.setBackgroundResource(0);
                CountDownView.this.day.setPadding(0, 0, 0, 0);
                CountDownView.this.day.setText(String.format("%d天", day));
                CountDownView.this.hour.setText(hourToShow);
                CountDownView.this.minute.setText(minuteToShow);
                CountDownView.this.second.setText(secondToShow);
            } else if (content != null) {
                String dayToShow = day + "";
                String countDownText = "本场倒计时" + day + "天"
                        + hourToShow + "小时" + minuteToShow + "分" + secondToShow + "秒";
                SpannableString span = new SpannableString(countDownText);
                int start = 5;
                int end = start + dayToShow.length();
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end + 1;
                end = start + hourToShow.length();
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end + 2;
                end = start + minuteToShow.length();
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end + 1;
                end = start + secondToShow.length();
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                content.setText(span);
            }
        }

        @Override
        public void onFinish() {
            isFinished = true;
            remainTime = 0;
            if (currentType == TYPE_ONE) {
                if (day == null) return;
                hour.setVisibility(GONE);
                minute.setVisibility(GONE);
                second.setVisibility(GONE);
                divider1.setVisibility(GONE);
                divider2.setVisibility(GONE);
                day.setText("倒计时已结束");
                day.setTextSize(12);
                day.setTextColor(getContext().getResources().getColor(R.color.gray_light));
                day.setBackgroundResource(R.color.gray_cc);
                int paddingHor = ScreenHelper.dp2Px(getContext(), 13);
                int paddingVer = ScreenHelper.dp2Px(getContext(), 1);
                day.setPadding(paddingHor, paddingVer, paddingHor, paddingVer);
            } else if (content != null) {
                String countDownText = "倒计时0天00小时00分00秒";
                SpannableString span = new SpannableString(countDownText);
                int start = 5;
                int end = start + 1;
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end + 1;
                end = start + 2;
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end + 2;
                end = start + 2;
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                start = end + 1;
                end = start + 2;
                span.setSpan(new ForegroundColorSpan(Color.parseColor("#A3211E")),
                        start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                content.setText(span);
                content.setText(countDownText);
            }
        }
    }
}
