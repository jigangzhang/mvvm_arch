package com.god.seep.base.receiver;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.text.TextUtils;

import com.god.seep.base.util.AppUtil;
import com.god.seep.base.util.NetUtil;

import java.io.File;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private OnNetChangeListener mListener;

    public NetworkChangeReceiver(OnNetChangeListener mListener) {
        this.mListener = mListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) return;
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            if (mListener != null)
                mListener.onNetChanged(NetUtil.isNetAvailable(context));
        } else if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (id != -1) {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                Cursor cursor = manager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    String fileName = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    if (fileName != null) {
                        AppUtil.installApp(context, new File(Uri.parse(fileName).getPath()));
                    }
                }
                if (cursor != null)
                    cursor.close();
            }
        }
    }

    public void setOnNetChangeListener(OnNetChangeListener mListener) {
        this.mListener = mListener;
    }

    public interface OnNetChangeListener {
        void onNetChanged(boolean hasNet);
    }
}
