package com.god.seep.base.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.os.Process;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ExceptionCaught implements UncaughtExceptionHandler {
    private static ExceptionCaught sExceptionCaught;
    private Context mContext;

    private ExceptionCaught(Context context) {
        mContext = context;
    }

    public static ExceptionCaught getInstance(Context context) {
        return sExceptionCaught = new ExceptionCaught(context);
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void uncaughtException(final Thread thread, final Throwable ex) {
        File file = initFile();//初始化日志文件
        if (null == thread || null == ex || null == file) {
            Process.killProcess(Process.myPid());
            System.exit(0);
            return;
        }
        StringBuilder sb = new StringBuilder();
        String format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis()));//崩溃发生日期
        long threadId = thread.getId();//线程id
        String threadName = thread.getName();//线程名
        String groupName = null;//线程group name
        if (thread.getThreadGroup() != null) {
            groupName = thread.getThreadGroup().getName();
        }
        sb.append("==========")
                .append(format)
                .append("========")
                .append("\n")
                .append("app version: ")
                .append(AppUtil.getVersionName(mContext))
                .append("\n")
                .append("====ThreadId: ")
                .append(threadId)
                .append("\n")
                .append("====ThreadName: ")
                .append(threadName)
                .append("\n")
                .append("====ThreadGroupName:")
                .append(groupName)
                .append("\n")
                .append("\n");
        Throwable cause = ex;
        while (cause != null) {
            StackTraceElement[] stackTrace = cause.getStackTrace();//堆栈序列
            //拼接崩溃信息
            for (StackTraceElement element : stackTrace) {
                sb.append(element == null ? null : element.toString())//崩溃信息
                        .append("\n");
            }
            sb.append("====message:")
                    .append(cause.getMessage())//异常信息(可能为null)
                    .append("\n")
                    .append("==============分隔线===============")//
                    .append("\n");
            cause = ex.getCause();
        }
        try {
            FileWriter fw = new FileWriter(file, true);
            fw.append(sb.toString());
            fw.flush();
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Process.killProcess(Process.myPid());
        System.exit(0);
    }

    /**
     * 初始化文件
     */
    private File initFile() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return null;
        }
        String logPath = Environment.getExternalStorageDirectory() + File.separator + "ymcxBase.log";
        File file = new File(logPath);
        //文件不存在,则创建文件
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //文件不存在,return
            if (!file.exists()) {
                return null;
            }
        }
        return file;
    }
}