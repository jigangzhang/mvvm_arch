package com.god.seep.base.update;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;

import com.god.seep.base.util.NetUtil;
import com.god.seep.base.util.ToastHelper;

import java.io.File;

public class DownloadManager {
    private android.app.DownloadManager mDLManager;
    private Context mContext;

    public static DownloadManager getInstance(Context context) {
        return new DownloadManager(context);
    }

    private DownloadManager() {
    }

    private DownloadManager(Context context) {
        mContext = context;
        mDLManager = getManager();
    }

    private android.app.DownloadManager getManager() {
        if (mDLManager == null)
            mDLManager = (android.app.DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        return mDLManager;
    }

    /**
     * @param apkName name apk
     * @return 下载任务Id
     */
    public long startDownLoad(String url, String apkName) {
        android.app.DownloadManager manager = getManager();
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
        request.allowScanningByMediaScanner();//设置文件由MediaScanner扫描
        if (NetUtil.isMobileConnected(mContext))
            ToastHelper.showToast(mContext, "注意：当前为数据流量下载");
        request.setAllowedOverMetered(true);//是否允许流量下载
        request.setDescription("益民出行更新");//下载描述
        request.setMimeType("application/vnd.android.package-archive");
        File file = new File(Environment.getExternalStorageDirectory() + File.separator +
                Environment.DIRECTORY_DOWNLOADS + File.separator + apkName);
        request.setDestinationUri(Uri.fromFile(file));//下载文件外部存储URI
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE);//通知栏可见性
        return manager.enqueue(request);
    }

    /**
     * 返回百分比
     */
    public int queryProcess(long id) {
        int percent = 0;
        android.app.DownloadManager manager = getManager();
        android.app.DownloadManager.Query query = new android.app.DownloadManager.Query();
        Cursor cursor = manager.query(query.setFilterById(id));
        if (cursor != null && cursor.moveToFirst()) {
            int total = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_TOTAL_SIZE_BYTES));//total size
            int progress = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));//已下载大小
            if (total > 0)
                percent = (progress * 100) / total;
        }
        if (cursor != null)
            cursor.close();
        return percent;
    }

    public String queryStatus(long id) {
        String stat = "";
        android.app.DownloadManager manager = getManager();
        android.app.DownloadManager.Query query = new android.app.DownloadManager.Query();
        Cursor cursor = manager.query(query.setFilterById(id));
        if (cursor != null && cursor.moveToFirst()) {
            int status = cursor.getInt(cursor.getColumnIndex(android.app.DownloadManager.COLUMN_STATUS));//状态：成功、暂停、失败
            switch (status) {
                case android.app.DownloadManager.STATUS_FAILED:
                    stat = "下载失败";
                    break;
            }
        }
        if (cursor != null)
            cursor.close();
        return stat;
    }

    public void cancelDownLoad(long id) {
        if (mDLManager != null && id != 0)
            mDLManager.remove(id);
    }
}
