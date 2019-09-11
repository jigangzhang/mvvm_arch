
## Java异常处理

[参考](https://www.jianshu.com/p/eb34c5df30e5)    
    
    经测试，在 Android 的 API 21 ( Android 5.0 ) 以下，Crash 会直接退出应用，但是在 API 21 ( Android 5.0 ) 以上，系统会遵循以下原则进行重启：
        包含 Service，如果应用 Crash 的时候，运行着Service，那么系统会重新启动 Service。
        不包含 Service，只有一个 Activity，那么系统不会重新启动该 Activity。
        不包含 Service，但当前堆栈中存在两个 Activity：Act1 -> Act2，如果 Act2 发生了 Crash ，那么系统会重启 Act1。
        不包含 Service，但是当前堆栈中存在三个 Activity：Act1 -> Act2 -> Act3，如果 Act3 崩溃，那么系统会重启 Act2，并且 Act1 依然存在，即可以从重启的 Act2 回到 Act1。
    
#### 应用Crash时的回调
    
    Thread.setDefaultUncaughtExceptionHandler { t, e ->
        Log.e("tag", "thread:${t.name}")
        e.printStackTrace()
        Process.killProcess(Process.myPid())
        System.exit(0)
    }
    
    如果应用在发生崩溃时，回退栈内依然存在没有退出的Activity，即使调用了System.exit(0)方法，应用依然会自动重启。
    因此我们就需要在应用退出之前，先清除栈内所有的Activity
    自定义一个ActivityManager，可在上述代码中 killProcess 之前，清理栈中所有Activity
   
例子：    
```
class CrashCollectHandler : Thread.UncaughtExceptionHandler {
    var mContext: Context? = null
    var mDefaultHandler:Thread.UncaughtExceptionHandler ?=null
    
    companion object {
        val instance by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { CrashCollectHandler() }
    }
    
    fun init(pContext: Context) {
        this.mContext = pContext
        // 获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }
    
    //当UncaughtException发生时会转入该函数来处理
    override fun uncaughtException(t: Thread?, e: Throwable?) {
        if (!handleException(e) && mDefaultHandler!=null){
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler?.uncaughtException(t,e)
        }else{
            try {
                //给Toast留出时间
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            //退出程序
            App.i.removeAllActivity()
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)

        }

    }
    
    fun handleException(ex: Throwable?):Boolean {
        if (ex == null){
            return false
        }
        Thread{
            Looper.prepare()
            toast("很抱歉,程序出现异常,即将退出")
            Looper.loop()
        }.start()
        //收集设备参数信息
        //collectDeviceInfo(mContext);
        //保存日志文件
        //saveCrashInfo2File(ex);
        // 注：收集设备信息和保存日志文件的代码就没必要在这里贴出来了
        //文中只是提供思路，并不一定必须收集信息和保存日志
        //因为现在大部分的项目里都集成了第三方的崩溃分析日志，如`Bugly` 或 `啄木鸟等`
        //如果自己定制化处理的话，反而比较麻烦和消耗精力，毕竟开发资源是有限的
        return true
    }
}
```    