package com.god.seep.base.util;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Stack;

public class AppManager {
    private static final String TAG = "AppManager";
    private Stack<AppCompatActivity> mStack;
    private static AppManager mInstance;

    private AppManager() {
        mStack = new Stack<>();
    }

    public static synchronized AppManager getInstance() {
        if (mInstance == null) {
            mInstance = new AppManager();
        }
        return mInstance;
    }

    /**
     * 将Activity加到向量末端
     */
    public void register(AppCompatActivity activity) {
        if (mStack != null)
            mStack.add(activity);
    }

    /**
     * 将Activity从向量中移除
     */
    public void unregister(AppCompatActivity activity) {
        if (mStack != null)
            mStack.remove(activity);
    }

    public AppCompatActivity getTopActivity() {
        int size = mStack.size();
        if (size == 0)
            return null;
        for (int i = size - 1; i >= 0; i--) {
            AppCompatActivity activity = mStack.get(i);
            if (activity != null && !activity.isDestroyed()) {
                return activity;
            }
        }
        return null;
    }

    /**
     * Activity 入栈
     */
    public void push(AppCompatActivity activity) {
        mStack.push(activity);
    }

    /**
     * Activity 出栈
     */
    public void pop() {
        if (!mStack.isEmpty())
            mStack.pop();
    }

    /**
     * 销毁某个 Activity
     */
    public void destroyActivity(Class<?> cls) {
        for (AppCompatActivity activity : mStack) {
            if (cls.getName().equals(activity.getClass().getName())) {
                unregister(activity);
                activity.finish();
                break;
            }
        }
    }

    private void destroyAll() {
        if (mStack != null)
            for (AppCompatActivity activity : mStack) {
                Log.e(TAG, "activity--" + activity.getClass().getName());
                unregister(activity);
                activity.finish();
//            pop();
            }
        mStack = null;
    }

    /**
     * 退出App，杀掉进程
     */
    public void exit() {
        destroyAll();
//        Process.killProcess(Process.myPid());//这种方法退出应用，是会保留某些后进程,例如:Service,Notifications等。
//        System.exit(0);//是将你的整个虚拟机里的内容都停掉,
        // 如果是在第一个 Activity 调用 Process.killProcess 或 System.exit(0) 都会 kill 掉当前进程。但是如果不是在第一个 Activity 中调用，
        // 但是如果不在第一个 Activity 中调用Process.killProcess 或 System.exit(0) 当前进程确实也被 kill 掉了，但 app 会重新启动，又创建了一个新的进程。
//        ActivityManager am = (ActivityManager) App.getInstance().getSystemService(Context.ACTIVITY_SERVICE);
//        am.killBackgroundProcesses(App.getInstance().getPackageName());
        //这种方式退出应用，会结束本应用程序的一切活动,因为本方法会根据应用程序的包名杀死所有进程包括Activity,Service,Notifications等
    }
}
