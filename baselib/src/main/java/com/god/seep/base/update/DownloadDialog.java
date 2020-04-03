package com.god.seep.base.update;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.chengcheng.zhuanche.customer.App;
import com.chengcheng.zhuanche.customer.R;
import com.chengcheng.zhuanche.customer.bean.AppVersionInfo;
import com.chengcheng.zhuanche.customer.databinding.DialogDownloadBinding;
import com.chengcheng.zhuanche.customer.ui.base.BaseDialog;
import com.chengcheng.zhuanche.customer.utils.AppManager;
import com.chengcheng.zhuanche.customer.utils.AppUtil;
import com.chengcheng.zhuanche.customer.utils.PermissionUtil;
import com.chengcheng.zhuanche.customer.utils.ScreenHelper;
import com.chengcheng.zhuanche.customer.utils.ToastHelper;

import java.io.File;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class DownloadDialog extends BaseDialog {
    private DialogDownloadBinding mBinding;
    private AppVersionInfo info;
    private Activity mActivity;
    private boolean isForce;

    public DownloadDialog(@NonNull Activity context, AppVersionInfo versionInfo) {
        super(context, versionInfo.getForceUpdate() == 0);
        this.info = versionInfo;
        this.mActivity = context;
        this.isForce = versionInfo.getForceUpdate() == 1;
    }

    @Override
    public void initPresenter() {

    }

    @Override
    public void onViewCreate(@Nullable Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(getLayoutInflater(), R.layout.dialog_download, null, false);
        mBinding.setView(this);
        this.setContentView(mBinding.getRoot());
        mBinding.setIsForce(isForce);
        mBinding.setVersionInfo(mContext.getString(R.string.app_name) + info.getDeviceChannelVersionName() + "(" + info.getDeviceChannelSize() + "M)");
        mBinding.setVersionDesc(info.getUpdateDesc());
        if (isForce)
            this.setCancelable(false);
        this.setWidth(App.width - ScreenHelper.dp2Px(getContext(), 40));
        String appName = getContext().getString(R.string.app_name) + info.getDeviceChannelVersionName() + ".apk";
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + appName);
        if ((file != null && file.exists()) && file.length() > 0) {
            mBinding.update.setText(getContext().getString(R.string.str_install));
        }
    }

    public void onNegativeEv() {
        dismiss();
        if (isForce) {
            AppManager.getInstance().exit();
        }
    }

    public void onPositive() {
        if (!PermissionUtil.hasStorePermission(getContext())) {
            PermissionUtil.applyStorePermission(mActivity);
            return;
        }
        String appName = getContext().getString(R.string.app_name) + info.getDeviceChannelVersionName() + ".apk";
        File file = new File(Environment.getExternalStorageDirectory()
                + File.separator + Environment.DIRECTORY_DOWNLOADS + File.separator + appName);
        if ((file != null && file.exists()) && file.length() > 0) {
            AppUtil.installApp(getContext(), file);//在此情况下，不同版本uri产生问题
            return;
        }
        final DownloadManager manager = DownloadManager.getInstance(getContext());
        long id = manager.startDownLoad(info.getDeviceChannelUrl(), appName);
        if (id == -1) {
            ToastHelper.showToast(getContext(), "下载失败");
            return;
        }
        if (!isForce) {
            ToastHelper.showToast(getContext(), "正在后台下载...");
            dismiss();
        } else {
            mBinding.update.setText(getContext().getString(R.string.str_install));
            mBinding.update.setTextColor(getContext().getResources().getColor(R.color.gray_disable));
            mBinding.update.setClickable(false);
            mBinding.cancel.setTextColor(getContext().getResources().getColor(R.color.gray_disable));
            mBinding.cancel.setClickable(false);
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String status = manager.queryStatus(id);
                    Message message = Message.obtain();
                    if (status.equals("下载失败")) {
                        message.what = 0x11;
                        message.obj = status;
                        handler.sendMessage(message);
                    } else {
                        int process = manager.queryProcess(id);
                        message.what = 0x12;
                        message.arg1 = process;
                        handler.sendMessage(message);
                        if (process >= 100)
                            timer.cancel();
                    }
                }
            }, 100, 500);
            mBinding.setProgress(0);
            mBinding.progress.setText("0%");
            mBinding.setProVisible(isForce);
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0x11:
                    mBinding.setProgress(100);
                    mBinding.progress.setText(msg.obj.toString());
                    mBinding.update.setText("重新下载");
                    mBinding.update.setTextColor(getContext().getResources().getColor(R.color.blue_bg));
                    mBinding.update.setClickable(true);
                    mBinding.cancel.setTextColor(getContext().getResources().getColor(R.color.gray_light));
                    mBinding.cancel.setClickable(true);
                    break;
                case 0x12:
                    mBinding.setProgress(msg.arg1);
                    mBinding.progress.setText(String.format(Locale.getDefault(), "%d%%", msg.arg1));
                    if (msg.arg1 == 100) {
                        mBinding.update.setTextColor(getContext().getResources().getColor(R.color.blue_bg));
                        mBinding.update.setClickable(true);
                        mBinding.cancel.setTextColor(getContext().getResources().getColor(R.color.gray_light));
                        mBinding.cancel.setClickable(true);
                    }
                    break;
            }
        }
    };
}
