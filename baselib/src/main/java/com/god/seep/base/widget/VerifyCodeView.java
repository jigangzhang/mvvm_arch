package com.god.seep.base.widget;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.god.seep.base.R;

public class VerifyCodeView extends RelativeLayout {
    private EditText editText;
    private TextView[] textViews;
    private View[] lines;//底部光标
    private static int MAX = 4;//4位验证码
    private String inputContent;

    public VerifyCodeView(Context context) {
        this(context, null);
    }

    public VerifyCodeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerifyCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.view_verify_code, this);

        textViews = new TextView[MAX];
        textViews[0] = findViewById(R.id.tv_0);
        textViews[1] = findViewById(R.id.tv_1);
        textViews[2] = findViewById(R.id.tv_2);
        textViews[3] = findViewById(R.id.tv_3);

        lines = new View[MAX];
        lines[0] = findViewById(R.id.vl_0);
        lines[1] = findViewById(R.id.vl_1);
        lines[2] = findViewById(R.id.vl_2);
        lines[3] = findViewById(R.id.vl_3);
        lines[0].setBackgroundColor(getResources().getColor(R.color.blue_shadow));

        editText = findViewById(R.id.edit_text_view);

        editText.setCursorVisible(false);//隐藏光标
        setEditTextListener();
    }

    public EditText getEditText() {
        return editText;
    }

    private void setEditTextListener() {
        editText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                int index = charSequence.length();
                for (View v : lines) {
                    v.setBackgroundColor(getResources().getColor(R.color.gray_text));
                }
                if (index == 4)
                    index = 3;//输入完成不需要光标时直接return
                lines[index].setBackgroundColor(getResources().getColor(R.color.blue_shadow));
            }

            @Override
            public void afterTextChanged(Editable editable) {
                inputContent = editText.getText().toString();

                if (inputCompleteListener != null) {
                    if (inputContent.length() >= MAX) {
                        inputCompleteListener.inputComplete();
                    }
                }

                for (int i = 0; i < MAX; i++) {
                    if (i < inputContent.length()) {
                        textViews[i].setText(String.valueOf(inputContent.charAt(i)));
                    } else {
                        textViews[i].setText("");
                    }
                }
            }
        });
    }

    public void cleanContent() {
        editText.setText("");
    }

    public String getEditContent() {
        return inputContent;
    }

    private InputCompleteListener inputCompleteListener;

    public void setInputCompleteListener(InputCompleteListener inputCompleteListener) {
        this.inputCompleteListener = inputCompleteListener;
    }

    public interface InputCompleteListener {
        void inputComplete();
    }

}
