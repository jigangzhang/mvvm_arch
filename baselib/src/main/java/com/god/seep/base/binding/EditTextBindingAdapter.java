package com.god.seep.base.binding;

import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.widget.EditText;

import com.god.seep.base.R;
import com.god.seep.base.widget.EmojiFilter;

import androidx.databinding.BindingAdapter;
import androidx.databinding.adapters.ListenerUtil;

public class EditTextBindingAdapter {

    @BindingAdapter(value = {"maxLength", "filterSpecial", "filterEmoji", "filterCH"}, requireAll = false)
    public static void setFilter(EditText editText, int maxLength, boolean filterSpecial, boolean filterEmoji, boolean filterCH) {
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength), new EmojiFilter(filterSpecial, filterEmoji, filterCH)});
    }

    @BindingAdapter("onTextChanged")
    public static void setTextWatcher(EditText editText, final OnTextChangedListener listener) {
        TextWatcher newValue;
        if (listener == null) {
            newValue = null;
        } else {
            newValue = new TextWatcher() {
                String old = "";

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (old.equals(s.toString())) {
                        return;
                    } else {
                        old = s.toString();
                    }
                    listener.onTextChanged(s, start, before, count);
                }

                @Override
                public void afterTextChanged(Editable s) {

                }
            };
        }
        TextWatcher oldValue = ListenerUtil.trackListener(editText, newValue, R.id.onTextChanged);
        if (oldValue != null) {
            editText.removeTextChangedListener(oldValue);
        }
        if (newValue != null) {
            editText.addTextChangedListener(newValue);
        }
    }

    public interface OnTextChangedListener {
        void onTextChanged(CharSequence sequence, int start, int before, int count);
    }
}
