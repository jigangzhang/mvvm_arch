
## Application启动流程

#### 概述
    
    自home页面点击App图标起，经过一系列的跳转，成功fork了一个App进程，最后，调用了ActivityThread的main函数。
    ActivityThread并非是一个Thread对象，其代表的是自fork成功后程序一直在其中运行的那个线程。
    本节将从ActivityThread的main函数出发，直到第一个Activity成功启动为止。
    
#### 步骤

    ActivityThread.main：
        //CloseGuard默认为true，并且可能很垃圾。我们在这里禁用它，但稍后在调试版本中有选择地启用它（通过StrictMode），但使用DropBox而不是日志
        CloseGuard.setEnabled(false);
        Environment.initForCurrentUser();   //new了一个UserEnvironment，并获取了userId
        //在libcore中设置事件日志记录器
        EventLogger.setReporter(new EventLoggingReporter());
        //确保TrustedCertificateStore查找CA证书的位置正确
        final File configDir = Environment.getUserConfigDirectory(UserHandle.myUserId());   //返回用户的配置目录。系统服务使用它来存储与用户相关的文件，任何以该用户身份运行的应用程序都应该能够读取这些文件
        TrustedCertificateStore.setDefaultUserDirectory(configDir);     //给CA证书认证设置默认路径
        Process.setArgV0("<pre-initialized>");      //设置Process的argv[0]参数，调用native方法
        Looper.prepareMainLooper();     //初始化mainLooper，Handler的标准使用方式
        //如果在命令行中提供，请找到{@link #PROC_START_SEQ_IDENT}的值。 格式为“ seq = 114”
        long startSeq = 0;
        if (args != null) {     //main方法的参数可能是null
            for (int i = args.length - 1; i >= 0; --i) {
                if (args[i] != null && args[i].startsWith(PROC_START_SEQ_IDENT)) {
                    startSeq = Long.parseLong(
                            args[i].substring(PROC_START_SEQ_IDENT.length()));
                }
            }
        }
        ActivityThread thread = new ActivityThread();   //构造函数内new了一个ResourcesManager
        thread.attach(false, startSeq);
        if (sMainThreadHandler == null) {
            sMainThreadHandler = thread.getHandler();   //返回的是 mH这个最重要的Handler
        }
        if (false) {
            Looper.myLooper().setMessageLogging(new LogPrinter(Log.DEBUG, "ActivityThread"));
        }
        Looper.loop();      //开启一个死循环，从MessageQueue中读取消息
        throw new RuntimeException("Main thread loop unexpectedly exited");     //异常退出
    
    Thread.attach：
        sCurrentActivityThread = this;
        mSystemThread = system;     //system为false
        if (!system) {
            ViewRootImpl.addFirstDrawHandler(new Runnable() {   //在第一次draw时执行
                @Override
                public void run() {
                    ensureJitEnabled();     //启用JIT？
                }
            });
            android.ddm.DdmHandleAppName.setAppName("<pre-initialized>", UserHandle.myUserId());    //设置应用程序名称。 当我们被命名时调用，可以在DDMS连接之前或之后调用。 对于后者，我们需要发送一个APNM消息
            RuntimeInit.setApplicationObject(mAppThread.asBinder());    //设置标识此应用程序/进程的对象，以报告VM错误
            final IActivityManager mgr = ActivityManager.getService();  //拿到AMS的本地代理
            try {
                mgr.attachApplication(mAppThread, startSeq);    //重要步骤，远程调用，会在SystemServer进程中执行
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
            // 注意是否接近堆极限
            BinderInternal.addGcWatcher(new Runnable() {    //用于调试Binder？在finalize方法中调用，是在内存回收时触发？
                @Override public void run() {
                    if (!mSomeActivitiesChanged) {
                        return;
                    }
                    Runtime runtime = Runtime.getRuntime();
                    long dalvikMax = runtime.maxMemory();   //VM的最大内存
                    long dalvikUsed = runtime.totalMemory() - runtime.freeMemory();     //VM已使用的内存
                    if (dalvikUsed > ((3*dalvikMax)/4)) {
                        if (DEBUG_MEMORY_TRIM) Slog.d(TAG, "Dalvik max=" + (dalvikMax/1024) + " total=" + (runtime.totalMemory()/1024) + " used=" + (dalvikUsed/1024));
                        mSomeActivitiesChanged = false;
                        try {
                            mgr.releaseSomeActivities(mAppThread);  //远程调用，释放内存？
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                    }
                }
            });
        } else {
            //不要在这里设置Application对象-如果系统崩溃，我们将无法显示警报，我们只是想died
            android.ddm.DdmHandleAppName.setAppName("system_process", UserHandle.myUserId());
            try {
                mInstrumentation = new Instrumentation();
                mInstrumentation.basicInit(this);       //设置ActivityThread
                ContextImpl context = ContextImpl.createAppContext(this, getSystemContext().mPackageInfo);
                mInitialApplication = context.mPackageInfo.makeApplication(true, null);     //构造Application对象，调用其attach方法
                mInitialApplication.onCreate();     //调用Application onCreate方法
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate Application():" + e.toString(), e);
            }
        }
        DropBox.setReporter(new DropBoxReporter());     //添加dropbox日志到libcore
        ViewRootImpl.ConfigChangedCallback configChangedCallback = (Configuration globalConfig) -> {    //用于通知全局配置更改的回调
            synchronized (mResourcesManager) {
                //我们需要立即将此更改应用于资源，因为返回后，视图层次结构将被告知
                if (mResourcesManager.applyConfigurationToResourcesLocked(globalConfig, null /* compat */)) {
                    updateLocaleListFromAppContext(mInitialApplication.getApplicationContext(), mResourcesManager.getConfiguration().getLocales());
                    //这实际上改变了资源！告诉大家
                    if (mPendingConfiguration == null || mPendingConfiguration.isOtherSeqNewer(globalConfig)) {
                        mPendingConfiguration = globalConfig;
                        sendMessage(H.CONFIGURATION_CHANGED, globalConfig);     //将全局配置发送改变的信息发送到mH
                    }
                }
            }
        };
        ViewRootImpl.addConfigCallback(configChangedCallback);      //系统配置更改的回调，在performConfigurationChange中被调用，是在performTraversals中（页面初始化）、配置改变时被调用
    
    接着看ActivityManagerService.attachApplication：（此处是通过远程调用在SystemServer中执行的）
        synchronized (this) {
            int callingPid = Binder.getCallingPid();        //返回当前正在处理事务的进程id？
            final int callingUid = Binder.getCallingUid();  //返回当前正在处理事务的进程uid？
            final long origId = Binder.clearCallingIdentity();  //重置当前线程上传入的IPC标识？
            attachApplicationLocked(thread, callingPid, callingUid, startSeq);
            Binder.restoreCallingIdentity(origId);      //将当前线程上传入的IPC标识恢复到clearCallingIdentity之前的值
        }
    ActivityManagerService.attachApplicationLocked：
        //查找附着的应用程序记录...如果我们在多个进程中运行，则通过pid查找；如果我们使用匿名线程模拟进程，则仅提取下一个应用程序记录
        ProcessRecord app;
        long startTime = SystemClock.uptimeMillis();
        //mPidsSelfLocked，保存所有进程，当前运行的所有进程都是由pid组织的。键是运行应用程序的pid。注意:此对象受其自身锁的保护，而不是全局的活动管理器锁!
        if (pid != MY_PID && pid >= 0) {
            synchronized (mPidsSelfLocked) {
                app = mPidsSelfLocked.get(pid);
            }
        } else {
            app = null;
        }
        //在我们有机会更新内部状态之前，可能进程已经调用过了attachApplication
        if (app == null && startSeq > 0) {      //在APP进程启动过程startProcessLocked中，已经将ProcessRecord添加到mPendingStarts中了
            final ProcessRecord pending = mPendingStarts.get(startSeq);     //mPendingStarts，包含用于启动挂起进程的ProcessRecord对象，handleProcessStartedLocked主要是将ProcessRecord对象放到mPidsSelfLocked中？
            if (pending != null && pending.startUid == callingUid && handleProcessStartedLocked(pending, pid, pending.usingWrapper, startSeq, true)) {
                app = pending;
            }
        }
        if (app == null) {
            //（没有挂起的进程）No pending application record for pid " + pid + " (IApplicationThread " + thread + "); dropping process"
            if (pid > 0 && pid != MY_PID) {
                killProcessQuiet(pid);      //TODO: killProcessGroup(app.info.uid, pid);    通过发送系统信号杀掉进程
            } else {
                thread.scheduleExit();      //通过Looper().quit()结束主线程，即结束进程
            }
            return false;
        }
        //如果此应用程序记录仍附加到先前的进程中，请立即清理它
        if (app.thread != null) {
            handleAppDiedLocked(app, true, true);   //用于从AMS中删除已存在的进程主要方法，结果就是该进程消失。清除与进程的所有连接。
        }
        //告诉进程有关自身的一切
        if (DEBUG_ALL) Slog.v(TAG, "Binding process pid " + pid + " to record " + app);
        final String processName = app.processName;
        try {
            AppDeathRecipient adr = new AppDeathRecipient(app, pid, thread);    //用于Binder远程连接，在托管IBinder的进程断开连接时接收回调，回调中杀掉进程？
            thread.asBinder().linkToDeath(adr, 0);      //注册死亡通知，远端使用
            app.deathRecipient = adr;
        } catch (RemoteException e) {
            app.resetPackageList(mProcessStats);        //从列表中删除除info中指定的包以外的所有包
            startProcessLocked(app, "link fail", processName);  //重启进程？
            return false;
        }
        app.makeActive(thread, mProcessStats);      //主要用于给app.thread赋值
        app.curAdj = app.setAdj = app.verifiedAdj = ProcessList.INVALID_ADJ;
        app.curSchedGroup = app.setSchedGroup = ProcessList.SCHED_GROUP_DEFAULT;
        app.forcingToImportant = null;
        updateProcessForegroundLocked(app, false, false);       //更新前台服务？和Battery Service有关？
        app.hasShownUi = false;
        app.debugging = false;
        app.cached = false;
        app.killedByAm = false;
        app.killed = false;
        //我们谨慎地使用PackageManager用于过滤的相同状态，因为我们使用此标志来决定在以后用户解锁时是否需要安装提供程序
        app.unlocked = StorageManager.isUserKeyUnlocked(app.userId);
        mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);       //移除延迟启动进程的消息？
        boolean normalMode = mProcessesReady || isAllowedWhileBooting(app.info);
        List<ProviderInfo> providers = normalMode ? generateApplicationProvidersLocked(app) : null;
        if (providers != null && checkAppInLaunchingProvidersLocked(app)) {
            Message msg = mHandler.obtainMessage(CONTENT_PROVIDER_PUBLISH_TIMEOUT_MSG);
            msg.obj = app;
            mHandler.sendMessageDelayed(msg, CONTENT_PROVIDER_PUBLISH_TIMEOUT);
        }
        //attachApplicationLocked: before bindApplication
        //New app record " + app + " thread=" + thread.asBinder() + " pid=" + pid
        try {
            int testMode = ApplicationThreadConstants.DEBUG_OFF;
            if (mDebugApp != null && mDebugApp.equals(processName)) {
                testMode = mWaitForDebugger ? ApplicationThreadConstants.DEBUG_WAIT : ApplicationThreadConstants.DEBUG_ON;
                app.debugging = true;
                if (mDebugTransient) {
                    mDebugApp = mOrigDebugApp;
                    mWaitForDebugger = mOrigWaitForDebugger;
                }
            }       //和debug有关
            boolean enableTrackAllocation = false;
            if (mTrackAllocationApp != null && mTrackAllocationApp.equals(processName)) {
                enableTrackAllocation = true;
                mTrackAllocationApp = null;
            }       //和track allocation有关
            //如果正在启动该应用以进行还原或完全备份，请进行特殊设置
            boolean isRestrictedBackupMode = false;
            if (mBackupTarget != null && mBackupAppName.equals(processName)) {
                isRestrictedBackupMode = mBackupTarget.appInfo.uid >= FIRST_APPLICATION_UID && ((mBackupTarget.backupMode == BackupRecord.RESTORE)
                                || (mBackupTarget.backupMode == BackupRecord.RESTORE_FULL) || (mBackupTarget.backupMode == BackupRecord.BACKUP_FULL));
            }
            if (app.instr != null) {
                notifyPackageUse(app.instr.mClass.getPackageName(), PackageManager.NOTIFY_PACKAGE_USE_INSTRUMENTATION);
            }
            //Binding proc " + processName + " with config " + getGlobalConfiguration()
            ApplicationInfo appInfo = app.instr != null ? app.instr.mTargetInfo : app.info;
            app.compat = compatibilityInfoForPackageLocked(appInfo);
            ProfilerInfo profilerInfo = null;
            String preBindAgent = null;
            if (mProfileApp != null && mProfileApp.equals(processName)) {
                mProfileProc = app;
                if (mProfilerInfo != null) {
                    //如果已给出文件或应在绑定时加载代理，则向应用发送探查器信息对象
                    boolean needsInfo = mProfilerInfo.profileFile != null || mProfilerInfo.attachAgentDuringBind;
                    profilerInfo = needsInfo ? new ProfilerInfo(mProfilerInfo) : null;
                    if (mProfilerInfo.agent != null) {
                        preBindAgent = mProfilerInfo.agent;
                    }
                }
            } else if (app.instr != null && app.instr.mProfileFile != null) {
                profilerInfo = new ProfilerInfo(app.instr.mProfileFile, null, 0, false, false, null, false);
            }
            if (mAppAgentMap != null && mAppAgentMap.containsKey(processName)) {
                //我们需要在此处进行可调试的检查。 有关为什么将检查推迟到此处的信息，请参见setAgentApp
                if ((app.info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                    String agent = mAppAgentMap.get(processName);
                    //不要覆盖已请求的代理
                    if (profilerInfo == null) {
                        profilerInfo = new ProfilerInfo(null, null, 0, false, false, mAppAgentMap.get(processName), true);
                    } else if (profilerInfo.agent == null) {
                        profilerInfo = profilerInfo.setAgent(mAppAgentMap.get(processName), true);
                    }
                }
            }
            if (profilerInfo != null && profilerInfo.profileFd != null) {
                profilerInfo.profileFd = profilerInfo.profileFd.dup();
                if (TextUtils.equals(mProfileApp, processName) && mProfilerInfo != null) {
                    clearProfilerLocked();      //清除ProfilerInfo信息？
                }
            }
            //我们已弃用Build.SERIAL，面向v2安全沙箱的应用程序和面向高于O MR1的API的应用程序均无法访问它。由于对串行的访问现在已获得许可，因此我们降低了该值
            final String buildSerial = (appInfo.targetSandboxVersion < 2 && appInfo.targetSdkVersion < Build.VERSION_CODES.P) ? sTheRealBuildSerial : Build.UNKNOWN;
            //检查这是否是次要进程，应该将其合并到某些当前活动的instrumentation中。（请注意，在执行以上所有分析工作之后，我们将执行此操作，因为分析当前仅可在主要的instrumentation进程中进行）
            if (mActiveInstrumentation.size() > 0 && app.instr == null) {
                for (int i = mActiveInstrumentation.size() - 1; i >= 0 && app.instr == null; i--) {
                    ActiveInstrumentation aInstr = mActiveInstrumentation.get(i);
                    if (!aInstr.mFinished && aInstr.mTargetInfo.uid == app.uid) {
                        if (aInstr.mTargetProcesses.length == 0) {
                            //这是通配符模式，应包括为目标Instrumentation启动的每个进程
                            if (aInstr.mTargetInfo.packageName.equals(app.info.packageName)) {
                                app.instr = aInstr;
                                aInstr.mRunningProcesses.add(app);
                            }
                        } else {
                            for (String proc : aInstr.mTargetProcesses) {
                                if (proc.equals(app.processName)) {
                                    app.instr = aInstr;                     //给app.instr赋值
                                    aInstr.mRunningProcesses.add(app);      //将当前进程的ApplicationRecord信息添加到mRunningProcesses中
                                    break;
                                }
                            }
                        }
                    }
                }
            }        
            //如果要求我们在启动时附加代理，请在绑定应用程序代码之前立即进行
            if (preBindAgent != null) {
                thread.attachAgent(preBindAgent);
            }
            //确定应用程序是否需要在自动填充兼容模式下运行。
            boolean isAutofillCompatEnabled = false;
            if (UserHandle.getAppId(app.info.uid) >= Process.FIRST_APPLICATION_UID) {
                final AutofillManagerInternal afm = LocalServices.getService(
                        AutofillManagerInternal.class);
                if (afm != null) {
                    isAutofillCompatEnabled = afm.isCompatibilityModeRequested(
                            app.info.packageName, app.info.versionCode, app.userId);
                }
            }
            //attachApplicationLocked: immediately before bindApplication
            mStackSupervisor.getActivityMetricsLogger().notifyBindApplication(app);
            if (app.isolatedEntryPoint != null) {
                //这是一个隔离进程，应该只调用一个入口点（EntryPoint，执行其main方法），而不是绑定Application
                thread.runIsolatedEntryPoint(app.isolatedEntryPoint, app.isolatedEntryPointArgs);
            } else if (app.instr != null) {
                thread.bindApplication(processName, appInfo, providers, app.instr.mClass,
                        profilerInfo, app.instr.mArguments, app.instr.mWatcher, app.instr.mUiAutomationConnection, testMode,
                        mBinderTransactionTrackingEnabled, enableTrackAllocation, isRestrictedBackupMode || !normalMode, app.persistent,
                        new Configuration(getGlobalConfiguration()), app.compat, getCommonServicesLocked(app.isolated),
                        mCoreSettingsObserver.getCoreSettingsLocked(), buildSerial, isAutofillCompatEnabled);
            } else {
                thread.bindApplication(processName, appInfo, providers, null, profilerInfo, null, null, null, testMode,
                        mBinderTransactionTrackingEnabled, enableTrackAllocation, isRestrictedBackupMode || !normalMode, app.persistent,
                        new Configuration(getGlobalConfiguration()), app.compat, getCommonServicesLocked(app.isolated),
                        mCoreSettingsObserver.getCoreSettingsLocked(), buildSerial, isAutofillCompatEnabled);
            }       //上面两项都是远程调用ApplicationThread的bindApplication方法，这也是一个跨进程调用，在App进程中执行
            if (profilerInfo != null) {
                profilerInfo.closeFd();
                profilerInfo = null;
            }
            //attachApplicationLocked: immediately after bindApplication
            updateLruProcessLocked(app, false, null);
            checkTime(startTime, "attachApplicationLocked: after updateLruProcessLocked");
            app.lastRequestedGc = app.lastLowMemory = SystemClock.uptimeMillis();
        } catch (Exception e) {
            //todo: Yikes! 待办事项：Yi！我们应该做什么？目前，我们将尝试启动另一个进程，但这很容易使我们陷入无限重启进程的循环中...
            Slog.wtf(TAG, "Exception thrown during bind of " + app, e);
            app.resetPackageList(mProcessStats);
            app.unlinkDeathRecipient();
            startProcessLocked(app, "bind fail", processName);
            return false;
        }
        //从启动应用程序列表中删除该记录
        mPersistentStartingProcesses.remove(app);
        if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG_PROCESSES, "Attach application locked removing on hold: " + app);
        mProcessesOnHold.remove(app);
        boolean badApp = false;
        boolean didSomething = false;
        //查看栈顶的Activity是否正在等待在此进程中运行...
        if (normalMode) {
            try {
                if (mStackSupervisor.attachApplicationLocked(app)) {    //启动Activity
                    didSomething = true;
                }
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown launching activities in " + app, e);
                badApp = true;
            }
        }
        //查找应该在此进程中运行的任何服务...
        if (!badApp) {
            try {
                didSomething |= mServices.attachApplicationLocked(app, processName);    //启动Service
                checkTime(startTime, "attachApplicationLocked: after mServices.attachApplicationLocked");
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown starting services in " + app, e);
                badApp = true;
            }
        }
        //检查下一个广播接收器是否在此进程中...
        if (!badApp && isPendingBroadcastProcessLocked(pid)) {
            try {
                didSomething |= sendPendingBroadcastsLocked(app);   //启动广播？
                checkTime(startTime, "attachApplicationLocked: after sendPendingBroadcastsLocked");
            } catch (Exception e) {
                //如果该应用在尝试启动接收器时失败，则我们将其声明为“bad”
                Slog.wtf(TAG, "Exception thrown dispatching broadcasts in " + app, e);
                badApp = true;
            }
        }
        // 检查下一个备份代理是否在此进程中...
        if (!badApp && mBackupTarget != null && mBackupTarget.app == app) {
            //New app is backup target, launching agent for " + app
            notifyPackageUse(mBackupTarget.appInfo.packageName, PackageManager.NOTIFY_PACKAGE_USE_BACKUP);
            try {
                thread.scheduleCreateBackupAgent(mBackupTarget.appInfo,
                        compatibilityInfoForPackageLocked(mBackupTarget.appInfo), mBackupTarget.backupMode);
            } catch (Exception e) {
                Slog.wtf(TAG, "Exception thrown creating backup agent in " + app, e);
                badApp = true;
            }
        }
        if (badApp) {
            app.kill("error during init", true);
            handleAppDiedLocked(app, false, true);
            return false;
        }
        if (!didSomething) {
            updateOomAdjLocked();
            checkTime(startTime, "attachApplicationLocked: after updateOomAdjLocked");
        }
        return true;
    attachApplicationLocked，传入其中的thread为IApplicationThread，这是一个AIDL形式的接口，其在ActivityThread中的实现类为ApplicationThread，是服务端的实现类，在AMS中拿到其代理进行远程调用
    在这里通过远程调用，将bindApplication方法执行切换到App进程中
    bindApplication中通过Handler将BIND_APPLICATION消息发送到H这个Handler中，然后在handleBindApplication中进行处理，在其中初始化Application并执行其onCreate方法等
    Application启动之后，接着依次启动Activity、Service、BroadCast等
    
#### 初始化Application
    
    ApplicationThread.bindApplication：
        if (services != null) {
            //在ServiceManager中设置服务缓存
            ServiceManager.initServiceCache(services);  //仅在首次由活动管理器启动并绑定进程时才调用此方法。此时该进程中只有一个线程，因此不会进行锁定
        }
        setCoreSettings(coreSettings);  //应用系统设置，通过Handler执行handleSetCoreSettings，最终是将设置应用于所有Activity，会重新启动所有的Activity
        AppBindData data = new AppBindData();   //静态内部类，一些App信息
        data.processName = processName;
        data.appInfo = appInfo;
        data.providers = providers;
        data.instrumentationName = instrumentationName;
        data.instrumentationArgs = instrumentationArgs;
        data.instrumentationWatcher = instrumentationWatcher;
        data.instrumentationUiAutomationConnection = instrumentationUiConnection;
        data.debugMode = debugMode;
        data.enableBinderTracking = enableBinderTracking;
        data.trackAllocation = trackAllocation;
        data.restrictedBackupMode = isRestrictedBackupMode;
        data.persistent = persistent;
        data.config = config;
        data.compatInfo = compatInfo;
        data.initProfilerInfo = profilerInfo;
        data.buildSerial = buildSerial;
        data.autofillCompatibilityEnabled = autofillCompatibilityEnabled;
        sendMessage(H.BIND_APPLICATION, data);      //将这些信息发送到Handler(mH)中进行处理
    H.handleMessage：
        ...
        case BIND_APPLICATION:
            AppBindData data = (AppBindData)msg.obj;
            handleBindApplication(data);
            break;
        ...
    ActivityThread.handleBindApplication：
        //将UI线程注册为运行时的敏感线程
        VMRuntime.registerSensitiveThread();
        if (data.trackAllocation) {     //追踪配置？
            DdmVmInternal.enableRecentAllocations(true);    //与DDMS有关？
        }
        //请注意此进程是何时开始的
        Process.setStartTimes(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis());
        mBoundApplication = data;
        mConfiguration = new Configuration(data.config);
        mCompatConfiguration = new Configuration(data.config);
        mProfiler = new Profiler();
        String agent = null;
        if (data.initProfilerInfo != null) {
            mProfiler.profileFile = data.initProfilerInfo.profileFile;
            mProfiler.profileFd = data.initProfilerInfo.profileFd;
            mProfiler.samplingInterval = data.initProfilerInfo.samplingInterval;
            mProfiler.autoStopProfiler = data.initProfilerInfo.autoStopProfiler;
            mProfiler.streamingOutput = data.initProfilerInfo.streamingOutput;
            if (data.initProfilerInfo.attachAgentDuringBind) {
                agent = data.initProfilerInfo.agent;
            }
        }       //给一些变量赋值
        //发送应用名称； 要在等待调试器之前做
        Process.setArgV0(data.processName);
        android.ddm.DdmHandleAppName.setAppName(data.processName, UserHandle.myUserId());
        VMRuntime.setProcessPackageName(data.appInfo.packageName);
        if (mProfiler.profileFd != null) {
            mProfiler.startProfiling();     //开始分析？
        }
        //如果应用程序是Honeycomb MR1或更早版本，请切换其AsyncTask实现以使用线程池执行程序。 通常，我们使用序列化的executor作为默认值。 这必须在主线程中发生，因此main looper设置正确
        if (data.appInfo.targetSdkVersion <= android.os.Build.VERSION_CODES.HONEYCOMB_MR1) {
            AsyncTask.setDefaultExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        Message.updateCheckRecycle(data.appInfo.targetSdkVersion);      //更新Message内部gCheckRecycle值
        //在P之前，对Bitmap进行内部解码的调用使用了BitmapFactory，它可以扩展以解决密度问题。在P中，我们切换到ImageDecoder，它跳过了高档代码以节省内存。如果旧版本的应用程序依赖于位图的大小而不考虑其密度，则ImageDecoder仍需要在旧版应用程序中进行扩展
        ImageDecoder.sApiLevel = data.appInfo.targetSdkVersion;
        //在产生新进程之前，将时区重置为系统时区。之所以需要这样做，是因为该进程产生后系统时区可能已更改。如果不这样做，此进程将具有不正确的系统时区
        TimeZone.setDefault(null);
        //设置LocaleList。 一旦我们创建了App Context，这种情况可能会改变
        LocaleList.setDefault(data.config.getLocales());
        synchronized (mResourcesManager) {
            //系统配置由于已预先加载，因此可能会更新，因此可能无法反映配置更改。可以安全地假定AppBindData中传递的配置对象是最新的
            mResourcesManager.applyConfigurationToResourcesLocked(data.config, data.compatInfo);    //应用配置
            mCurDefaultDisplayDpi = data.config.densityDpi;
            //这将调用mResourcesManager，因此将其保留在同步块内
            applyCompatConfiguration(mCurDefaultDisplayDpi);    //应用配置，内部调用ResourcesManager
        }
        data.info = getPackageInfoNoCheck(data.appInfo, data.compatInfo);   //内部构建一个LoadedApk
        if (agent != null) {
            handleAttachAgent(agent, data.info);        //附着代理到VM上？
        }
        //如果需要，将此进程切换到密度兼容模式
        if ((data.appInfo.flags&ApplicationInfo.FLAG_SUPPORTS_SCREEN_DENSITIES) == 0) {
            mDensityCompatMode = true;
            Bitmap.setDefaultDensity(DisplayMetrics.DENSITY_DEFAULT);   //设置默认Bitmap密度
        }
        updateDefaultDensity();     //内部再次调用setDefaultDensity设置密度
        final String use24HourSetting = mCoreSettings.getString(Settings.System.TIME_12_24);
        Boolean is24Hr = null;
        if (use24HourSetting != null) {
            is24Hr = "24".equals(use24HourSetting) ? Boolean.TRUE : Boolean.FALSE;  //是否是24小时制
        }
        // null:使用默认的区域设置进行12/24小时格式化，false:使用12小时格式，true:使用24小时格式
        DateFormat.set24HourTimePref(is24Hr);
        View.mDebugViewAttributes = mCoreSettings.getInt(Settings.Global.DEBUG_VIEW_ATTRIBUTES, 0) != 0;
        StrictMode.initThreadDefaults(data.appInfo);
        StrictMode.initVmDefaults(data.appInfo);        //Strict Mode设置默认值
        //我们已弃用Build.SERIAL，只有面向NMR1 SDK之前版本的应用才能看到它。由于现在已获得对串行的访问权限，因此我们下推了该值，在此我们在加载任何应用程序代码之前对其进行了修复
        try {
            Field field = Build.class.getDeclaredField("SERIAL");
            field.setAccessible(true);
            field.set(Build.class, data.buildSerial);       //设置SERIAL值
        } catch (NoSuchFieldException | IllegalAccessException e) {
        }
        if (data.debugMode != ApplicationThreadConstants.DEBUG_OFF) {       //debug设置
            //XXX应该可以选择更改端口
            Debug.changeDebugPort(8100);        //设置debug端口
            if (data.debugMode == ApplicationThreadConstants.DEBUG_WAIT) {
                Slog.w(TAG, "Application " + data.info.getPackageName() + " is waiting for the debugger on port 8100...");
                IActivityManager mgr = ActivityManager.getService();
                try {
                    mgr.showWaitingForDebugger(mAppThread, true);       //远程调用，AMS中debug相关，debugger尚未连接，需要等待，会弹出一个等待debugger的对话框
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
                Debug.waitForDebugger();    //等待调试器连接。一旦调试器附加，它就会返回，因此如果您想立即开始跟踪，那么您需要在waitForDebugger()调用之后放置一个断点。
                try {
                    mgr.showWaitingForDebugger(mAppThread, false);  //debugger已连接，关闭对话框
                } catch (RemoteException ex) {
                    throw ex.rethrowFromSystemServer();
                }
            } else {
                Slog.w(TAG, "Application " + data.info.getPackageName() + " can be debugged on port 8100...");
            }
        }
        //如果我们是可调试的，则允许应用程序生成systrace消息
        boolean isAppDebuggable = (data.appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        Trace.setAppTracingAllowed(isAppDebuggable);
        ThreadedRenderer.setDebuggingEnabled(isAppDebuggable || Build.IS_DEBUGGABLE);
        if (isAppDebuggable && data.enableBinderTracking) {
            Binder.enableTracing();
        }
        //由于我们设置时区的原因，在此进程中初始化默认的http代理
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "Setup proxies");
        final IBinder b = ServiceManager.getService(Context.CONNECTIVITY_SERVICE);
        if (b != null) {
            //在预引导模式下（进行初始启动以收集密码），并非所有系统都已启动。 这包括连接服务，因此如果我们无法获得连接，请不要崩溃。
            final IConnectivityManager service = IConnectivityManager.Stub.asInterface(b);
            try {
                final ProxyInfo proxyInfo = service.getProxyForNetwork(null);   //返回默认活动网络的代理，若无网络权限，返回null
                Proxy.setHttpProxySystemProperty(proxyInfo);        //设置系统属性，关于网络代理
            } catch (RemoteException e) {
                Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
                throw e.rethrowFromSystemServer();
            }
        }
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        //Instrumentation会影响类加载器，因此请在设置App Context之前加载它。
        final InstrumentationInfo ii;
        if (data.instrumentationName != null) {
            try {
                ii = new ApplicationPackageManager(null, getPackageManager()).getInstrumentationInfo(data.instrumentationName, 0);
            } catch (PackageManager.NameNotFoundException e) {
                throw new RuntimeException("Unable to find instrumentation info for: " + data.instrumentationName);
            }
            //警告潜在的ABI不匹配
            if (!Objects.equals(data.appInfo.primaryCpuAbi, ii.primaryCpuAbi) || !Objects.equals(data.appInfo.secondaryCpuAbi, ii.secondaryCpuAbi)) {
                Slog.w(TAG, "Package uses different ABI(s) than its instrumentation: " + "package[" + data.appInfo.packageName + "]: "
                        + data.appInfo.primaryCpuAbi + ", " + data.appInfo.secondaryCpuAbi + " instrumentation[" + ii.packageName + "]: " + ii.primaryCpuAbi + ", " + ii.secondaryCpuAbi);
            }
            mInstrumentationPackageName = ii.packageName;
            mInstrumentationAppDir = ii.sourceDir;
            mInstrumentationSplitAppDirs = ii.splitSourceDirs;
            mInstrumentationLibDir = getInstrumentationLibrary(data.appInfo, ii);
            mInstrumentedAppDir = data.info.getAppDir();
            mInstrumentedSplitAppDirs = data.info.getSplitAppDirs();
            mInstrumentedLibDir = data.info.getLibDir();
        } else {
            ii = null;
        }
        final ContextImpl appContext = ContextImpl.createAppContext(this, data.info);   //创建app context
        updateLocaleListFromAppContext(appContext, mResourcesManager.getConfiguration().getLocales());  //应用程序资源的LocaleList集可能被打乱了，因此首选的地区是位置0。我们必须在原始LocaleList中找到这个首选地区的索引
        if (!Process.isIsolated()) {        //当前进程是否处于隔离沙箱中
            final int oldMask = StrictMode.allowThreadDiskWritesMask();
            try {
                setupGraphicsSupport(appContext);       //生成一个缓存文件夹，用于存储生成/编译得到的图形相关代码，ThreadedRenderer、RenderScriptCacheDir
            } finally {
                StrictMode.setThreadPolicyMask(oldMask);
            }
        } else {
            ThreadedRenderer.setIsolatedProcess(true);  //隔离沙箱中的进程
        }
        //如果我们使用配置文件，请设置dex报告程序以将任何相关的dex负载通知软件包管理器。空闲维护作业将使用报告的信息来优化已加载的dex文件。
        //请注意，每个应用程序只需要一个全局报告器。 确保在调用onCreate之前执行此操作，以便我们可以捕获完整的应用程序启动。
        if (SystemProperties.getBoolean("dalvik.vm.usejitprofiles", false)) {
            BaseDexClassLoader.setReporter(DexLoadReporter.getInstance());
        }
        //安装网络安全配置提供程序。 这必须在加载应用程序代码之前发生，以防止在安装提供程序之前创建TLS对象实例的问题。
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "NetworkSecurityConfigProvider.install");
        NetworkSecurityConfigProvider.install(appContext);  //安装NSSP
        Trace.traceEnd(Trace.TRACE_TAG_ACTIVITY_MANAGER);
        //继续加载 instrumentation
        if (ii != null) {
            ApplicationInfo instrApp;
            try {
                instrApp = getPackageManager().getApplicationInfo(ii.packageName, 0, UserHandle.myUserId());
            } catch (RemoteException e) {
                instrApp = null;
            }
            if (instrApp == null) {
                instrApp = new ApplicationInfo();
            }
            ii.copyTo(instrApp);        //将ii的值赋给instrApp
            instrApp.initForUser(UserHandle.myUserId());        //初始化，给一些文件路径赋值
            final LoadedApk pi = getPackageInfo(instrApp, data.compatInfo, appContext.getClassLoader(), false, true, false);    //从mPackages中取出LoadedApk，若无则构造一个LoadedApk并放到mPackages中
            final ContextImpl instrContext = ContextImpl.createAppContext(this, pi);    //使用上面的pi重新构造一个app context
            try {
                final ClassLoader cl = instrContext.getClassLoader();   //data.instrumentationName来自ProcessRecord.instr
                mInstrumentation = (Instrumentation)cl.loadClass(data.instrumentationName.getClassName()).newInstance();    //Instrumentation实例化
            } catch (Exception e) {
                throw new RuntimeException("Unable to instantiate instrumentation " + data.instrumentationName + ": " + e.toString(), e);
            }
            final ComponentName component = new ComponentName(ii.packageName, ii.name);     //构造一个ComponentName
            mInstrumentation.init(this, instrContext, appContext, component, data.instrumentationWatcher, data.instrumentationUiAutomationConnection);  //只是给Instrumentation的一些变量赋值
            if (mProfiler.profileFile != null && !ii.handleProfiling && mProfiler.profileFd == null) {  //分析器，生成文件分析
                mProfiler.handlingProfiling = true;
                final File file = new File(mProfiler.profileFile);
                file.getParentFile().mkdirs();
                Debug.startMethodTracing(file.toString(), 8 * 1024 * 1024);
            }
        } else {
            mInstrumentation = new Instrumentation();
            mInstrumentation.basicInit(this);       //只设置ActivityThread，其他为null
        }
        if ((data.appInfo.flags&ApplicationInfo.FLAG_LARGE_HEAP) != 0) {    //manifest中设置的largeHeap为true时
            dalvik.system.VMRuntime.getRuntime().clearGrowthLimit();
        } else {
            //小堆，将其限制在当前的增长限制之内，然后让堆在增长限制达到非增长限制容量之后释放页面。 b / 18387825
            dalvik.system.VMRuntime.getRuntime().clampGrowthLimit();
        }
        //在应用程序和提供程序设置期间允许磁盘访问。 这可能会阻止处理有序广播，但是以后的处理可能最终会进行相同的磁盘访问
        Application app;
        final StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskWrites();
        final StrictMode.ThreadPolicy writesAllowedPolicy = StrictMode.getThreadPolicy();
        try {
            //如果要启动该应用程序以进行完全备份或还原，请在受限环境中使用base application class将其启动
            app = data.info.makeApplication(data.restrictedBackupMode, null);   //restrictedBackupMode为true，则强制使用android.app.Application，否则实例化manifest中的Application Name（内部使用Instrumentation.newApplication实例化Application）
            //传播自动填充兼容状态
            app.setAutofillCompatibilityEnabled(data.autofillCompatibilityEnabled);
            mInitialApplication = app;
            //不要以受限模式启动provider； 它们可能取决于应用程序的自定义Application类
            if (!data.restrictedBackupMode) {
                if (!ArrayUtils.isEmpty(data.providers)) {
                    installContentProviders(app, data.providers);
                    //对于包含content provider的进程，我们要确保“在某个时候”启用了JIT。
                    mH.sendEmptyMessageDelayed(H.ENABLE_JIT, 10*1000);
                }
            }
            //请在provider之后执行此操作，因为通常情况下instrumentation tests会开始其测试线程，我们不希望如此。
            try {
                mInstrumentation.onCreate(data.instrumentationArgs);    //在启动instrumentation 且未加载任何application 代码之前调用。 通常，这将实现为简单地调用start（）来启动instrumentation 线程，然后将在onStart（）中继续执行。如果不需要自己的线程-也就是说，您正在编写完全异步的instrumentation （返回事件循环以使应用程序可以运行），则可以在此处简单地开始您的instrumentation ，例如调用startActivity（Intent） 开始适当的应用程序第一个activity 
            }
            catch (Exception e) {
                throw new RuntimeException("Exception thrown in onCreate() of " + data.instrumentationName + ": " + e.toString(), e);
            }
            try {
                mInstrumentation.callApplicationOnCreate(app);  //内部执行application的onCreate()方法的调用。 默认实现只是调用该方法。注意：此方法将在onCreate（Bundle）之后立即调用。 instrumentation 测试通常在onCreate（）中启动其测试线程； 您需要注意这些之间的竞争。 （在它和其他所有东西之间，但是让我们从这里开始。）
            } catch (Exception e) {
                if (!mInstrumentation.onException(app, e)) {
                    throw new RuntimeException("Unable to create application " + app.getClass().getName() + ": " + e.toString(), e);
                }
            }
        } finally {
            //如果应用程序的目标是<O-MR1(27)，或者在启动过程中未更改线程策略，请破坏该策略以维护b / 36951662的行为
            if (data.appInfo.targetSdkVersion < Build.VERSION_CODES.O_MR1 || StrictMode.getThreadPolicy().equals(writesAllowedPolicy)) {
                StrictMode.setThreadPolicy(savedPolicy);
            }
        }
        //预加载字体资源
        FontsContract.setApplicationContextForResources(appContext);
        if (!Process.isIsolated()) {    //非隔离进程
            try {
                final ApplicationInfo info = getPackageManager().getApplicationInfo(data.appInfo.packageName, PackageManager.GET_META_DATA /*flags*/, UserHandle.myUserId());
                if (info.metaData != null) {
                    //预加载的字体资源信息
                    final int preloadedFontsResource = info.metaData.getInt(ApplicationInfo.METADATA_PRELOADED_FONTS, 0);
                    if (preloadedFontsResource != 0) {
                        data.info.getResources().preloadFonts(preloadedFontsResource);
                    }
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
    handleBindApplication中，初始化了Application类，并调用了其onCreate方法
    下一步就是启动第一个Activity，启动过程中会涉及Activity声明周期以及View的绘制流程

#### 启动Activity
    
    在ActivityManagerService.attachApplicationLocked中，在bindApplication后接着就是依次启动Activity、Service、BroadCast
    Activity相关操作--mStackSupervisor.attachApplicationLocked(app)   
    Service相关操作- -mServices.attachApplicationLocked(app, processName)
    BroadCast相关操作-sendPendingBroadcastsLocked(app)
    
    ActivityStackSupervisor.attachApplicationLocked：
        final String processName = app.processName;
        boolean didSomething = false;
        //mActivityDisplays，从displayId映射到显示当前状态，根据displayId生成一个ActivityDisplay，添加到mActivityDisplays中
        for (int displayNdx = mActivityDisplays.size() - 1; displayNdx >= 0; --displayNdx) {    //遍历mActivityDisplays
            final ActivityDisplay display = mActivityDisplays.valueAt(displayNdx);      //ActivityDisplay，在系统中，每个Display对应这些类之一。 能够容纳零个或多个附加的ActivityStack
            for (int stackNdx = display.getChildCount() - 1; stackNdx >= 0; --stackNdx) {
                final ActivityStack stack = display.getChildAt(stackNdx);   //从mStacks中取出ActivityStack。mStacks为此Display上的所有堆栈。顺序很重要，最上面的堆栈在所有其他堆栈的前面，最下面的堆栈在后面。由ActivityManager包类直接访问。任何更改列表的调用也应调用onStackOrderChanged()
                if (!isFocusedStack(stack)) {   //是否是mFocusedStack，当前正在接收输入或启动下一个activity的堆栈
                    continue;
                }
                stack.getAllRunningVisibleActivitiesLocked(mTmpActivityList);   //将mTaskHistory中的符合要求的ActivityRecord添加到mTmpActivityList中。mTaskHistory，历史记录所有Activity的TaskRecord
                final ActivityRecord top = stack.topRunningActivityLocked();    //拿到mTaskHistory中对当前用户可显示的记录
                final int size = mTmpActivityList.size();
                for (int i = 0; i < size; i++) {        //遍历mTmpActivityList
                    final ActivityRecord activity = mTmpActivityList.get(i);
                    if (activity.app == null && app.uid == activity.info.applicationInfo.uid
                            && processName.equals(activity.processName)) {  //ActivityRecord的进程信息为null？uid和进程名一样
                        try {
                            if (realStartActivityLocked(activity, app, top == activity /* andResume */, true /* checkConfig */)) {  //启动Activity
                                didSomething = true;
                            }
                        } catch (RemoteException e) {
                            //Exception in new application when starting activity " + top.intent.getComponent().flattenToShortString()
                            throw e;
                        }
                    }
                }
            }
        }
        if (!didSomething) {    //为false表示上面没有成功启动一个Activity
            ensureActivitiesVisibleLocked(null, 0, !PRESERVE_WINDOWS);  //确保所有需要在系统中可见的Activity实际上都在并更新它们的配置
        }
        return didSomething;
    这段主要是使历史记录中的Activity可见？
    历史记录中不存在当前APP，那么在哪里启动Activity？
    
    ActivityStackSupervisor.realStartActivityLocked：
        if (!allPausedActivitiesComplete()) {   //确保没有处于暂停状态的activity
            //当activity正在暂停时，我们会跳过启动任何新的activity，直到暂停完成为止。 注意：我们也对从暂停状态开始的activity执行此操作，因为这些activity将首先恢复，然后在客户端暂停
            if (DEBUG_SWITCH || DEBUG_PAUSE || DEBUG_STATES) 
                Slog.v(TAG_PAUSE, "realStartActivityLocked: Skipping start of r=" + r + " some activities pausing...");
            return false;
        }
        final TaskRecord task = r.getTask();
        final ActivityStack stack = task.getStack();
        beginDeferResume();         //开始推迟resume，以避免一次重复resume
        try {
            r.startFreezingScreenLocked(app, 0);    //仅当此activity当前已附加到应用程序并且该应用程序未被阻塞或无响应时，才冻结屏幕。 在任何其他情况下，我们都不能指望将屏幕解冻，因此最好保持原样
            //安排启动时间以收集有关慢速应用程序的信息
            r.startLaunchTickingLocked();       //延迟发送一个Message，以确定App卡顿？
            r.setProcess(app);      //设置进程信息？
            if (getKeyguardController().isKeyguardLocked()) {   //返回true表示键盘正在显示？
                r.notifyUnknownVisibilityLaunched();    //没有显示的activity永远不会添加窗口，因此没有必要等待它们进行relayout
            }
            //让窗口管理器根据新的activity顺序重新评估屏幕的方向。请注意，其结果是，它可以以新的方向回调到活动管理器中。我们不在乎，因为该activity当前未运行，因此无论如何我们都只是重新启动它
            if (checkConfig) {
                //在这里延迟恢复，因为我们不久将要启动新的activity。我们不想在确保配置并尝试恢复聚焦堆栈的顶部activity的同时执行同一记录的冗余启动
                ensureVisibilityAndConfig(r, r.getDisplayId(), false /* markFrozenIfConfigChanged */, true /* deferResume */);  //确保所有activity可见、更新方向和配置
            }
            if (r.getStack().checkKeyguardVisibility(r, true /* shouldBeVisible */, true /* isTop */)) {  //如果可见时考虑了密钥保护状态，则为真，否则为假
                //仅当基于键盘锁状态允许activity可见时，才将可见性设置为true。 这样可以避免在窗口管理器中将其设置为运动，该运动随后由于后来的调用而取消，以确保将可见性设置为false的可见activity
                r.setVisibility(true);
            }
            final int applicationInfoUid = (r.info.applicationInfo != null) ? r.info.applicationInfo.uid : -1;
            if ((r.userId != app.userId) || (r.appInfo.uid != applicationInfoUid)) {
                //User ID for activity changing for " + r + " appInfo.uid=" + r.appInfo.uid + " info.ai.uid=" + applicationInfoUid + " old=" + r.app + " new=" + app);
            }
            app.waitingToKill = null;
            r.launchCount++;
            r.lastLaunchTime = SystemClock.uptimeMillis();
            if (DEBUG_ALL) Slog.v(TAG, "Launching: " + r);      //正在启动Activity
            int idx = app.activities.indexOf(r);
            if (idx < 0) {
                app.activities.add(r);      //将ActivityRecord添加到activities（进程中运行的所有activity）
            }
            mService.updateLruProcessLocked(app, true, null);   //更新进程的Lru列表
            mService.updateOomAdjLocked();      //OOM相关？
            final LockTaskController lockTaskController = mService.getLockTaskController();
            if (task.mLockTaskAuth == LOCK_TASK_AUTH_LAUNCHABLE || task.mLockTaskAuth == LOCK_TASK_AUTH_LAUNCHABLE_PRIV
                    || (task.mLockTaskAuth == LOCK_TASK_AUTH_WHITELISTED && lockTaskController.getLockTaskModeState() == LOCK_TASK_MODE_LOCKED)) {
                lockTaskController.startLockTaskMode(task, false, 0 /* blank UID */);
            }
            try {
                if (app.thread == null) {
                    throw new RemoteException();
                }
                List<ResultInfo> results = null;
                List<ReferrerIntent> newIntents = null;
                if (andResume) {
                    //如果activity在启动后立即暂停，我们不需要提供新的Intent和/或设置结果
                    results = r.results;
                    newIntents = r.newIntents;
                }
                if (DEBUG_SWITCH) Slog.v(TAG_SWITCH, "Launching: " + r + " icicle=" + r.icicle + " with results=" + results + " newIntents=" + newIntents + " andResume=" + andResume);
                if (r.isActivityTypeHome()) {   //activity类型为home？
                    //Home进程是任务的根进程
                    mService.mHomeProcess = task.mActivities.get(0).app;
                }
                mService.notifyPackageUse(r.intent.getComponent().getPackageName(), PackageManager.NOTIFY_PACKAGE_USE_ACTIVITY);    //通知PMS干啥？
                r.sleeping = false;
                r.forceNewConfig = false;
                mService.getAppWarningsLocked().onStartActivity(r); //在activity启动时调用，展示一些弹框：showUnsupportedCompileSdkDialog、showUnsupportedDisplaySizeDialog、showDeprecatedTargetDialog
                mService.showAskCompatModeDialogLocked(r);      //兼容模式提示框？
                r.compat = mService.compatibilityInfoForPackageLocked(r.info.applicationInfo);  //兼容模式包？
                ProfilerInfo profilerInfo = null;
                if (mService.mProfileApp != null && mService.mProfileApp.equals(app.processName)) {
                    if (mService.mProfileProc == null || mService.mProfileProc == app) {    //分析程序？
                        mService.mProfileProc = app;
                        ProfilerInfo profilerInfoSvc = mService.mProfilerInfo;
                        if (profilerInfoSvc != null && profilerInfoSvc.profileFile != null) {
                            if (profilerInfoSvc.profileFd != null) {
                                try {
                                    profilerInfoSvc.profileFd = profilerInfoSvc.profileFd.dup();
                                } catch (IOException e) {
                                    profilerInfoSvc.closeFd();
                                }
                            }
                            profilerInfo = new ProfilerInfo(profilerInfoSvc);
                        }
                    }
                }
                app.hasShownUi = true;
                app.pendingUiClean = true;
                app.forceProcessStateUpTo(mService.mTopProcessState);   //强制更新进程状态，mTopProcessState用于运行top activity的进程的进程状态。这在TOP和TOP_SLEEPING之间更改为跟随mSleeping
                //因为我们可能要在系统进程中启动活动，所以它可能不会通过Binder接口创建新的配置。因此，我们必须始终在此处创建一个新的配置
                final MergedConfiguration mergedConfiguration = new MergedConfiguration(mService.getGlobalConfiguration(), r.getMergedOverrideConfiguration());
                r.setLastReportedConfiguration(mergedConfiguration);    //将最后报告的配置设置为客户机。每当向客户端发送此activity的新合并配置时，应调用
                logIfTransactionTooLarge(r.intent, r.icicle);   //intent中的bundle大小和icicle的大小之和过大时打印log（大于200000）
                //创建Activity的启动事务
                final ClientTransaction clientTransaction = ClientTransaction.obtain(app.thread, r.appToken);   //obtain从对象池中取出一个实例，然后将参数赋给这个实例
                //LaunchActivityItem作为Callback，最终会调用其execute或preExecute方法
                clientTransaction.addCallback(LaunchActivityItem.obtain(new Intent(r.intent), System.identityHashCode(r), r.info,
                        //TODO:让它采用合并的配置，而不是单独的全局配置和替代配置
                        mergedConfiguration.getGlobalConfiguration(), mergedConfiguration.getOverrideConfiguration(), r.compat,
                        r.launchedFromPackage, task.voiceInteractor, app.repProcState, r.icicle, r.persistentState, results, newIntents, mService.isNextTransitionForward(), profilerInfo));
                //设置所需的最终状态
                final ActivityLifecycleItem lifecycleItem;
                if (andResume) {
                    lifecycleItem = ResumeActivityItem.obtain(mService.isNextTransitionForward());
                } else {
                    lifecycleItem = PauseActivityItem.obtain();     //生命周期相关？
                }
                clientTransaction.setLifecycleStateRequest(lifecycleItem);      //将lifecycleItem赋给clientTransaction的内部变量
                //提交事务？安排启动Activity？
                mService.getLifecycleManager().scheduleTransaction(clientTransaction);  //scheduleTransaction内部会执行ClientTransaction的schedule方法，最终调用ApplicationThread的scheduleTransaction函数
                if ((app.info.privateFlags & ApplicationInfo.PRIVATE_FLAG_CANT_SAVE_STATE) != 0 && mService.mHasHeavyWeightFeature) {
                    //这可能是一个重量级的进程（多进程？）！请注意，程序包管理器将确保仅activity可以在.apk的主进程中运行，这是唯一被认为很繁琐的事情
                    if (app.processName.equals(app.info.packageName)) {
                        if (mService.mHeavyWeightProcess != null && mService.mHeavyWeightProcess != app) {
                            Slog.w(TAG, "Starting new heavy weight process " + app + " when already running " + mService.mHeavyWeightProcess);
                        }
                        mService.mHeavyWeightProcess = app;
                        Message msg = mService.mHandler.obtainMessage(ActivityManagerService.POST_HEAVY_NOTIFICATION_MSG);
                        msg.obj = r;
                        mService.mHandler.sendMessage(msg);
                    }
                }
            } catch (RemoteException e) {
                if (r.launchFailed) {
                    // 这是我们第二次失败-finish activity并放弃
                    Slog.e(TAG, "Second failure launching " + r.intent.getComponent().flattenToShortString() + ", giving up", e);
                    mService.appDiedLocked(app);    //杀掉进程
                    stack.requestFinishActivityLocked(r.appToken, Activity.RESULT_CANCELED, null, "2nd-crash", false);
                    return false;
                }
                // 这是我们第一次失败-重新启动进程，然后重试
                r.launchFailed = true;
                app.activities.remove(r);
                throw e;
            }
        } finally {
            endDeferResume();       //结束延迟恢复并确定是否可以调用恢复
        }
        r.launchFailed = false;     //activity启动成功
        if (stack.updateLRUListLocked(r)) {     //更新ActivityStack中的LRU列表
            Slog.w(TAG, "Activity " + r + " being launched, but already in LRU list");
        }
        // TODO(lifecycler):恢复或暂停请求是启动事务的一部分，因此应相应地更新状态
        if (andResume && readyToResume()) {
            //作为启动过程的一部分，ActivityThread还执行恢复
            stack.minimalResumeActivityLocked(r);       //resume Activity
        } else {
            //此活动不是在恢复状态下开始的...看起来我们要求它暂停+停止（但仍保持可见），并且它已经这样做并报告了当前的冰柱和其他状态
            if (DEBUG_STATES) Slog.v(TAG_STATES, "Moving to PAUSED: " + r + " (starting in paused state)");
            r.setState(PAUSED, "realStartActivityLocked");  //更新Activity状态为PAUSED？
        }
        //如果需要，启动新版本设置屏幕。我们在启动初始Activity（即home）之后执行此操作，以便它可以有机会在后台进行初始化，从而使切换回它的速度更快并且外观更好。
        if (isFocusedStack(stack)) {
            mService.getActivityStartController().startSetupActivity();     //启动设置页面，应该是在启动Home时启动的
        }
        //更新我们绑定到的任何服务，这可能会关心他们的客户是否有Activity
        if (r.app != null) {
            mService.mServices.updateServiceConnectionActivitiesLocked(r.app);  //更新和Activity绑定的相关服务？
        }
        return true;
    realStartActivityLocked主要做了启动Activity，然后执行resume？
    mService.getLifecycleManager().scheduleTransaction(clientTransaction)提交事务，内部会执行ClientTransaction的schedule方法，最终调用ApplicationThread的scheduleTransaction函数
    ApplicationThread的scheduleTransaction函数中又执行了ActivityThread的scheduleTransaction方法
    ActivityThread继承自ClientTransactionHandler，没有重载scheduleTransaction，即实际执行的是ClientTransactionHandler的scheduleTransaction方法
    
    ClientTransactionHandler.scheduleTransaction：
        //准备并安排事务执行
        transaction.preExecute(this);       //为ClientTransaction的方法
        sendMessage(ActivityThread.H.EXECUTE_TRANSACTION, transaction);     //在ActivityThread中发送Message到mH中，执行TransactionExecutor.execute(ClientTransaction);
    ClientTransaction.preExecute：
        //在客户端调度事务时，执行需要执行的操作
        if (mActivityCallbacks != null) {      //上面添加的Callback是添加到mActivityCallbacks中的
            final int size = mActivityCallbacks.size();
            for (int i = 0; i < size; ++i) {    //遍历
                mActivityCallbacks.get(i).preExecute(clientTransactionHandler, mActivityToken);     //执行LaunchActivityItem的preExecute
            }
        }
        if (mLifecycleStateRequest != null) {   //mLifecycleStateRequest为上面赋予的lifecycleItem（ActivityLifecycleItem）
            mLifecycleStateRequest.preExecute(clientTransactionHandler, mActivityToken);    //执行ActivityLifecycleItem的preExecute
        }
    LaunchActivityItem.preExecute（只是更新了一些值，未做实际的动作）：
        client.updateProcessState(mProcState, false);   //实际又调用ApplicationThread的updateProcessState方法，主要是更新进程状态
        client.updatePendingConfiguration(mCurConfig);  //实际也调用ApplicationThread的updatePendingConfiguration方法，给一个待使用的状态变量赋值
        
    lifecycleItem是一个抽象类，其实现类有ResumeActivityItem、PauseActivityItem，实际执行的是它们的preExecute方法
    ResumeActivityItem内部实际也是更新进程状态，PauseActivityItem中未实现该方法
    这些preExecute方法的功能大体一致，都是更改一些待使用的变量的值，实际动作看ActivityThread的sendMessage
    
    TransactionExecutor.execute(ClientTransaction)：
        //按顺序执行回调
        final IBinder token = transaction.getActivityToken();
        log("Start resolving transaction for client: " + mTransactionHandler + ", token: " + token);
        executeCallbacks(transaction);      //依次执行回调方法，实际是LaunchActivityItem的execute、postExecute
        executeLifecycleState(transaction); //会执行lifecycleItem的execute、postExecute方法（ResumeActivityItem或PauseActivityItem）
        mPendingActions.clear();
        log("End resolving transaction");
    TransactionExecutor.execute中会依次执行LaunchActivityItem的execute、postExecute，lifecycleItem的execute、postExecute
    
    LaunchActivityItem中未实现postExecute方法，在execute中最终会执行ActivityThread.handleLaunchActivity(r, pendingActions, null /* customIntent */)
    
    lifecycleItem的子类ResumeActivityItem：
        先执行execute，其中最终会执行到ActivityThread.handleResumeActivity(token, true /* finalStateRequest */, mIsForward, "RESUME_ACTIVITY")
        然后是postExecute，其中执行--ActivityManager.getService().activityResumed(token); 即通知AMS Activity已经resume了

    lifecycleItem的子类PauseActivityItem：
        execute中最终会执行到ActivityThread.handlePauseActivity(token, mFinished, mUserLeaving, mConfigChanges, pendingActions, "PAUSE_ACTIVITY_ITEM")
        postExecute中执行--ActivityManager.getService().activityPaused(token); 即通知AMS Activity已经Pause了
    
    在此时的调用顺序应该是：
        LaunchActivityItem.execute --> ActivityThread.handleLaunchActivity
        --> ResumeActivityItem.execute --> ActivityThread.handleResumeActivity
        --> ResumeActivityItem.postExecute --> ActivityManager.getService().activityResumed
        --> 然后是根据用户操作最相应的状态切换等

#### 真正的Activity启动过程
    
    Activity启动的扩展实现。当server请求启动或重新启动时使用
    ActivityThread.handleLaunchActivity：
        //如果我们准备在后台运行时准备gc，那么我们又恢复了活动状态，因此请跳过它
        unscheduleGcIdler();        //移除GC消息，removeIdleHandler(mGcIdler)和mH.removeMessages(H.GC_WHEN_IDLE)
        mSomeActivitiesChanged = true;
        if (r.profilerInfo != null) {       //开始分析
            mProfiler.setProfiler(r.profilerInfo);
            mProfiler.startProfiling();
        }
        //确保我们使用最新的配置运行
        handleConfigurationChanged(null, null);
        //Handling launch of r
        //在创建activity之前进行初始化
        if (!ThreadedRenderer.sRendererDisabled) {  //进程可以将此标志设置为false，以防止使用线程渲染
            GraphicsEnvironment.earlyInitEGL();     //启动一个后台线程来初始化EGL。初始化EGL涉及到加载和初始化图形驱动程序。一些驱动程序需要几毫秒的时间来完成这个任务，所以当一个应用程序试图渲染它的第一帧时，按需这样做会直接增加用户可见的应用程序启动延迟。通过在单独的线程上更早地启动它，通常可以在UI准备好绘制之前完成它。应该只在chooseDriver()之后调用
        }
        WindowManagerGlobal.initialize();       //初始化WindowManagerService，得到其本地代理
        final Activity a = performLaunchActivity(r, customIntent);      //启动Activity的核心处理
        if (a != null) {
            r.createdConfig = new Configuration(mConfiguration);
            reportSizeConfigurations(r);      //最终是通过AMS给ActivityRecord的一些SizeConfiguration设置值，这些值来自r.activity.getResources().getSizeConfigurations()
            if (!r.activity.mFinished && pendingActions != null) {
                pendingActions.setOldState(r.state);
                pendingActions.setRestoreInstanceState(true);
                pendingActions.setCallOnPostCreate(true);
            }
        } else {
            //如果出于任何原因出现错误，请告诉Activity Manager停止我们
            try {
                ActivityManager.getService().finishActivity(r.token, Activity.RESULT_CANCELED, null, Activity.DONT_FINISH_TASK_WITH_ACTIVITY);
            } catch (RemoteException ex) {
                throw ex.rethrowFromSystemServer();
            }
        }
        return a;
    通过performLaunchActivity启动Activity后，检查是否启动成功，返回不为null表示成功，若不成功，则通过AMS finish当前Activity
    ActivityThread.performLaunchActivity：
        ActivityInfo aInfo = r.activityInfo;
        if (r.packageInfo == null) {
            r.packageInfo = getPackageInfo(aInfo.applicationInfo, r.compatInfo, Context.CONTEXT_INCLUDE_CODE);
        }
        ComponentName component = r.intent.getComponent();
        if (component == null) {
            component = r.intent.resolveActivity(mInitialApplication.getPackageManager());  //解析ComponentName
            r.intent.setComponent(component);
        }
        if (r.activityInfo.targetActivity != null) {
            component = new ComponentName(r.activityInfo.packageName, r.activityInfo.targetActivity);   //ComponentName为实际要启动的Activity类名、包名信息
        }
        ContextImpl appContext = createBaseContextForActivity(r);   //内部通过ContextImpl构建一个Activity Context
        Activity activity = null;
        try {
            java.lang.ClassLoader cl = appContext.getClassLoader();
            activity = mInstrumentation.newActivity(cl, component.getClassName(), r.intent);    //实例化Activity，内部通过AppComponentFactory的instantiateApplication方法，最终使用cl.loadClass(className).newInstance()实例化（ClassLoader）
            StrictMode.incrementExpectedActivityCount(activity.getClass());
            r.intent.setExtrasClassLoader(cl);
            r.intent.prepareToEnterProcess();
            if (r.state != null) {
                r.state.setClassLoader(cl);
            }
        } catch (Exception e) {
            if (!mInstrumentation.onException(activity, e)) {
                throw new RuntimeException("Unable to instantiate activity " + component + ": " + e.toString(), e);
            }
        }
        try {
            Application app = r.packageInfo.makeApplication(false, mInstrumentation);   //直接返回Application实例，在上面已经实例化了
            if (localLOGV) Slog.v(TAG, "Performing launch of " + r);
            if (localLOGV) Slog.v(TAG, r + ": app=" + app + ", appName=" + app.getPackageName() + ", pkg=" + r.packageInfo.getPackageName() + ", comp=" + r.intent.getComponent().toShortString() + ", dir=" + r.packageInfo.getAppDir());
            if (activity != null) {
                CharSequence title = r.activityInfo.loadLabel(appContext.getPackageManager());  //加载xml中的label标签内容
                Configuration config = new Configuration(mCompatConfiguration);     //拷贝赋值
                if (r.overrideConfig != null) {
                    config.updateFrom(r.overrideConfig);    //更新config中的一些配置参数值
                }
                if (DEBUG_CONFIGURATION) Slog.v(TAG, "Launching activity " + r.activityInfo.name + " with config " + config);
                Window window = null;
                if (r.mPendingRemoveWindow != null && r.mPreserveWindow) {
                    window = r.mPendingRemoveWindow;
                    r.mPendingRemoveWindow = null;
                    r.mPendingRemoveWindowManager = null;
                }
                appContext.setOuterContext(activity);
                //attach中，首先调用attachBaseContext，然后是Fragments.attachHost，接着new了一个PhoneWindow作为UI的承载，再对window的一些参数赋值
                activity.attach(appContext, this, getInstrumentation(), r.token, r.ident, app, r.intent, r.activityInfo, title, r.parent,
                        r.embeddedID, r.lastNonConfigurationInstances, config, r.referrer, r.voiceInteractor, window, r.configCallback);
                if (customIntent != null) {
                    activity.mIntent = customIntent;    //intent
                }
                r.lastNonConfigurationInstances = null;
                checkAndBlockForNetworkAccess();    //检查mNetworkBlockSeq是否为INVALID_PROC_STATE_SEQ，如果是，则立即返回。否则，对ActivityManagerService进行阻塞调用，以等待网络规则更新
                activity.mStartedActivity = false;
                int theme = r.activityInfo.getThemeResource();
                if (theme != 0) {
                    activity.setTheme(theme);       //为Activity设置主题
                }
                activity.mCalled = false;
                if (r.isPersistable()) {        //是否本地持久化
                    mInstrumentation.callActivityOnCreate(activity, r.state, r.persistentState);    //内部调用Activity的performCreate，其中会先调用到onCreate，然后是mFragments.dispatchActivityCreated（最终应该是Fragment的相关回调）
                } else {
                    mInstrumentation.callActivityOnCreate(activity, r.state);       //同上
                }
                if (!activity.mCalled) {    //mCalled会在onCreate中被置为true，所以必须调用super.onCreate，否则会抛异常
                    throw new SuperNotCalledException("Activity " + r.intent.getComponent().toShortString() + " did not call through to super.onCreate()");
                }
                r.activity = activity;
            }
            r.setState(ON_CREATE);  //设置Activity状态
            mActivities.put(r.token, r);        //将Activity添加到集合中
        } catch (SuperNotCalledException e) {
            throw e;
        } catch (Exception e) {
            if (!mInstrumentation.onException(activity, e)) {
                throw new RuntimeException("Unable to start activity " + component + ": " + e.toString(), e);
            }
        }
        return activity;
    上面主要是实例化了Activity，并且调用了其attach、onCreate方法
    attach中，首先调用attachBaseContext，然后是Fragments.attachHost，接着new了一个PhoneWindow作为UI容器，再对window的一些参数赋值
    
    ActivityThread.handleResumeActivity：
        //如果我们准备在后台运行后使用gc，那么我们又恢复了活动状态，因此请跳过它
        unscheduleGcIdler();        //移除GC消息，removeIdleHandler(mGcIdler)和mH.removeMessages(H.GC_WHEN_IDLE)
        mSomeActivitiesChanged = true;
        // TODO 将resumeArgs推送到Activity中以供考虑
        final ActivityClientRecord r = performResumeActivity(token, finalStateRequest, reason);  //Resume the activity
        if (r == null) {
            //我们实际上并未恢复Activity，因此跳过了任何后续操作
            return;
        }
        final Activity a = r.activity;
        if (localLOGV) {
            Slog.v(TAG, "Resume " + r + " started activity: " + a.mStartedActivity + ", hideForNow: " + r.hideForNow + ", finished: " + a.mFinished);
        }
        final int forwardBit = isForward ? WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION : 0;
        //如果尚未将窗口添加到窗口管理器，并且此人未完成自己的任务或未开始其他Activity，则继续添加窗口
        boolean willBeVisible = !a.mStartedActivity;    //mStartedActivity为false，该项为true
        if (!willBeVisible) {
            try {
                willBeVisible = ActivityManager.getService().willActivityBeVisible(a.getActivityToken());
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
        }
        if (r.window == null && !a.mFinished && willBeVisible) {
            r.window = r.activity.getWindow();      //设置容器
            View decor = r.window.getDecorView();   //拿到 DecorView
            decor.setVisibility(View.INVISIBLE);    //占位但不显示？
            ViewManager wm = a.getWindowManager();
            WindowManager.LayoutParams l = r.window.getAttributes();
            a.mDecor = decor;       //赋值给Activity
            l.type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION;
            l.softInputMode |= forwardBit;
            if (r.mPreserveWindow) {
                a.mWindowAdded = true;
                r.mPreserveWindow = false;
                //通常，ViewRoot在addView->ViewRootImpl.setView中给Activity设置回调。如果我们改为重用decor view，则必须通知根视图回调可能已更改
                ViewRootImpl impl = decor.getViewRootImpl();        //View绘制相关的重要类
                if (impl != null) {
                    impl.notifyChildRebuilt();      //重绘View？通知我们，在进行窗口保存操作后，我们的孩子已经重建。在这些情况下，我们保留相同的DecorView，但控制它的Activity是不同的实例，我们需要更新回调
                }
            }
            if (a.mVisibleFromClient) {
                if (!a.mWindowAdded) {
                    a.mWindowAdded = true;
                    wm.addView(decor, l);       //add view
                } else {
                    //该Activity将获得此LayoutParams更改的回调。但是，那时将不设置decor（在此方法中设置），因此将不执行任何操作。该调用确保回调在decor集发生
                    a.onWindowAttributesChanged(l);
                }
            }
            //如果已经添加了该窗口，但是在恢复过程中我们开始了另一个Activity，则不要使该窗口可见
        } else if (!willBeVisible) {
            if (localLOGV) Slog.v(TAG, "Launch " + r + " mStartedActivity set");
            r.hideForNow = true;
        }
        //摆脱任何闲荡的事物
        cleanUpPendingRemoveWindows(r, false /* force */);  
        //现在，如果已添加该窗口，则该窗口是可见的，我们不仅可以完成操作，而且也不会开始其他activity
        if (!r.activity.mFinished && willBeVisible && r.activity.mDecor != null && !r.hideForNow) {
            if (r.newConfig != null) {
                performConfigurationChangedForActivity(r, r.newConfig);     //应用配置，若配置有更新，则会调用到activity.onConfigurationChanged
                if (DEBUG_CONFIGURATION) {
                    Slog.v(TAG, "Resuming activity " + r.activityInfo.name + " with newConfig " + r.activity.mCurrentConfig);
                }
                r.newConfig = null;
            }
            if (localLOGV) Slog.v(TAG, "Resuming " + r + " with isForward=" + isForward);
            WindowManager.LayoutParams l = r.window.getAttributes();
            if ((l.softInputMode & WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION) != forwardBit) {
                l.softInputMode = (l.softInputMode & (~WindowManager.LayoutParams.SOFT_INPUT_IS_FORWARD_NAVIGATION)) | forwardBit;
                if (r.activity.mVisibleFromClient) {
                    ViewManager wm = a.getWindowManager();
                    View decor = r.window.getDecorView();
                    wm.updateViewLayout(decor, l);      //更新View？
                }
            }
            r.activity.mVisibleFromServer = true;
            mNumVisibleActivities++;
            if (r.activity.mVisibleFromClient) {
                r.activity.makeVisible();   //会将decor添加到WindowManager中，并使Decor可见（mDecor.setVisibility(View.VISIBLE)）
            }
        }
        r.nextIdle = mNewActivities;
        mNewActivities = r;
        if (localLOGV) Slog.v(TAG, "Scheduling idle handler for " + r);
        Looper.myQueue().addIdleHandler(new Idler());       //调度空闲处理程序
    首先在performResumeActivity中调用到Activity的onResume，然后对Window做一些操作，View的绘制应该在onResume中？
    在onStart、onResume执行后，会应用新的配置，若配置有更新则会调用到activity.onConfigurationChanged
    
    ActivityThread.performResumeActivity：
        final ActivityClientRecord r = mActivities.get(token);  //在performLaunch时，已经将Activity添加到mActivities中了
        if (localLOGV) {
            Slog.v(TAG, "Performing resume of " + r + " finished=" + r.activity.mFinished);
        }
        if (r == null || r.activity.mFinished) {
            return null;
        }
        if (r.getLifecycleState() == ON_RESUME) {   //状态重复
            if (!finalStateRequest) {
                final RuntimeException e = new IllegalStateException("Trying to resume activity which is already resumed");
                Slog.e(TAG, e.getMessage(), e);
                Slog.e(TAG, r.getStateString());
                // TODO(lifecycler):当activity收到具有重新启动请求和“恢复的”最终状态请求的两个后续事务，并且第二次重新启动被省略时，可能会出现双重恢复请求。 我们仍然尝试为最终状态处理两个恢复请求。 对于除此以外的其他情况，我们预计不会发生
            }
            return null;
        }
        if (finalStateRequest) {
            r.hideForNow = false;
            r.activity.mStartedActivity = false;
        }
        try {
            r.activity.onStateNotSaved();   //在onResume()出现之前，以及其他pre-resume的回调（例如onNewIntent(Intent)和onActivityResult(int，int，Intent))之前调用。这主要是为了向activity提示不再保存其状态-通常将在onSaveInstanceState（Bundle）之后且在再次resumed/started activity之前调用它
            r.activity.mFragments.noteStateNotSaved();  //将fragment状态标记为未保存。这允许“state loss”检测
            checkAndBlockForNetworkAccess();    //检查mNetworkBlockSeq是否为INVALID_PROC_STATE_SEQ，如果是，则立即返回。否则，对ActivityManagerService进行阻塞调用，以等待网络规则更新
            if (r.pendingIntents != null) {
                deliverNewIntents(r, r.pendingIntents);     //内部最终会执行对activity的onNewIntent(Intent)方法的调用
                r.pendingIntents = null;
            }
            if (r.pendingResults != null) {
                deliverResults(r, r.pendingResults, reason); //内部最终会执行activity/fragment的onActivityResult方法，或者是activity/fragment的onRequestPermissionsResult方法
                r.pendingResults = null;
            }
            r.activity.performResume(r.startsNotResumed, reason);   //会依次执行activity/fragment的onRestart、onStart、onResume，在调用onResume后才安装当前状态栏和菜单
            r.state = null;
            r.persistentState = null;
            r.setState(ON_RESUME);  //设置activity状态
        } catch (Exception e) {
            if (!mInstrumentation.onException(r.activity, e)) {
                throw new RuntimeException("Unable to resume activity " + r.intent.getComponent().toShortString() + ": " + e.toString(), e);
            }
        }
        return r;
    performResumeActivity，在activity.performResume中会执行到Activity的onStart和onResume
    在上面可知，activity中的相关方法的调用顺序为：
        onNewIntent -> onActivityResult -> onRequestPermissionsResult -> onRestart -> onStart -> onResume -> onConfigurationChanged
        onActivityResult和onRequestPermissionsResult的顺序不确定，也可能是onRequestPermissionsResult先于onActivityResult
    在调用onResume后才安装当前状态栏和菜单
    Fragment的生命周期是与Activity的生命周期密切相关的
    
    到此，从Activity实例化到Activity的onResume方法执行的整个流程已梳理完毕，后续将是View绘制相关流程