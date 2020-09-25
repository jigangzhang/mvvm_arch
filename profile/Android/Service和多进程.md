
## Service和多进程
    
#### Service生命周期简述
    
    ComponentInfo中的processName就是在Manifest中设置的进程名，若未设置，则同applicationInfo.processName
    
    startService时的Service生命周期：
        attach --> onCreate --> onStartCommand/onTaskRemoved（Intent是否taskRemoved） --> onDestroy --> detachAndCleanUp
    bindService时的Service生命周期：
        attach --> onCreate --> onBind/onRebind --> onUnbind --> onDestroy --> detachAndCleanUp
    
    先调用bindService，会触发onCreate，再调用startService，直接走onStartCommand
    
    unbindService只调用 onUnbind，onDestroy在 stop service 时调用，如：直接调用stopService 或 unbindService后需要停止
    
    onUnbind--如果希望稍后在新客户端绑定到服务时调用服务的onRebind(Intent)方法，则返回true   
    
    rebind是由Service的onUnbind方法的返回值决定的，Service第一次绑定时rebind为false，而之后 onUnbind返回true，则再次绑定时 rebind为true
    Service第一次绑定时rebind为false，而之后 onUnbind返回true，则再次绑定时 rebind为true（具体以AMS的unbindFinished为准）
    
    Service的onUnbind返回true，会调用AMS的unbindFinished，其中如果当前Service是在mDestroyingServices列表中，
    或者仅存在当前这一个绑定，则会设置AppBindRecord的doRebind为true，而与当前onUnbind返回值doRebind无关
    
    Service的onBind后会调用到c.conn.connected(r.name, service, false)，最终可能会调用ServiceConnection的onServiceConnected？
    Service的onRebind没有调用publishService，所以不会触发ServiceConnection的onServiceConnected？
    
    c.conn.connected(r.name, service, false)的实现在LoadedApk中：
        若当前Service为新绑定的：
            调用链为 linkToDeath -> onServiceConnected
        若存在旧的绑定，且与新绑定不同：
            调用链为 新 linkToDeath -> 旧 unlinkToDeath -> 旧 onServiceDisconnected -> 新 onServiceConnected
        若新、旧绑定相同，则直接返回
    
    与ANR有关：
        ActivityManager.getService().serviceDoneExecuting
        会在ActivityThread中，当service的onCreate、onBind、onRebind、onUnbind、onDestroy之后调用
        
#### Service的多进程和生命周期相关方法的调用测试

    startService时的Service生命周期：
        attach --> onCreate --> onStartCommand/onTaskRemoved（Intent是否taskRemoved） --> onDestroy --> detachAndCleanUp
    bindService时的Service生命周期：
        attach --> onCreate --> onBind/onRebind --> onUnbind --> onDestroy --> detachAndCleanUp
    
    bindService(Intent service, ServiceConnection conn, int flags)的flags有：
        BIND_AUTO_CREATE：
            只要绑定存在，就自动创建Service。注意，虽然这将创建Service，
            但onStartCommand(Intent,int,int)方法仍然只会在对startService(Intent)的显式调用后才会被调用。
            但是，即使没有它，在创建Service后仍然可以访问Service对象。
            注意，在ICE_CREAM_SANDWICH（14）之前，不提供此标志也会影响系统对目标Service进程的优先级。
            在设置后，它被触发的惟一方法是从服务绑定，在这种情况下，仅当Activity处于前台时才重要。
            现在，要实现此行为，您必须显式地提供新标记BIND_ADJUST_WITH_ACTIVITY。为了兼容性，
            未指定BIND_AUTO_CREATE的旧应用程序将自动设置BIND_WAIVE_PRIORITY和BIND_ADJUST_WITH_ACTIVITY标志，以实现相同的目的。
        BIND_DEBUG_UNBIND：
            包括对不匹配的unbind调用的调试帮助。
            设置此标志后，将保留unbindService(ServiceConnection)调用的调用堆栈，如果稍后进行了不正确的unbind调用，则打印该堆栈。
            请注意，这样做需要保留关于绑定的信息，绑定是在应用程序的生命周期中进行的，从而导致了泄漏——这应该只用于调试
        BIND_NOT_FOREGROUND：
            不允许此绑定将目标Service的进程提升到前台调度优先级。
            它仍然会被提升到至少与Client端相同的内存优先级（这样它的进程在Client端没有被杀死的任何情况下都不会被杀死），
            但是出于CPU调度的目的，它可能会被留在后台。
            这只在绑定Client端是前台进程而目标Service是后台进程的情况下产生影响
        BIND_ABOVE_CLIENT：
            表示绑定到此Service的Client端应用程序认为该Service比应用程序本身更重要。
            设置后，平台将尝试让内存不足杀手杀死应用程序，然后再杀死它所绑定的Service，尽管不能保证确实如此
            （the out of memory killer）
        BIND_ALLOW_OOM_MANAGEMENT：
            允许承载绑定Service的进程进行正常的内存管理。
            它将被视为一个正在运行的服务，允许系统(暂时)删除进程，如果内存不足或其他突发事件，
            它可能会这样做；如果它运行了很长一段时间，它会更积极地让它成为被杀死(和重新启动)的候选对象。
        BIND_WAIVE_PRIORITY：
            不要影响目标Service托管进程的调度或内存管理优先级。
            允许在后台LRU列表中管理Service的进程，就像在后台管理常规应用程序进程一样
        BIND_IMPORTANT：
            此Service对Client端非常重要，因此应该在Client端处于前台进程级别时将其带到前台进程级别。
            通常，进程只能由Client端提升到可见性级别，即使该Client端位于前台
        BIND_ADJUST_WITH_ACTIVITY：
            如果从某个Activity进行绑定，
            则允许根据该Activity是否对用户可见而提高目标Service的进程重要性，
            而不管是否使用另一个标志来减少用于影响该Client端进程的总体重要性的数量
    
    attach、detachAndCleanUp 为内部接口（隐藏方法），不能重写。onStart已经用onStartCommand替代了
    onStartCommand的返回值（是START_CONTINUATION_MASK）有：
        START_STICKY_COMPATIBILITY：
            START_STICKY的兼容版本，它不保证Service被杀死后onStartCommand(Intent,int,int)将再次被调用
        START_STICKY：
            如果该Service的进程在启动时被杀死（从onStartCommand(Intent,int,int)返回之后），则将其保留为启动状态，
            但不保留传递的Intent。 稍后，系统将尝试重新创建服务。 
            因为它处于启动状态，所以它将保证在创建新Service实例后调用onStartCommand(Intent,int,int)；
            如果没有任何待处理的启动命令要传递给Service，则将使用空Intent对象调用该命令，因此您必须注意进行检查。
            对于在任意时间段内将明确启动和停止运行的事物（例如执行背景音乐播放的服务），此模式有意义。
        START_NOT_STICKY：
            如果此Service的进程在启动时被杀死（从onStartCommand(Intent,int,int)返回之后），
            并且没有新的启动Intent可传递给它，则使Service脱离启动状态，直到以后显式调用Context.startService(Intent)时才重新创建。
            该Service将不会收到一个Intent为null的onStartCommand(Intent,int,int)调用，因为如果没有待交付的Intent可以传递，则该Service不会重新启动。
            对于希望由于启动而执行某些工作的事情来说，此模式很有意义，但是可以在内存不足的情况下将其停止，
            并将在稍后显式的重新启动自己以执行更多工作。这样的Service的一个示例是从服务器轮询数据的服务：
            它可以安排闹钟启动Service，使其每N分钟轮询一次闹钟？当从闹钟中调用其onStartCommand(Intent,int,int)时，
            它将在N分钟后安排一个新闹钟，并产生一个线程进行联网。
            如果在执行该检查时杀死了它的进程，则在闹钟响起之前，Service不会重新启动。
        START_REDELIVER_INTENT：
            如果此Service的进程在启动时被杀死（从onStartCommand(Intent,int,int)返回之后），
            则它将被安排重新启动，最后一次传递的Intent将通过onStartCommand(Intent, int, int)重新传递给它。
            此Intent将保持重新传递的安排，直到Service使用提供给onStartCommand(Intent,int,int)的start ID 调用stopSelf(int)，
            该Service将不会收到一个Intent为null的onStartCommand(Intent,int,int)调用，
            因为它只有在没有处理完发送给它的所有Intent时才会重新启动（任何此类挂起事件将在重启时传递）。
            
    IntentService中有对START_NOT_STICKY、START_REDELIVER_INTENT的使用实例
    onStartCommand的参数：
        intent：
         如果Service在其进程被杀死后正在重新启动，
         并且它先前已返回除START_STICKY_COMPATIBILITY以外的任何内容，则该值为null？
         根据上面几项返回值的描述可知，只有在START_STICKY Service重启时才会返回null，后两项不会
        flags：
            0：默认？
            START_FLAG_REDELIVERY：
                如果Intent是对之前传递的Intent的重新传递，
                则在onStartCommand(Intent,int,int)中设置此标志，
                因为Service之前返回了START_REDELIVER_INTENT，但在为该Intent调用stopSelf(int)之前已被杀死。
            START_FLAG_RETRY：
                如果Intent是重试，则在onStartCommand(Intent, int, int)中设置此标志，
                因为最初的尝试从未到达或从onStartCommand返回(Intent, int, int)。
                即之前没有成功执行过onStartCommand                
        startId：
            表示要启动的特定请求的唯一整数。与stopSelfResult(int)一起使用。
            在停止Service时使用，onStartCommand每次传过来的startId不一样，停止Service要使用最近一次传过来的startId
    stopSelfResult(int)：
        此为旧版本，最新的是stopSelf(int startId)
    onTaskRemoved(Intent)：
        如果Service当前正在运行，并且用户删除了来自Service应用程序的任务，则调用此方法。
        如果设置了ServiceInfo.FLAG_STOP_WITH_TASK。那么你将不会收到这个回调；相反，Service将被简单地停止。
    onDestroy()：
        由系统调用，通知Service它不再被使用并且正在被删除。
        此时，Service应该清理它所持有的任何资源(线程、注册的接收者等)。
        在返回时，将不再调用此Service对象，并且它实际上已死亡。不要直接调用这个方法。
        
        没有以 startService 方式开启过 service，即只以 bindService 方式启动 Service时，
        在最后一个连接断开时 （unbindService） service 也同时销毁了
    
    onBind(Intent)：
        将通信通道返回给Service。如果Client不能绑定到Service，可能返回null。
        返回的IBinder通常用于使用aidl描述的复杂接口。
        注意，与其他应用程序组件不同，对这里返回的IBinder接口的调用可能不会发生在进程的主线程上。
        有关主线程的更多信息可以在进程和线程中找到（guide/topics/fundamentals/processes-and-threads.html）。
        Intent：
            Context.bindService的用于绑定到此Service的Intent。
            请注意，此时在Intent中包含的任何其他内容都不会在此处显示。
        返回一个IBinder，Client通过它可以调用Service中的方法。
    onRebind(Intent)：
        在新Client已连接到该Service之后调用，在此之前它已经被通知所有Client在其onUnbind(intent)中都已断开连接。
        只有当onUnbind(Intent)的实现被重写为返回true时，才会调用这个函数
        Intent：
            Context.bindService用于绑定到此Service的Intent。
            请注意，此时在Intent中包含的任何其他内容都不会在此处显示。
    onUnbind(Intent)：
        当所有Client都与Service发布的特定接口断开连接时调用。默认实现不执行任何操作，返回false。
        即当所有连接断开时触发
        Intent：
            Context.bindService用于绑定到此Service的Intent。
            请注意，此时在Intent中包含的任何其他内容都不会在此处显示。
        如果您希望稍后在新Client绑定到该Service时调用onRebind(Intent)方法，则返回true
        
    多进程：
        以 : 开头的进程属于当前应用的私有进程，其他应用组件不可以和它跑在同一进程中
        不以 : 开头，以 . 开头的进程处于全局进程，其他应用通过 ShareUID 方式可以和它跑在同一进程中（相同的UID和签名），共享数据（data目录、内存等）
        每个进程都分配一个独立的虚拟机
        跨进程通信方式：
            通过 Intent、 共享文件、 sharedPreference；
            基于 Binder 的 Messenger 、 AIDL；
            ContentProvider；
            Socket。
       IPC 传输数据时需要序列化 Serializable、Parcelable
       Serializable：
            需要指定 serialVersionUID 在反序列化时 判断是否同一个类
            静态成员变量属于类不属于对象，所以不会参与序列化、 transient 标记的成员变量不参与序列化
            需要大量 I/O 操作，开销大， 适用于 序列化到存储设备，网络传输等
       Parcelable：
            系统实现类有： Intent、 Bundle、 Bitmap等， List、 Map 也可以（需要里面的元素可序列化）
            效率高，使用麻烦，适用于 Android系统
    
       AIDL注册多个 listener 时 使用 RemoteCallbackList 保存， 注销、遍历等操作
       AIDL耗时问题：
            客户端调用远程服务的方法时：
               被调用的方法运行在服务端的 Binder 线程池中，此时客户端线程被挂起，
               若服务端耗时，UI线程会ANR
               客户端的 onServiceConnected、onServiceDisconnected 运行在UI线程中，不可调用服务端耗时方法
               服务端方法运行在 Binder 线程池中，不在开线程
     
            远程服务端调用客户端 listener 方法时：
               被调用的方法运行在客户端的 Binder 线程池中，此时服务端线程被挂起，
               若客户端方法耗时，UI线程会ANR（Service 在 UI线程中），应该在服务端中开线程调用
               客户端listener 方法 运行在 Binder线程池中，不可在其中直接访问 UI相关内容（使用 Handler切换到 UI线程）
     
            远程服务 权限验证：
               onBind中验证：
                   permission 验证：
                       Manifest 中申明权限：
                           <permission android:name="permission-name" android:protectionLevel="normal">
                   onBind 中鉴权：
                       int check = checkCallingOrSelfPermission("permission-name")
                       if (check == DENIED) return null;
                   使用服务时申请权限：
                           <users-permission   android:name="permission-name"/>
               onTransact 中权限验证：
                       验证方式同上： 返回 false
                       采用UID、PID验证：
                           通过getCallingUid、getCallingPid 拿到 Uid、Pid，通过这两个参数验证包名等
                               getPackageManager().getPackagesForUid(getCallingUid()) 等等
    
#### startService/startForegroundService

    使用startService启动Service时，实际会调用 ContextWrapper.startService：
        public ComponentName startService(Intent service) {
            return mBase.startService(service);
        }
    mBase是一个Context，可通过ContextWrapper.attachBaseContext赋予值。在Activity.attach中调用了该方法：attachBaseContext(context)
    Activity中的这个Context是在ActivityThread.performLaunchActivity：
        ...
        ContextImpl appContext = createBaseContextForActivity(r);
        ...
        activity.attach(appContext, this, getInstrumentation(), r.token, r.ident, app, r.intent, r.activityInfo, title, r.parent,
                                r.embeddedID, r.lastNonConfigurationInstances, config, r.referrer, r.voiceInteractor, window, r.configCallback);
        ...
    mBase即是Activity中的Context，其实例就是ContextImpl
    Activity中实现了启动Activity的步骤，若是从Activity之外启动一个Activity，则调用的是ContextImpl的startActivity
    接着看ContextImpl.startService：
        warnIfCallingFromSystemProcess();   //如果系统进程直接调用诸如startService(Intent)之类的方法而不是startServiceAsUser(Intent,UserHandle)，则记录警告。“AsUser”变体使我们能够适当地执行用户的限制。
        return startServiceCommon(service, false, mUser);
    startForegroundService和startService的区别在于第二个参数为true
    
    ContextImpl.startServiceCommon(Intent service, boolean requireForeground, UserHandle user) {
        validateServiceIntent(service); //检查Component、Package是否都为null，即防止隐式启动Service（5.0以上系统）
        service.prepareToLeaveProcess(this);    //准备离开当前App进程，Component为null或PackageName不是当前进程包名 即为离开当前进程
        ComponentName cn = ActivityManager.getService().startService(
            mMainThread.getApplicationThread(), service, service.resolveTypeIfNeeded(
                        getContentResolver()), requireForeground, getOpPackageName(), user.getIdentifier());
        if (cn != null) {
            if (cn.getPackageName().equals("!")) {  //无权限不能启动
                "Not allowed to start service " + service + " without permission " + cn.getClassName());    
            } else if (cn.getPackageName().equals("!!")) {  //不能启动？
                "Unable to start service " + service + ": " + cn.getClassName());
            } else if (cn.getPackageName().equals("?")) {   //不允许启动？
                "Not allowed to start service " + service + ": " + cn.getClassName());
            }
        }
        return cn;
    }
    ActivityManager.getService获取到的是AMS的远程代理？接着看AMS的startService
    ActivityManagerService.startService(IApplicationThread caller, Intent service, String resolvedType, boolean requireForeground, String callingPackage, int userId) throws TransactionTooLargeException {
       enforceNotIsolatedCaller("startService");    //确保不是隔离进程调用
       //拒绝可能泄漏的文件描述符 FD
       if (service != null && service.hasFileDescriptors() == true) {   //Intent中是否包含fd，不能传递fd
           throw new IllegalArgumentException("File descriptors passed in Intent");
       }    
       if (callingPackage == null) {    //当前包名
           throw new IllegalArgumentException("callingPackage cannot be null");
       }
       *** startService: " + service + " type=" + resolvedType + " fg=" + requireForeground //启动Service，MIME类型、是否前台服务
       synchronized(this) {
           final int callingPid = Binder.getCallingPid();   //当前进程的PID？
           final int callingUid = Binder.getCallingUid();   //当前进程的UID？
           final long origId = Binder.clearCallingIdentity();
           ComponentName res;
           try {
               res = mServices.startServiceLocked(caller, service,
                       resolvedType, callingPid, callingUid, requireForeground, callingPackage, userId);
           } finally {
               Binder.restoreCallingIdentity(origId);
           }
           return res;
       }
    }
    
    mServices是ActiveServices的实例
    ActiveServices.startServiceLocked(IApplicationThread caller, Intent service, String resolvedType, int callingPid, int callingUid, boolean fgRequired, String callingPackage, final int userId) throws TransactionTooLargeException {
        final boolean callerFg;
        if (caller != null) {   //ActivityThread中的ApplicationThread，不为null
            final ProcessRecord callerApp = mAm.getRecordForAppLocked(caller);  //在LRU List中找当前进程信息
            if (callerApp == null) {    //未找到
                throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + callingPid + ") when starting service " + service);
            }
            callerFg = callerApp.setSchedGroup != ProcessList.SCHED_GROUP_BACKGROUND;
        } else {
            callerFg = true;    //从前台调用？
        }
        //从ServiceMap中检索是否存到当前Service信息，若不存在调用PackageManagerInternal.resolveService解析Service信息，并添加到ServiceMap中，然后检查权限
        ServiceLookupResult res = retrieveServiceLocked(service, resolvedType, callingPackage, callingPid, callingUid, userId, true, callerFg, false, false);
        if (res == null) {  //service解析失败或未授予权限？
            return null;
        }
        if (res.record == null) {   //未授予权限？    ! 与上面的启动结果对应
            return new ComponentName("!", res.permission != null ? res.permission : "private to package");
        }
        ServiceRecord r = res.record;
        if (!mAm.mUserController.exists(r.userId)) {    //用户是否存在
            Slog.w(TAG, "Trying to start service with non-existent user! " + r.userId);
            return null;
        }
        //如果我们是间接启动（例如从PendingIntent启动），请确定我们是否正在以后台状态启动app。这样就取消了与例如 O +后台服务启动策略的惰性状态。
        final boolean bgLaunch = !mAm.isUidActiveLocked(r.appInfo.uid);     //判断是否后台启动？
        //如果应用程序具有严格的后台限制，则无论其目标SDK版本如何，我们都会将任何bg service视为与传统应用程序强制限制的情况类似
        boolean forcedStandby = false;
        if (bgLaunch && appRestrictedAnyInBackground(r.appInfo.uid, r.packageName)) {
            "Forcing bg-only service start only for " + r.shortName + " : bgLaunch=" + bgLaunch + " callerFg=" + callerFg);
            forcedStandby = true;
        }
        //如果这是直接启动前台服务，请确保根据应用程序操作允许
        boolean forceSilentAbort = false;
        if (fgRequired) {   //前台服务，以startForegroundService方式启动Service
            final int mode = mAm.mAppOpsService.checkOperation(AppOpsManager.OP_START_FOREGROUND, r.appInfo.uid, r.packageName);  //检查操作模式
            switch (mode) {
                case AppOpsManager.MODE_ALLOWED:    //允许
                case AppOpsManager.MODE_DEFAULT:    //默认
                    // All okay.
                    break;
                case AppOpsManager.MODE_IGNORED:    //忽略？
                    //不允许，请退回正常的启动服务，如果后台检查限制了该操作，则无提示地失败
                    Slog.w(TAG, "startForegroundService not allowed due to app op: service " + service + " to " + r.name.flattenToShortString() + " from pid=" + callingPid + " uid=" + callingUid + " pkg=" + callingPackage);
                    fgRequired = false;
                    forceSilentAbort = true;
                    break;
                default:
                    return new ComponentName("!!", "foreground not allowed as per app op");
            }
        }
        //如果这不是直接启动前台服务，请检查我们启动任意服务的能力
        if (forcedStandby || (!r.startRequested && !fgRequired)) {  //startRequested 为是否显示调用
            //在继续之前-如果不允许该应用程序在后台启动服务，那么在这一点上，我们不会让它过时
            final int allowed = mAm.getAppStartModeLocked(r.appInfo.uid, r.packageName, r.appInfo.targetSdkVersion, callingPid, false, false, forcedStandby);
            if (allowed != ActivityManager.APP_START_MODE_NORMAL) {     //不是正常启动，即不允许后台启动？
                "Background start not allowed: service " + service + " to " + r.name.flattenToShortString() + " from pid=" + callingPid + " uid=" + callingUid + " pkg=" + callingPackage + " startFg?=" + fgRequired);
                if (allowed == ActivityManager.APP_START_MODE_DELAYED || forceSilentAbort) {
                    //在这种情况下，我们会静默禁用该应用程序，以尽可能减少现有应用程序的中断
                    return null;
                }
                if (forcedStandby) {
                    //这是O+应用，但我们可能在这里，因为用户已将其置于严格的后台限制之下。如果应用程序试图做正确的事情，请不要对其进行惩罚，但是出于这个原因，我们拒绝了它
                    if (fgRequired) {
                            "Silently dropping foreground service launch due to FAS");  //在严格的后台限制下？取消启动前台服务
                        return null;
                    }
                }
                //这个app知道是在新模型中不允许执行此操作，因此请告诉发生了什么
                UidRecord uidRec = mAm.mActiveUids.get(r.appInfo.uid);
                return new ComponentName("?", "app is in background uid " + uidRec);    //对应上面的启动结果
            }
        }
        //此时，我们基于这是普通的startService()还是startForegroundService()应用了允许启动策略。现在，仅要求应用程序大于8.0？就必须遵守startForegroundService()->startForeground()合同（调用顺序）。
        if (r.appInfo.targetSdkVersion < Build.VERSION_CODES.O && fgRequired) {
            //8.0以下，不是一定要startForegroundService()之后调用startForeground()
            "startForegroundService() but host targets " + r.appInfo.targetSdkVersion + " - not requiring startForeground()");
            fgRequired = false;
        }
        NeededUriGrants neededGrants = mAm.checkGrantUriPermissionFromIntentLocked(   //权限检查？
                        callingUid, r.packageName, service, service.getFlags(), null, r.userId);
        //如果权限需要经过审查才能运行任何应用程序组件，我们将不启动服务并启动审查activity，如果调用的应用程序位于前台，则在审查完成后将其传递给启动服务的pendingIntent
        if (mAm.mPermissionReviewRequired) {    //需要权限检查
            //XXX 这与fgRequired无关！
            if (!requestStartTargetPermissionsReviewIfNeededLocked(r, callingPackage, callingUid, service, callerFg, userId)) {
                return null;
            }
        }
        if (unscheduleServiceRestartLocked(r, callingUid, false)) {
            "START SERVICE WHILE RESTART PENDING: " + r
        }
        r.lastActivity = SystemClock.uptimeMillis();
        r.startRequested = true;
        r.delayedStop = false;
        r.fgRequired = fgRequired;
        r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(),
                service, neededGrants, callingUid));
        if (fgRequired) {
            //我们现在正在有效地运行前台服务
            mAm.mAppOpsService.startOperation(AppOpsManager.getToken(mAm.mAppOpsService), AppOpsManager.OP_START_FOREGROUND, r.appInfo.uid, r.packageName, true);
        }
        final ServiceMap smap = getServiceMapLocked(r.userId);  //拿到保存Service信息的map（解析后的Service信息）
        boolean addToStarting = false;
        if (!callerFg && !fgRequired && r.app == null && mAm.mUserController.hasStartedUserState(r.userId)) {
            ProcessRecord proc = mAm.getProcessRecordLocked(r.processName, r.appInfo.uid, false);   //找出进程信息
            if (proc == null || proc.curProcState > ActivityManager.PROCESS_STATE_RECEIVER) {   //与广播的启动有关？
                //如果这不是来自前台调用者，那么如果已经有其他后台服务正在启动，我们可能希望延迟启动。 当许多应用程序都在处理诸如连接广播之类的事情时，这是为了避免进程启动垃圾邮件。 
                //我们仅对高速缓存的进程执行此操作，因为否则应用程序可能具有以下假设：调用startService（）使服务在其自己的进程中运行，并且该进程在服务启动之前不会被杀死。 对于接收者而言尤其如此，接收者可以在onReceive（）中启动服务以执行一些其他工作，并已初始化了一些全局状态。
                "Potential start delay of " + r + " in " + proc);
                if (r.delayed) {
                    //该服务已被安排为延迟启动；只是让它还在等待
                    "Continuing to delay: " + r
                    return r.name;
                }
                if (smap.mStartingBackground.size() >= mMaxStartingBackground) {  //正在后台启动的service大于最大限制
                    //其他事情正在开始，延迟！
                    "Delaying start of: " + r
                    smap.mDelayedStartList.add(r);      //加到延迟启动列表中
                    r.delayed = true;
                    return r.name;
                }
                "Not delaying: " + r        //没有延迟
                addToStarting = true;
            } else if (proc.curProcState >= ActivityManager.PROCESS_STATE_SERVICE) {    //service的状态
                //当我们将这个新服务加入等待中的后台启动服务时，我们会稍微放松一些，以包括当前正在运行其他服务或接收器的进程
                addToStarting = true;
                "Not delaying, but counting as bg: " + r);
            } else if (DEBUG_DELAYED_STARTS) {  //debug延迟启动
            }
        } else if (DEBUG_DELAYED_STARTS) {
            if (callerFg || fgRequired) {
                "Not potential delay (callerFg=" + callerFg + " uid=" + callingUid + " pid=" + callingPid + " fgRequired=" + fgRequired + "): " + r);
            } else if (r.app != null) {
                "Not potential delay (cur app=" + r.app + "): " + r);
            } else {
                "Not potential delay (user " + r.userId + " not started): " + r);
            }
        }
        ComponentName cmp = startServiceInnerLocked(smap, service, r, callerFg, addToStarting);
        return cmp;
    }
    在Service解析、权限检查等一系列的操作之后（包括前台和后台启动service的限制，以及延迟启动service的情况），调用startServiceInnerLocked启动Service
    
    ActiveServices.startServiceInnerLocked(ServiceMap smap, Intent service, ServiceRecord r, boolean callerFg, boolean addToStarting) throws TransactionTooLargeException {
        ServiceState stracker = r.getTracker();
        if (stracker != null) {
            stracker.setStarted(true, mAm.mProcessStats.getMemFactorLocked(), r.lastActivity);
        }
        r.callStart = false;
        synchronized (r.stats.getBatteryStats()) {
           r.stats.startRunningLocked();
        }
        String error = bringUpServiceLocked(r, service.getFlags(), callerFg, false, false);  //启动Service
        if (error != null) {
            return new ComponentName("!!", error);      //启动失败
        }
        if (r.startRequested && addToStarting) {    //显示启动且加入到延迟加载列表中？
            boolean first = smap.mStartingBackground.size() == 0;   //是否是后台启动列表中的第一个？
            smap.mStartingBackground.add(r);
            r.startingBgTimeout = SystemClock.uptimeMillis() + mAm.mConstants.BG_START_TIMEOUT;
            if (first) {
                smap.rescheduleDelayedStartsLocked();   //将mDelayedStartList中的Service启动--调用startServiceInnerLocked
            }
        } else if (callerFg || r.fgRequired) {  //前台调用？或是服务启动后需要进入前台？
            smap.ensureNotStartingBackgroundLocked(r);  //从后台和延迟列表中移除Service
        }
        return r.name;
    }
    调用bringUpServiceLocked启动Service，若无error，则将该Service从延迟列表中启动或移除
    
    ActiveServices.bringUpServiceLocked(ServiceRecord r, int intentFlags, boolean execInFg, boolean whileRestarting, boolean permissionsReviewRequired)：
        if (r.app != null && r.app.thread != null) {    //ServiceRecord中的ProcessRecord和ApplicationThread不为null，表示该Service已经被启动了，只需调用Service的onStartCommand方法
            sendServiceArgsLocked(r, execInFg, false);  //最终会转入ActivityThread调用到Service.onStartCommand方法
            return null;
        }
        if (!whileRestarting && mRestartingServices.contains(r)) {  //当前不是重启，并且重启的列表中包含当前Service，则返回等待重启
            //如果等待重启，则什么也不做
            return null;
        }
        //现在，我们正在启动服务，因此不再处于重新启动状态
        if (mRestartingServices.remove(r)) {    //结合上面，不论是正在重启或重启列表中包含Service，都要移除
            clearRestartingIfNeededLocked(r);   //清除跟踪器的重启状态？
        }
        //确保此服务不再被视为延迟，我们现在开始启动
        if (r.delayed) {
            getServiceMapLocked(r.userId).mDelayedStartList.remove(r);  //从mDelayedStartList中移除
            r.delayed = false;
        }
        //确保拥有此服务的用户已启动。 如果没有，我们不想让它运行
        if (!mAm.mUserController.hasStartedUserState(r.userId)) {
            String msg = "Unable to launch app " + r.appInfo.packageName + "/" + r.appInfo.uid + " for service " + r.intent.getIntent() + ": user " + r.userId + " is stopped";
            bringDownServiceLocked(r);  //向所有连接（ConnectionRecord）报告Service不可用，结束Service启动，一些参数复位
            return msg;
        }
        //服务正在启动，它的包无法停止
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(r.packageName, false, r.userId);  //使包不能停止？
        } catch (IllegalArgumentException e) {
            "Failed trying to unstop package " + r.packageName + ": " + e);
        }
        final boolean isolated = (r.serviceInfo.flags&ServiceInfo.FLAG_ISOLATED_PROCESS) != 0;  //独立进程？隔离进程？
        final String procName = r.processName;  //进程名
        String hostingType = "service";
        ProcessRecord app;  //进程信息
        if (!isolated) {    //非隔离进程
            app = mAm.getProcessRecordLocked(procName, r.appInfo.uid, false);   //获取进程信息
            "bringUpServiceLocked: appInfo.uid=" + r.appInfo.uid + " app=" + app);
            if (app != null && app.thread != null) {    //进程已存在
                try {
                    app.addPackage(r.appInfo.packageName, r.appInfo.longVersionCode, mAm.mProcessStats);    //添加到pkg集合中
                    realStartServiceLocked(r, app, execInFg);   //真正的启动Service
                    return null;    //成功启动，返回
                } catch (RemoteException e) {
                    "Exception when starting service " + r.shortName, e);
                }
                //如果抛出了死对象异常，请重新启动应用程序
            }
        } else {    //是隔离进程
            //如果此服务在隔离进程中运行，则每次调用startProcessLocked()时，我们都会获得一个新的隔离的进程，如果当前正在等待上一个进程，则将启动另一个进程。为了解决这个问题，我们在服务中存储它正在运行或正在等待出现的任何当前隔离进程
            app = r.isolatedProc;
            if (WebViewZygote.isMultiprocessEnabled() && r.serviceInfo.packageName.equals(WebViewZygote.getPackageName())) {
                hostingType = "webview_service";        //WebView的进程？
            }
        }
        //后续都是Service未成功运行
        //未运行-启动它，并排队该服务记录以在应用启动时执行
        if (app == null && !permissionsReviewRequired) {    //进程未启动？且不需要权限Review
            if ((app=mAm.startProcessLocked(procName, r.appInfo, true, intentFlags, hostingType, r.name, false, isolated, false)) == null) { //启动进程，但启动失败？
                "Unable to launch app " + r.appInfo.packageName + "/" + r.appInfo.uid + " for service " + r.intent.getIntent() + ": process is bad";
                bringDownServiceLocked(r);  //结束启动Service？
                return msg;
            }
            if (isolated) {
                r.isolatedProc = app;   //是隔离进程，则给当前Service赋予其独立进程
            }
        }
        if (r.fgRequired) { //前台运行？
            "Whitelisting " + UserHandle.formatUid(r.appInfo.uid) + " for fg-service launch");
            mAm.tempWhitelistUidLocked(r.appInfo.uid, SERVICE_START_FOREGROUND_TIMEOUT, "fg-service-launch");   //将targetUid列入白名单以暂时绕过省电模式
        }
        if (!mPendingServices.contains(r)) {    //添加到挂起待运行的Service
            mPendingServices.add(r);
        }
        if (r.delayedStop) {
            //哦，嘿，我们已经被要求停止！
            r.delayedStop = false;
            if (r.startRequested) { //显示启动
                "Applying delayed stop (in bring up): " + r);   //应用延迟停止(在调出时)
                stopServiceLocked(r);
            }
        }

        return null;
    若Service已经被启动了，则通过sendServiceArgsLocked最后调用onStartCommand，否则通过一系列的可启动判断后，且不是在隔离进程中运行，则调用realStartServiceLocked
    
    ActiveServices.realStartServiceLocked(ServiceRecord r, ProcessRecord app, boolean execInFg)：
        if (app.thread == null) {   //进程未启动？
            throw new RemoteException();
        }
        r.app = app;    //下次startService时，会判断该项，以决定Service是否已启动
        r.restartTime = r.lastActivity = SystemClock.uptimeMillis();    //Service启动时间？
        final boolean newService = app.services.add(r);     //添加到进程信息的Service列表中，返回是否是新Service
        bumpServiceExecutingLocked(r, execInFg, "create");  //给r中的一些参数赋值
        mAm.updateLruProcessLocked(app, false, null);       //更新LRU列表
        updateServiceForegroundLocked(r.app, /* oomAdj= */ false);  //更新前台Service？
        mAm.updateOomAdjLocked();
        boolean created = false;
        try {
            synchronized (r.stats.getBatteryStats()) {
                r.stats.startLaunchedLocked();
            }
            mAm.notifyPackageUse(r.serviceInfo.packageName, PackageManager.NOTIFY_PACKAGE_USE_SERVICE);
            app.forceProcessStateUpTo(ActivityManager.PROCESS_STATE_SERVICE);   //强制更新进程状态为Service
            app.thread.scheduleCreateService(r, r.serviceInfo,      //在ActivityThread中创建Service，会调用到onCreate
                    mAm.compatibilityInfoForPackageLocked(r.serviceInfo.applicationInfo), app.repProcState);
            r.postNotification();       //发通知？
            created = true;     //Service创建完毕
        } catch (DeadObjectException e) {
            "Application dead when creating service " + r);
            mAm.appDiedLocked(app);    //结束进程？
            throw e;
        } finally {
            if (!created) {    //没有创建成功
                //保持executeNesting计数准确
                final boolean inDestroying = mDestroyingServices.contains(r);  //已销毁的Service中包含Service？
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);     //计数？
                //清除
                if (newService) {  //新Service
                    app.services.remove(r);    //移除
                    r.app = null;
                }
                //重试
                if (!inDestroying) {   //没有正在销毁
                    scheduleServiceRestartLocked(r, false);     //重启Service
                }
            }
        }
        if (r.whitelistManager) {       //白名单？
            app.whitelistManager = true;
        }
        requestServiceBindingsLocked(r, execInFg);      //遍历r的bindings列表，请求bindService？执行ActivityThread中的scheduleBindService
        updateServiceClientActivitiesLocked(app, null, true);   //与bind有关？更新Activity的LRU？
        //如果服务处于启动状态，并且没有挂起的参数，则伪造一个参数，以便调用其onStartCommand()
        if (r.startRequested && r.callStart && r.pendingStarts.size() == 0) {
            r.pendingStarts.add(new ServiceRecord.StartItem(r, false, r.makeNextStartId(), null, null, 0));
        }
        sendServiceArgsLocked(r, execInFg, true);   //发送参数，以在ActivityThread中调用onStartCommand()
        if (r.delayed) {    //我们是否在后台等待启动此服务？
            getServiceMapLocked(r.userId).mDelayedStartList.remove(r);  //移除
            r.delayed = false;  //不再等待了
        }
        if (r.delayedStop) {    //服务已经停止，但是否正在延迟启动?
            //哦，嘿，我们已经被要求停止！
            r.delayedStop = false;  //取消延迟启动？
            if (r.startRequested) {     //有人显式地调用了start?（startService）
                "Applying delayed stop (from start): " + r);    //应用延迟停止(从开始)
                stopServiceLocked(r);   //停止Service
            }
        }
    Service的启动，是由SystemServer进程转入App进程调用ActivityThread中的scheduleCreateService创建Service
    若Service创建成功，会通过requestServiceBindingsLocked  bindService？以及使用sendServiceArgsLocked，调用onStartCommand()
    
    ApplicationThread.scheduleCreateService(IBinder token, ServiceInfo info, CompatibilityInfo compatInfo, int processState)：
        updateProcessState(processState, false);    //更新processState
        CreateServiceData s = new CreateServiceData();
        s.token = token;
        s.info = info;
        s.compatInfo = compatInfo;
        sendMessage(H.CREATE_SERVICE, s);   //handler--ActivityThread.mH
    ActivityThread.H.handleMessage：
        case CREATE_SERVICE:
            "serviceCreate: " + String.valueOf(msg.obj)));
            handleCreateService((CreateServiceData)msg.obj);
            break;
    ActivityThread.handleCreateService(CreateServiceData data)：
        //如果我们准备在后台运行后准备使用gc，那么我们又恢复了活动状态，因此请跳过它
        unscheduleGcIdler();        //跳过后台GC？
        LoadedApk packageInfo = getPackageInfoNoCheck(data.info.applicationInfo, data.compatInfo);  //获取APK信息？
        Service service = null;
        try {
            java.lang.ClassLoader cl = packageInfo.getClassLoader();
            service = packageInfo.getAppFactory().instantiateService(cl, data.info.name, data.intent);  //实例化Service
        } catch (Exception e) {
            if (!mInstrumentation.onException(service, e)) {
                throw new RuntimeException("Unable to instantiate service " + data.info.name + ": " + e.toString(), e);
            }
        }
        try {
            "Creating service " + data.info.name);
            ContextImpl context = ContextImpl.createAppContext(this, packageInfo);  //同Activity中的Context创建
            context.setOuterContext(service);
            Application app = packageInfo.makeApplication(false, mInstrumentation); //返回以经创建好的Application对象
            service.attach(context, this, data.info.name, data.token, app,
                    ActivityManager.getService());  //类似Activity的attach方法，给一些参数赋值，并且调用attachBaseContext(context)，给ContextWrapper的mBase赋值
            service.onCreate();     //onCreate
            mServices.put(data.token, service);     //添加到集合中
            try {
                ActivityManager.getService().serviceDoneExecuting(data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);     //执行完毕，是否要销毁等
            } 
        } catch (Exception e) {
            if (!mInstrumentation.onException(service, e)) {
                throw new RuntimeException("Unable to create service " + data.info.name + ": " + e.toString(), e);
            }
        }
    在ActivityThread中实例化Service，先调用Service的attach，类似Activity的attach方法，给一些参数赋值，并且调用attachBaseContext(context)，给ContextWrapper的mBase赋值
    接着调用onCreate方法
    
    在Service.onCreate()结束后接着就是调用onStartCommand()
    不论是Service实例化之后还是再次调用startService，在AMS中，都是通过ActiveServices.sendServiceArgsLocked，触发调用onStartCommand()
    在sendServiceArgsLocked函数中，通过 r.app.thread.scheduleServiceArgs(r, slice) 转入ActivityThread执行后续操作
    ApplicationThread.scheduleServiceArgs(IBinder token, ParceledListSlice args)：
        List<ServiceStartArgs> list = args.getList();   //参数来自 ActiveServices.pendingStarts，若有多个挂起的start调用，就要多个参数，onStartCommand 也会调用多次
        for (int i = 0; i < list.size(); i++) {
            ServiceStartArgs ssa = list.get(i);
            ServiceArgsData s = new ServiceArgsData();
            s.token = token;
            s.taskRemoved = ssa.taskRemoved;
            s.startId = ssa.startId;
            s.flags = ssa.flags;
            s.args = ssa.args;

            sendMessage(H.SERVICE_ARGS, s);
        }
    ActivityThread.H.handleMessage：
        case SERVICE_ARGS:
            "serviceStart: " + String.valueOf(msg.obj)));
            handleServiceArgs((ServiceArgsData)msg.obj);
            break;
    ActivityThread.handleServiceArgs(ServiceArgsData data)：
        Service s = mServices.get(data.token);  //在实例化Service时，加入到这个集合中了
        if (s != null) {
           try {
               if (data.args != null) {
                   data.args.setExtrasClassLoader(s.getClassLoader());
                   data.args.prepareToEnterProcess();
               }
               int res;
               if (!data.taskRemoved) {
                   res = s.onStartCommand(data.args, data.flags, data.startId);     //Service的onStartCommand
               } else {
                   s.onTaskRemoved(data.args);  //Service的onTaskRemoved，指的是Intent被取消？
                   res = Service.START_TASK_REMOVED_COMPLETE;
               }
               QueuedWork.waitToFinish();
               try {
                   ActivityManager.getService().serviceDoneExecuting(data.token, SERVICE_DONE_EXECUTING_START, data.startId, res);
               } 
               ensureJitEnabled();      //启用Jit？
           } catch (Exception e) {
               if (!mInstrumentation.onException(s, e)) {
                   throw new RuntimeException("Unable to start service " + s + " with " + data.args + ": " + e.toString(), e);
               }
           }
        }
    调用到了Service的onStartCommand、onTaskRemoved
    到此，startService/startForegroundService的启动过程完毕
    
#### bindService

    ContextImpl.bindService(Intent service, ServiceConnection conn, int flags)：
        warnIfCallingFromSystemProcess();   ////如果系统进程直接调用诸如startService(Intent)之类的方法而不是startServiceAsUser(Intent,UserHandle)，则记录警告。“AsUser”变体使我们能够适当地执行用户的限制。
        return bindServiceCommon(service, conn, flags, mMainThread.getHandler(), getUser());    //mMainThread.getHandler() 返回的是 mH 这个Handler
   
    ContextImpl.bindServiceCommon(Intent service, ServiceConnection conn, int flags, Handler handler, UserHandle user)：
        /使其与DevicePolicyManager.bindDeviceAdminServiceAsUser保持同步
        IServiceConnection sd;
        if (conn == null)   throw new IllegalArgumentException("connection is null");
        if (mPackageInfo != null) {
            sd = mPackageInfo.getServiceDispatcher(conn, getOuterContext(), handler, flags);    //获取与 conn 绑定的IServiceConnection
        } else {
            throw new RuntimeException("Not supported in system context");
        }
        validateServiceIntent(service);     //隐式Intent的验证，5.0以上不允许使用
        IBinder token = getActivityToken(); //获取token，在构造ContextImpl时赋值的
        if (token == null && (flags&BIND_AUTO_CREATE) == 0 && mPackageInfo != null  //flags不是BIND_AUTO_CREATE
                && mPackageInfo.getApplicationInfo().targetSdkVersion < android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {  //4.0一下吸引
            flags |= BIND_WAIVE_PRIORITY;   //设置flags
        }
        service.prepareToLeaveProcess(this);    //是否要离开当前进程？
        int res = ActivityManager.getService().bindService( mMainThread.getApplicationThread(), getActivityToken(), service,
            service.resolveTypeIfNeeded(getContentResolver()), sd, flags, getOpPackageName(), user.getIdentifier());   //转入AMS（SystemServer进程）执行
        if (res < 0) {
            throw new SecurityException("Not allowed to bind to service " + service);
        }
        return res != 0;    //是否bind成功，返回0表示不成功，非0 成功
    检查传入的参数是否符合，如 是否是隐式Intent， conn不能为null等，然后转入AMS执行
    
    ActivityManagerService.bindService(IApplicationThread caller, IBinder token, Intent service, String resolvedType, IServiceConnection connection, int flags, String callingPackage, int userId)：
        enforceNotIsolatedCaller("bindService");    //验证是否是隔离进程在调用，不允许隔离进程调用，会抛异常
        //拒绝可能泄漏的文件描述符
        if (service != null && service.hasFileDescriptors() == true) {  //Intent不允许传fd
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        if (callingPackage == null) {   //bindService调用者的包名不能为空
            throw new IllegalArgumentException("callingPackage cannot be null");
        }
        synchronized(this) {
            return mServices.bindServiceLocked(caller, token, service, resolvedType, connection, flags, callingPackage, userId);
        }
    隔离进程中不能调用startService/bindService，Intent中不能带fd，最后转入ActiveServices
    
    ActiveServices.bindServiceLocked(IApplicationThread caller, IBinder token, Intent service, String resolvedType, final IServiceConnection connection, int flags, String callingPackage, final int userId)：
        "bindService: " + service + " type=" + resolvedType + " conn=" + connection.asBinder() + " flags=0x" + Integer.toHexString(flags));
        final ProcessRecord callerApp = mAm.getRecordForAppLocked(caller);  //在LRU中找出调用者进程信息
        if (callerApp == null) {
            throw new SecurityException("Unable to find app for caller " + caller + " (pid=" + Binder.getCallingPid() + ") when binding service " + service);
        }
        ActivityRecord activity = null;
        if (token != null) {
            activity = ActivityRecord.isInStackLocked(token);   //在ActivityStack中找出当前调用的ActivityRecord
            if (activity == null) {
                Slog.w(TAG, "Binding with unknown activity: " + token);     //绑定到未知的Activity？
                return 0;
            }
        }
        int clientLabel = 0;
        PendingIntent clientIntent = null;
        final boolean isCallerSystem = callerApp.info.uid == Process.SYSTEM_UID;    //是否是系统进程调用？
        if (isCallerSystem) {
            //令人毛骨悚然的事情-允许系统信息告诉我们它们是什么，因此我们可以在其他地方报告此信息，以供其他人了解为什么某些服务正在运行
            service.setDefusable(true);
            clientIntent = service.getParcelableExtra(Intent.EXTRA_CLIENT_INTENT);
            if (clientIntent != null) {
                clientLabel = service.getIntExtra(Intent.EXTRA_CLIENT_LABEL, 0);
                if (clientLabel != 0) {
                    //意图中没有有用的额外功能，请将其丢弃。用这些东西进行系统代码调用只需要知道这将会发生
                    service = service.cloneFilter();
                }
            }
        }
        if ((flags&Context.BIND_TREAT_LIKE_ACTIVITY) != 0) {
            mAm.enforceCallingPermission(android.Manifest.permission.MANAGE_ACTIVITY_STACKS, "BIND_TREAT_LIKE_ACTIVITY");    //检查权限
        }
        if ((flags & Context.BIND_ALLOW_WHITELIST_MANAGEMENT) != 0 && !isCallerSystem) {    //白名单管理？非系统调用者
            throw new SecurityException("Non-system caller " + caller + " (pid=" + Binder.getCallingPid() + ") set BIND_ALLOW_WHITELIST_MANAGEMENT when binding service " + service);
        }       //不允许非系统调用者设置白名单管理？
        if ((flags & Context.BIND_ALLOW_INSTANT) != 0 && !isCallerSystem) { //instant，非系统调用者
            throw new SecurityException("Non-system caller " + caller + " (pid=" + Binder.getCallingPid() + ") set BIND_ALLOW_INSTANT when binding service " + service);
        }       //非系统调用者不能使用instant run？
        final boolean callerFg = callerApp.setSchedGroup != ProcessList.SCHED_GROUP_BACKGROUND; //前台调用？
        final boolean isBindExternal = (flags & Context.BIND_EXTERNAL_SERVICE) != 0;    //绑定到调用者？允许应用程序提供属于使用服务的应用程序的服务，而不是提供服务的应用程序的服务
        final boolean allowInstant = (flags & Context.BIND_ALLOW_INSTANT) != 0;     //是否允许 instant run
        ServiceLookupResult res = retrieveServiceLocked(service, resolvedType, callingPackage, Binder.getCallingPid(),
                    Binder.getCallingUid(), userId, true, callerFg, isBindExternal, allowInstant);  //查找/解析Service信息？
        if (res == null) {
            return 0;       //绑定失败
        }
        if (res.record == null) {
            return -1;      //bind失败，不允许绑定？安全问题？未找到Service信息？
        }
        ServiceRecord s = res.record;       //service信息
        boolean permissionsReviewRequired = false;  //是否重新查看权限？
        //如果权限需要审查才能运行任何应用程序组件，我们会安排与服务的绑定但不启动其进程，然后我们启动一个审查activity，当该Activity完成时，将向该Activity传递回调以调用以启动绑定服务的进程。完成绑定
        if (mAm.mPermissionReviewRequired) {    //需要权限审查
            if (mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(s.packageName, s.userId)) { //当前包下的用户是否需要权限审查
                permissionsReviewRequired = true;   //要进行权限审查
                //显示仅用于从前台应用程序绑定的权限查看UI
                if (!callerFg) {
                    "u" + s.userId + " Binding to a service in package" + s.packageName + " requires a permissions review");
                    return 0;
                }
                final ServiceRecord serviceRecord = s;
                final Intent serviceIntent = service;   
                RemoteCallback callback = new RemoteCallback(new RemoteCallback.OnResultListener() {    //远程回调
                    @Override
                    public void onResult(Bundle result) {
                        synchronized(mAm) {
                            final long identity = Binder.clearCallingIdentity();
                            try {
                                if (!mPendingServices.contains(serviceRecord)) {    //挂起的Service中不包含当前Service，则返回
                                    return;
                                }
                                //如果仍然有待处理的记录，那么服务绑定请求仍然有效，因此将它们连接起来。只有在呼叫者清除了审核要求后，我们才会继续进行操作，否则我们将取消绑定，因为用户未批准
                                if (!mAm.getPackageManagerInternalLocked().isPermissionsReviewRequired(serviceRecord.packageName, serviceRecord.userId)) {    //当前包下的用户没有权限审查
                                    try {
                                        bringUpServiceLocked(serviceRecord, serviceIntent.getFlags(), callerFg, false, false);  //启动Service，同startService中的流程
                                    } catch (RemoteException e) {
                                        /* ignore - local call */
                                    }
                                } else {    //在审查权限的Activity中未清理审核要求，则表示启动失败
                                    unbindServiceLocked(connection);    //断开连接，移除相关缓存数据
                                }
                            } finally {
                                Binder.restoreCallingIdentity(identity);
                            }
                        }
                    }
                });
                final Intent intent = new Intent(Intent.ACTION_REVIEW_PERMISSIONS);     //隐式启动审查权限Activity
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                intent.putExtra(Intent.EXTRA_PACKAGE_NAME, s.packageName);
                intent.putExtra(Intent.EXTRA_REMOTE_CALLBACK, callback);    //远程回调
                "u" + s.userId + " Launching permission review for package " + s.packageName);
                mAm.mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mAm.mContext.startActivityAsUser(intent, new UserHandle(userId));   //启动Activity
                    }
                });
            }
        }
        final long origId = Binder.clearCallingIdentity();
        try {
            if (unscheduleServiceRestartLocked(s, callerApp.info.uid, false)) {     //从重启列表中移除当前Service（若有，从其他App中启动）
                "BIND SERVICE WHILE RESTART PENDING: " + s);    //重新启动挂起时的绑定服务：
            }
            if ((flags&Context.BIND_AUTO_CREATE) != 0) {    //flag--BIND_AUTO_CREATE
                s.lastActivity = SystemClock.uptimeMillis();
                if (!s.hasAutoCreateConnections()) {    //在已创建的连接中没有BIND_AUTO_CREATE flag，表示当前是第一个连接
                    //这是第一个绑定，请跟踪器知道
                    ServiceState stracker = s.getTracker();
                    if (stracker != null) {
                        stracker.setBound(true, mAm.mProcessStats.getMemFactorLocked(), s.lastActivity);
                    }
                }
            }
            mAm.startAssociationLocked(callerApp.uid, callerApp.processName, callerApp.curProcState, s.appInfo.uid, s.name, s.processName);  //将这些参数加入到AMS中的mAssociations 列表中
            //一旦将这些应用程序关联起来，如果其中一个呼叫者是短暂的，则目标应用程序现在应该能够看到正在调用的应用程序
            mAm.grantEphemeralAccessLocked(callerApp.userId, service, s.appInfo.uid, UserHandle.getAppId(callerApp.uid));
            AppBindRecord b = s.retrieveAppBindingLocked(service, callerApp);   //构造保存返回AppBindRecord
            ConnectionRecord c = new ConnectionRecord(b, activity, connection, flags, clientLabel, clientIntent);
            IBinder binder = connection.asBinder();     //binder
            ArrayList<ConnectionRecord> clist = s.connections.get(binder);
            if (clist == null) {
                clist = new ArrayList<ConnectionRecord>();
                s.connections.put(binder, clist);   //Service信息内部的列表
            }
            clist.add(c);       //存储连接 ConnectionRecord
            b.connections.add(c);
            if (activity != null) {
                if (activity.connections == null) {
                    activity.connections = new HashSet<ConnectionRecord>();
                }
                activity.connections.add(c);        //向ActivityRecord中记录连接 ConnectionRecord
            }
            b.client.connections.add(c);
            if ((c.flags&Context.BIND_ABOVE_CLIENT) != 0) {  //bindService的flag--BIND_ABOVE_CLIENT，表示绑定到此服务的客户端应用程序认为该服务比应用程序本身更重要。 设置后，平台将尝试让内存不足杀手杀死应用程序，然后再杀死它所绑定的服务，尽管不能保证确实如此
                b.client.hasAboveClient = true;
            }
            if ((c.flags&Context.BIND_ALLOW_WHITELIST_MANAGEMENT) != 0) {   //白名单管理？
                s.whitelistManager = true;
            }
            if (s.app != null) {
                updateServiceClientActivitiesLocked(s.app, c, true);
            }
            clist = mServiceConnections.get(binder);
            if (clist == null) {
                clist = new ArrayList<ConnectionRecord>();
                mServiceConnections.put(binder, clist);
            }
            clist.add(c);   //在ActiveServices中的mServiceConnections中保存所有的连接信息
            if ((flags&Context.BIND_AUTO_CREATE) != 0) {    //flag，自动创建？
                s.lastActivity = SystemClock.uptimeMillis();
                if (bringUpServiceLocked(s, service.getFlags(), callerFg, false, permissionsReviewRequired) != null) {   //启动Service，同startService
                    return 0;   //没有启动成功
                }
            }
            if (s.app != null) {
                if ((flags&Context.BIND_TREAT_LIKE_ACTIVITY) != 0) {
                    s.app.treatLikeActivity = true;
                }
                if (s.whitelistManager) {
                    s.app.whitelistManager = true;
                }
                //这本可以使服务更重要
                mAm.updateLruProcessLocked(s.app, s.app.hasClientActivities || s.app.treatLikeActivity, b.client);
                mAm.updateOomAdjLocked(s.app, true);    //更新缓存？
            }
            "Bind " + s + " with " + b + ": received=" + b.intent.received + " apps=" + b.intent.apps.size() + " doRebind=" + b.intent.doRebind);
            if (s.app != null && b.intent.received) {   //Service信息中已经存在进程信息，则表示当前Service已经启动了，此时可直接建立bind连接或调用onStartCommond？
                //服务已经在运行，因此我们可以立即发布连接
                try {
                    c.conn.connected(s.name, b.intent.binder, false);   //会调用ServiceConnection的onServiceConnected？
                } catch (Exception e) {
                    "Failure sending service " + s.shortName + " to connection " + c.conn.asBinder() + " (in " + c.binding.client.processName + ")", e);
                }
                //如果这是第一个连接到此绑定的应用程序，并且以前要求该服务告知何时重新绑定，则请这样做
                if (b.intent.apps.size() == 1 && b.intent.doRebind) {   //rebind
                    requestServiceBindingLocked(s, b.intent, callerFg, true);   //内部会调用到ActivityThread中的scheduleBindService，
                }
            } else if (!b.intent.requested) {
                requestServiceBindingLocked(s, b.intent, callerFg, false);  //调用到ActivityThread中的scheduleBindService，并设置requested为true
            }
            getServiceMapLocked(s.userId).ensureNotStartingBackgroundLocked(s);     //从后台启动列表中移除Service，并立即启动它，确保后台启动列表中无数据？
        } finally {
            Binder.restoreCallingIdentity(origId);
        }
        return 1;
    先找到或解析出当前Service信息，然后看是否要权限审查（permissionsReviewRequired），若需要，则启动一个审查Activity，在审查的结果中看是否要启动Service。
    接着从重启列表中移除当前Service（若有），看当前是否是第一个绑定的连接，保存bind、connect、binder等信息，以及bindService的一些flag的处理等
    若是BIND_AUTO_CREATE，则调用bringUpServiceLocked启动Service，
    若启动成功，服务已经在运行，则调用连接的回调（onServiceConnected），是第一个绑定且需要通知rebind，会调用到ActivityThread中的scheduleBindService
    若服务没有在运行，且requested为false，则通过requestServiceBindingLocked调用到ActivityThread中的scheduleBindService，启动Service？
    
    启动Service都是调用bringUpServiceLocked，同startService中的流程
    启动成功后，都会通过requestServiceBindingLocked这个函数，走bindService的流程
    ActiveServices.requestServiceBindingLocked(ServiceRecord r, IntentBindRecord i, boolean execInFg, boolean rebind)：
        if (r.app == null || r.app.thread == null) {
            //如果服务当前未运行，则无法绑定
            return false;
        }
        "requestBind " + i + ": requested=" + i.requested + " rebind=" + rebind);
        if ((!i.requested || rebind) && i.apps.size() > 0) {    
            try {
                bumpServiceExecutingLocked(r, execInFg, "bind");    //延迟启动的处理？和系统未boot完成有关？
                r.app.forceProcessStateUpTo(ActivityManager.PROCESS_STATE_SERVICE);     //强制更新进程状态
                r.app.thread.scheduleBindService(r, i.intent.getIntent(), rebind, r.app.repProcState);  //转入ActivityThread执行
                if (!rebind) {  //false
                    i.requested = true;
                }
                i.hasBound = true;
                i.doRebind = false;
            } catch (TransactionTooLargeException e) {
                //保持executeNesting计数准确
                "Crashed while binding " + r, e);
                final boolean inDestroying = mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);      //结束Service？
                throw e;
            } catch (RemoteException e) {
                "Crashed while binding " + r);
                //保持executeNesting计数准确
                final boolean inDestroying = mDestroyingServices.contains(r);
                serviceDoneExecutingLocked(r, inDestroying, inDestroying);      //结束Service？
                return false;
            }
        }
        return true;
    scheduleBindService是在ActivityThread中的处理，会调用到Service的onBind、onRebind
    
    ApplicationThread.scheduleBindService(IBinder token, Intent intent, boolean rebind, int processState)：
        updateProcessState(processState, false);        //更新进程状态，VM Runtime状态
        BindServiceData s = new BindServiceData();      //bind数据
        s.token = token;
        s.intent = intent;
        s.rebind = rebind;      //这个值是由Service的onUnbind方法的返回值决定的
        sendMessage(H.BIND_SERVICE, s);     //由H 这个handler处理
    
    ActivityThread.H.handleMessage：
        case BIND_SERVICE:
            Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "serviceBind");
            handleBindService((BindServiceData)msg.obj);
            Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);        
            
    ActivityThread.handleBindService(BindServiceData data)：
        Service s = mServices.get(data.token);      //获取Service，是在handleCreateService中创建的
        if (s != null) {
            try {
                data.intent.setExtrasClassLoader(s.getClassLoader());
                data.intent.prepareToEnterProcess();
                try {
                    if (!data.rebind) {     //rebind为false，Service第一次绑定时rebind为false，而之后 onUnbind返回true，则再次绑定时 rebind为true
                        IBinder binder = s.onBind(data.intent);     //Service的onBind
                        ActivityManager.getService().publishService(data.token, data.intent, binder);   //最终会调用到c.conn.connected(r.name, service, false)，可能会调用ServiceConnection的onServiceConnected？
                   } else {
                       s.onRebind(data.intent);     //Service的onRebind，onRebind不会触发ServiceConnection的onServiceConnected？
                       ActivityManager.getService().serviceDoneExecuting(data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);    //执行完毕，取消延迟等？
                   }
                   ensureJitEnabled();  //启用JIT？
                } catch (RemoteException ex) {
                   throw ex.rethrowFromSystemServer();
                }
            } catch (Exception e) {
                if (!mInstrumentation.onException(s, e)) {  //异常处理
                   throw new RuntimeException("Unable to bind to service " + s + " with " + data.intent + ": " + e.toString(), e);
                }
            }
        }
    rebind是由Service的onUnbind方法的返回值决定的，Service第一次绑定时rebind为false，而之后 onUnbind返回true，则再次绑定时 rebind为true
    Service第一次绑定时rebind为false，而之后 onUnbind返回true，则再次绑定时 rebind为true
    Service的onBind后会调用到c.conn.connected(r.name, service, false)，最终可能会调用ServiceConnection的onServiceConnected？
    Service的onRebind没有调用publishService，所以不会触发ServiceConnection的onServiceConnected？
    
    unbindService的调用
    ActivityThread.handleUnbindService(BindServiceData data)：
       Service s = mServices.get(data.token);    //获取Service，是在handleCreateService中创建的
       if (s != null) {
           try {
               data.intent.setExtrasClassLoader(s.getClassLoader());
               data.intent.prepareToEnterProcess();     //进入进程？
               boolean doRebind = s.onUnbind(data.intent);      //Service的onUnbind
               try {
                   if (doRebind) {  //onUnbind返回true
                       ActivityManager.getService().unbindFinished(data.token, data.intent, doRebind);  //如果当前Service是在mDestroyingServices中，或者仅存在当前这一个绑定，则会设置AppBindRecord的doRebind为true，与当前doRebind无关
                   } else {
                       ActivityManager.getService().serviceDoneExecuting(data.token, SERVICE_DONE_EXECUTING_ANON, 0, 0);
                   }
               } catch (RemoteException ex) {
                   throw ex.rethrowFromSystemServer();
               }
           } catch (Exception e) {
               if (!mInstrumentation.onException(s, e)) {
                   throw new RuntimeException("Unable to unbind to service " + s + " with " + data.intent + ": " + e.toString(), e);
               }
           }
       }
    Service的onUnbind返回true，会调用AMS的unbindFinished，其中如果当前Service是在mDestroyingServices列表中，
    或者仅存在当前这一个绑定，则会设置AppBindRecord的doRebind为true，而与当前onUnbind返回值doRebind无关
    unbindFinished中，若Service不在mDestroyingServices中，且有多个绑定，则会调用到requestServiceBindingLocked，
    从而触发scheduleBindService和handleBindService，rebind为true，最终会走Service的onRebind？
    
    至此，bindService流程分析完毕
    
