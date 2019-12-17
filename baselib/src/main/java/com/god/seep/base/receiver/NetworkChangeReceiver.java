package com.god.seep.base.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.text.TextUtils;

import com.god.seep.base.util.NetUtil;

/**
 * @company: 甘肃诚诚网络技术有限公司
 * @project: ymyc_customer_4.0
 * @author: zhangjigang
 * @date: 2018/8/28 16:23
 * @description:
 */
public class NetworkChangeReceiver extends BroadcastReceiver {
    private OnNetChangeListener mListener;

    public NetworkChangeReceiver(OnNetChangeListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) return;
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action))
            if (mListener != null)
                mListener.onNetChanged(NetUtil.isNetAvailable(context));
    }

    public void setOnNetChangeListener(OnNetChangeListener mListener) {
        this.mListener = mListener;
    }

    public interface OnNetChangeListener {
        void onNetChanged(boolean hasNet);
    }
}
