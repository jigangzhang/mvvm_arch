
## APP进程启动流程

[参考](http://liuwangshu.cn/framework/applicationprocess/1.html)

#### 概述
    
    要想启动一个应用程序，首先要保证这个应用程序所需要的应用程序进程已经被启动。
    ActivityManagerService在启动应用程序时会检查这个应用程序需要的应用程序进程是否存在，
    不存在就会请求Zygote进程将需要的应用程序进程启动。
    Zygote的Java框架层中，会创建一个Server端的Socket，这个Socket用来等待ActivityManagerService来请求Zygote来创建新的应用程序进程的。
    Zygote进程通过fork自身创建的应用程序进程，这样应用程序进程就会获得Zygote进程在启动时创建的虚拟机实例。
    在应用程序创建过程中除了获取虚拟机实例，还可以获得Binder线程池和消息循环，这样运行在应用进程中应用程序就可以方便的使用Binder进行进程间通信以及消息处理机制了。
    
#### 步骤

    点击Home上的App图标时，会调用Launcher的startActivitySafely方法：
    /packages/apps/Launcher3/src/com/android/launcher3/Launcher.startActivitySafely：
        boolean success = super.startActivitySafely(v, intent, item);
        if (success && v instanceof BubbleTextView) {
            //将其设置为启动 将用户从启动器导航到其他位置的 活动 的视图。
            //因为当activity完成启动时没有回调，启用按下状态并保存此引用，以便在返回Launcher时重置按下状态。
            BubbleTextView btv = (BubbleTextView) v;
            btv.setStayPressed(true);
            setOnResumeCallback(btv);
        }
        return success;
    Launcher继承自BaseDraggingActivity，其startActivitySafely方法：
        if (mIsSafeModeEnabled && !Utilities.isSystemApp(this, intent)) {
            return false;
        }
        //只有当快捷方式没有选择退出时，才使用新的动画启动（这是一个私人和Launcher之间的契约，将来可能会被忽略）
        boolean useLaunchAnimation = (v != null) && !intent.hasExtra(INTENT_EXTRA_IGNORE_LAUNCH_ANIMATION);
        Bundle optsBundle = useLaunchAnimation ? getActivityLaunchOptionsAsBundle(v) : null;
        UserHandle user = item == null ? null : item.user;
        // Prepare intent
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (v != null) {
            intent.setSourceBounds(getViewBounds(v));
        }
        boolean isShortcut = Utilities.ATLEAST_MARSHMALLOW && (item instanceof ShortcutInfo) && (item.itemType == Favorites.ITEM_TYPE_SHORTCUT || item.itemType == Favorites.ITEM_TYPE_DEEP_SHORTCUT) && !((ShortcutInfo) item).isPromise();
        if (isShortcut) {
            //由于遗留的原因，快捷方式需要一些特殊的检查
            startShortcutIntentSafely(intent, optsBundle, item);
        } else if (user == null || user.equals(Process.myUserHandle())) {   //myUserHandle返回用户句柄，home应用的用户句柄和要启动的App的用户句柄应该不同
            //可能会启动一些统计activity
            startActivity(intent, optsBundle);
        } else {
            LauncherAppsCompat.getInstance(this).startActivityForProfile(intent.getComponent(), user, intent.getSourceBounds(), optsBundle);
        }
        getUserEventDispatcher().logAppLaunch(v, intent);
        return true;
    以快捷方式启动App会执行startShortcutIntentSafely
    BaseDraggingActivity.startShortcutIntentSafely(Intent intent, Bundle optsBundle, ItemInfo info)：
        StrictMode.VmPolicy oldPolicy = StrictMode.getVmPolicy();
        try {
            // 暂时禁用所有默认检查的死亡惩罚。比如，包含文件Uri的快捷方式会导致崩溃，因为penaltyDeathOnFileUriExposure在NYC上是默认启用的。
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build());
            if (info.itemType == LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT) {
                String id = ((ShortcutInfo) info).getDeepShortcutId();
                String packageName = intent.getPackage();
                DeepShortcutManager.getInstance(this).startShortcut(packageName, id, intent.getSourceBounds(), optsBundle, info.user);
            } else {
                //可能会启动一些统计activity
                startActivity(intent, optsBundle);
            }
        } finally {
            StrictMode.setVmPolicy(oldPolicy);
        } 
    LauncherSettings.Favorites.ITEM_TYPE_DEEP_SHORTCUT，代表是一个应用程序创建的深层快捷方式
    以快捷方式启动，若是 ITEM_TYPE_SHORTCUT 会调用startActivity？若是 ITEM_TYPE_DEEP_SHORTCUT 会调用startShortcut？
    上面若useLaunchAnimation为false，optsBundle为null，否则，最终调用ActivityOptions的makeClipRevealAnimation 赋予动画效果（新activity从屏幕的一小块原始区域显示到最终的完整表示）和toBundle 生成Bundle
    新启动一个App，最终会调用Activity的startActivity，其内部会调用startActivityForResult(intent, -1, options)
    
    startActivityForResult，在其他协议中(如ACTION_MAIN或ACTION_VIEW)，您可能无法在期望的时候得到结果。
    例如，如果正在启动的activity使用singleTask启动模式，它将不会在您的任务中运行，因此您将立即收到一个cancel结果。
    作为一种特殊情况，如果在activity的初始onCreate(Bundle savedInstanceState)/onResume()期间
    使用requestCode >= 0调用startActivityForResult()，那么您的窗口将不会显示，直到从启动的activity返回结果。这是为了在重定向到另一个activity时避免可见的闪烁。
    所以在onCreate或onResume中使用startActivityForResult，且requestCode>=0时，直接跳到目标activity，中间不会有闪烁现象
    Activity.startActivityForResult：
        if (mParent == null) {
            options = transferSpringboardActivityOptions(options);      //options为null时可能会有效果，设置动画
            Instrumentation.ActivityResult ar = mInstrumentation.execStartActivity(
                    this, mMainThread.getApplicationThread(), mToken, this, intent, requestCode, options);
            if (ar != null) {
                mMainThread.sendActivityResult(mToken, mEmbeddedID, requestCode, ar.getResultCode(),ar.getResultData());
            }
            if (requestCode >= 0) {
                //如果这个启动请求一个结果，在接收到结果之前，我们可以避免使activity可见。
                //在onCreate或onResume()期间设置此code将在此期间隐藏activity，以避免闪烁。
                //这只能在请求结果时进行，因为这保证了我们将在activity finish后获取信息，无论它发生了什么。
                mStartedActivity = true;
            }
            cancelInputsAndStartExitTransition(options);        //隐藏软键盘并启动退出动画？（取消挂起的输入，如果要运行activity转换，则启动转换。）
            // TODO 考虑清除/刷新子窗口的其他事件源和事件。
        } else {
            if (options != null) {
                mParent.startActivityFromChild(this, intent, requestCode, options);
            } else {
                //注意，我们希望通过这个方法与可能覆盖它的现有应用程序兼容。
                mParent.startActivityFromChild(this, intent, requestCode);
            }
        }
    startActivityForResult中，若mParent为null，则执行mInstrumentation.execStartActivity，否则执行mParent.startActivityFromChild
    Launcher Activity的mParent为null？
    execStartActivity为执行startActivity调用
    Instrumentation.execStartActivity(Context who, IBinder contextThread, IBinder token, Activity target, Intent intent, int requestCode, Bundle options)：
        IApplicationThread whoThread = (IApplicationThread) contextThread;
        Uri referrer = target != null ? target.onProvideReferrer() : null;      //为null？
        if (referrer != null) {
            intent.putExtra(Intent.EXTRA_REFERRER, referrer);
        }
        if (mActivityMonitors != null) {        //activity监视器
            synchronized (mSync) {
                final int N = mActivityMonitors.size();
                for (int i=0; i<N; i++) {
                    final ActivityMonitor am = mActivityMonitors.get(i);
                    ActivityResult result = null;
                    if (am.ignoreMatchingSpecificIntents()) {
                        result = am.onStartActivity(intent);
                    }
                    if (result != null) {
                        am.mHits++;
                        return result;
                    } else if (am.match(who, null, intent)) {
                        am.mHits++;
                        if (am.isBlocking()) {
                            return requestCode >= 0 ? am.getResult() : null;
                        }
                        break;
                    }
                }
            }
        }
        intent.migrateExtraStreamToClipData();  //将ACTION_SEND和ACTION_SEND_MULTIPLE中的任何EXTRA_STREAM迁移到ClipData。还要检查ACTION_CHOOSER中的嵌套意图。
        intent.prepareToLeaveProcess(who);      //准备离开应用程序进程
        int result = ActivityManager.getService().startActivity(whoThread, who.getBasePackageName(), intent,
                        intent.resolveTypeIfNeeded(who.getContentResolver()), token, target != null ? target.mEmbeddedID : null, requestCode, 0, null, options);
        checkStartActivityResult(result, intent);
        return null;
    主要是调用ActivityManager.getService().startActivity
    
    ActivityManager.getService()返回的是IActivityManager接口类
    IActivityManager为AIDL接口类，IActivityManager.Stub为实现IActivityManager接口的抽象类，由服务端继承实现服务端功能
    ActivityManagerService为IActivityManager.Stub的实现类，其实现服务端功能，由客户端代理调用
    ActivityManager.getService()，返回的是客户端代理：IActivityManager.Stub.asInterface(b)
    
#### 转入SystemServer进程执行    
    上面通过AIDL方式，将方法调用转入AIDL的服务端执行，而ActivityManagerService由SystemServer进程启动，AMS服务端位于SystemServer，所以后续操作都是位于SystemServer进程？

    ActivityManagerService.startActivity内部调用了startActivityAsUser
    ActivityManagerService.startActivityAsUser：
        enforceNotIsolatedCaller("startActivity");      //确保不是由一个隔离的 user 调用
        userId = mActivityStartController.checkTargetUser(userId, validateIncomingUser,
        Binder.getCallingPid(), Binder.getCallingUid(), "startActivityAsUser"); //推导实际的调用者UID
        //TODO: 在这里切换到用户应用程序堆栈
        return mActivityStartController.obtainStarter(intent, "startActivityAsUser")    //返回一个ActivityStarter
            .setCaller(caller)
            .setCallingPackage(callingPackage)
            .setResolvedType(resolvedType)
            .setResultTo(resultTo)
            .setResultWho(resultWho)
            .setRequestCode(requestCode)
            .setStartFlags(startFlags)
            .setProfilerInfo(profilerInfo)
            .setActivityOptions(bOptions)
            .setMayWait(userId)
            .execute();
    ActivityStartController，委托activity启动的控制器，该类的主要目标是获取外部activity的启动请求，
    并将它们准备成一系列可由ActivityStarter处理的离散activity 启动。它还负责处理围绕activity启动而发生的逻辑，
    但并不一定影响activity的启动。示例包括power hint管理、处理挂起的activity列表以及记录home activity的启动。
    
    ActivityStarter，用于配置和执行启动activity的启动程序。此starter在execute执行前有效。在执行后，starter应该被认为是无效的，并且不再被修改或使用
    ActivityStarter，控制器，用于解释如何启动一个activity。这个类收集所有的逻辑，以确定intent和flag应该如何转换为activity和相关的任务和堆栈。
    ActivityStarter.execute：（根据前面提供的request参数启动activity）
        try {
            // TODO: 考虑将请求直接传递给这些方法，以允许事务差异和预处理。
            if (mRequest.mayWait) {     //为true，在上面setMayWait方法中设置
                return startActivityMayWait;
            } else {
                return startActivity;
            }
        } finally {
            onExecutionComplete();      //执行完成时调用。设置指示完成的状态，并在适当时进行回收。
        }
    ActivityStarter.startActivityMayWait：
        //拒绝可能泄露的文件描述符
        if (intent != null && intent.hasFileDescriptors()) {
            throw new IllegalArgumentException("File descriptors passed in Intent");
        }
        mSupervisor.getActivityMetricsLogger().notifyActivityLaunching();   //当我们开始启动一个activity时，在可能的最早点通知跟踪器
        boolean componentSpecified = intent.getComponent() != null;
        final int realCallingPid = Binder.getCallingPid();
        final int realCallingUid = Binder.getCallingUid();
        int callingPid;
        if (callingUid >= 0) {      //上面未传值，默认为0，即 callingPid=-1，callingUid=0
            callingPid = -1;
        } else if (caller == null) {
            callingPid = realCallingPid;
            callingUid = realCallingUid;
        } else {
            callingPid = callingUid = -1;
        }
        //保存一个副本，以防临时需要它
        final Intent ephemeralIntent = new Intent(intent);
        // 不要修改客户端的对象!
        intent = new Intent(intent);
        if (componentSpecified && !(Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() == null)
            && !Intent.ACTION_INSTALL_INSTANT_APP_PACKAGE.equals(intent.getAction())
            && !Intent.ACTION_RESOLVE_INSTANT_APP_PACKAGE.equals(intent.getAction())
            && mService.getPackageManagerInternalLocked().isInstantAppInstallerComponent(intent.getComponent())) {
            // 直接拦截临时安装程序的intent，临时安装程序不应该以raw intent启动；相反，调整intent，使它看起来像一个“正常的”即时应用启动
            intent.setComponent(null /*component*/);
            componentSpecified = false;
        }       //拦截临时安装App的Intent
        ResolveInfo rInfo = mSupervisor.resolveIntent(intent, resolvedType, userId, 0,
                          computeResolveFilterUid(callingUid, realCallingUid, mRequest.filterCallingUid));  //内部实际调用 mService.getPackageManagerInternalLocked().resolveIntent
        ...
        //收集有关目标intent的信息。
        ActivityInfo aInfo = mSupervisor.resolveActivity(intent, rInfo, startFlags, profilerInfo);  //拿到rInfo.activityInfo，为其intent设置packageName等，为AMS设置debug信息等
        synchronized (mService) {
            final ActivityStack stack = mSupervisor.mFocusedStack;  //当前接收输入或启动下一个activity的堆栈
            stack.mConfigWillChange = globalConfig != null && mService.getGlobalConfiguration().diff(globalConfig) != 0;//globalConfig在上面未设置，默认为null，该项应该为false
            final long origId = Binder.clearCallingIdentity();
            if (aInfo != null && (aInfo.applicationInfo.privateFlags
                   & ApplicationInfo.PRIVATE_FLAG_CANT_SAVE_STATE) != 0 && mService.mHasHeavyWeightFeature) {//mHasHeavyWeightFeature与android.R.attr#cantSaveState有关
                //这可能是一个heavy-weight进程!看看是否已经运行另一个不同的heavy-weight进程
                if (aInfo.processName.equals(aInfo.applicationInfo.packageName)) {
                    final ProcessRecord heavy = mService.mHeavyWeightProcess;
                    if (heavy != null && (heavy.info.uid != aInfo.applicationInfo.uid || !heavy.processName.equals(aInfo.processName))) {   //多进程？下面可能不会执行
                        int appCallingUid = callingUid;
                        if (caller != null) {
                            ProcessRecord callerApp = mService.getRecordForAppLocked(caller);
                            if (callerApp != null) {
                                appCallingUid = callerApp.info.uid;
                            } else {
                                SafeActivityOptions.abort(options);
                                return ActivityManager.START_PERMISSION_DENIED;
                            }
                        }
                        IIntentSender target = mService.getIntentSenderLocked(
                                        ActivityManager.INTENT_SENDER_ACTIVITY, "android",
                                        appCallingUid, userId, null, null, 0, new Intent[] { intent },
                                        new String[] { resolvedType }, PendingIntent.FLAG_CANCEL_CURRENT
                                                | PendingIntent.FLAG_ONE_SHOT, null);
                        Intent newIntent = new Intent();
                        if (requestCode >= 0) {
                            // Caller is requesting a result.
                            newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_HAS_RESULT, true);
                        }
                        newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_INTENT, new IntentSender(target));
                        if (heavy.activities.size() > 0) {
                            ActivityRecord hist = heavy.activities.get(0);
                            newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_CUR_APP, hist.packageName);
                            newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_CUR_TASK, hist.getTask().taskId);
                        }
                        newIntent.putExtra(HeavyWeightSwitcherActivity.KEY_NEW_APP, aInfo.packageName);
                        newIntent.setFlags(intent.getFlags());
                        newIntent.setClassName("android", HeavyWeightSwitcherActivity.class.getName());
                        intent = newIntent;
                        resolvedType = null;
                        caller = null;
                        callingUid = Binder.getCallingUid();
                        callingPid = Binder.getCallingPid();
                        componentSpecified = true;
                        rInfo = mSupervisor.resolveIntent(intent, null /*resolvedType*/, userId,
                                        0 /* matchFlags */, computeResolveFilterUid(
                                                callingUid, realCallingUid, mRequest.filterCallingUid));
                        aInfo = rInfo != null ? rInfo.activityInfo : null;
                        if (aInfo != null) {
                            aInfo = mService.getActivityInfoForUser(aInfo, userId);
                        }
                    }
                }
            }   //上面一段与可能与多进程有关
            
            final ActivityRecord[] outRecord = new ActivityRecord[1];
            int res = startActivity(caller, intent, ephemeralIntent, resolvedType, aInfo, rInfo,
                            voiceSession, voiceInteractor, resultTo, resultWho, requestCode, callingPid,
                            callingUid, callingPackage, realCallingPid, realCallingUid, startFlags, options,
                            ignoreTargetSecurity, componentSpecified, outRecord, inTask, reason,
                            allowPendingRemoteAnimationRegistryLookup);
            Binder.restoreCallingIdentity(origId);
            if (stack.mConfigWillChange) {      //该值可能为false，切换配置
                //如果调用者也想切换到一个新的配置，现在这样做。这允许一个干净的开关，因为我们正在等待当前活动暂停（所以我们不会摧毁它），还没有开始下一个活动。
                mService.enforceCallingPermission(android.Manifest.permission.CHANGE_CONFIGURATION, "updateConfiguration()");
                stack.mConfigWillChange = false;
                //Updating to new configuration after starting activity
                mService.updateConfigurationLocked(globalConfig, null, false);
            }
            if (outResult != null) {
                outResult.result = res;
                final ActivityRecord r = outRecord[0];
                switch(res) {
                    case START_SUCCESS: {
                        mSupervisor.mWaitingActivityLaunched.add(outResult);
                        do {
                            mService.wait();
                        } while (outResult.result != START_TASK_TO_FRONT && !outResult.timeout && outResult.who == null);
                        if (outResult.result == START_TASK_TO_FRONT) {
                            res = START_TASK_TO_FRONT;
                        }
                        break;
                    }
                    case START_DELIVERED_TO_TOP: {
                        outResult.timeout = false;
                        outResult.who = r.realActivity;
                        outResult.totalTime = 0;
                        outResult.thisTime = 0;
                        break;
                    }
                    case START_TASK_TO_FRONT: {
                        // ActivityRecord可以代表一个不同的activity，但是它不应该处于resumed状态。
                        if (r.nowVisible && r.isState(RESUMED)) {
                            outResult.timeout = false;
                            outResult.who = r.realActivity;
                            outResult.totalTime = 0;
                            outResult.thisTime = 0;
                        } else {
                            outResult.thisTime = SystemClock.uptimeMillis();
                            mSupervisor.waitActivityVisible(r.realActivity, outResult);
                            do {
                                mService.wait();
                            } while (!outResult.timeout && outResult.who == null);
                        }
                        break;
                    }
                }
            }
            mSupervisor.getActivityMetricsLogger().notifyActivityLaunched(res, outRecord[0]);   //activity启动完毕
            return res;
        }
    startActivityMayWait主要在启动activity前解析intent，解析activity，处理多进程？然后调用 startActivity启动activity，
    启动成功后，看是否需要更新配置（mService.updateConfigurationLocked），最后根据启动的返回结果对outResult做相应处理
    
    ActivityStarter.startActivity（L571）：
        int err = ActivityManager.START_SUCCESS;
        //尽早将可选的临时安装包从选项中取出
        final Bundle verificationBundle = options != null ? options.popAppVerificationBundle() : null;  //options为从AMS的startActivity中传入的bundle
        ProcessRecord callerApp = null; //有关当前正在运行的特定进程的完整信息
        if (caller != null) {
            callerApp = mService.getRecordForAppLocked(caller); //从LRU list中查找和caller有关的进程信息
            if (callerApp != null) {
                callingPid = callerApp.pid;
                callingUid = callerApp.info.uid;
            } else {
                //Unable to find app for caller (pid=" + callingPid + ") when starting: 
                err = ActivityManager.START_PERMISSION_DENIED;
            }
        }
        final int userId = aInfo != null && aInfo.applicationInfo != null ? UserHandle.getUserId(aInfo.applicationInfo.uid) : 0;  //获取userId
        if (err == ActivityManager.START_SUCCESS) {
            Slog.i(TAG, "START u" + userId + " {" + intent.toShortString(true, true, true, false) + "} from uid " + callingUid);
            //I/ActivityManager: START u0 {act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x30200000 cmp=com.chengcheng.zhuanche.driver/.activitys.StartActivity bnds=[533,486][697,651] (has extras)} from uid 10032
        }
        ActivityRecord sourceRecord = null;
        ActivityRecord resultRecord = null;
        if (resultTo != null) {
            sourceRecord = mSupervisor.isInAnyStackLocked(resultTo);    //从mActivityDisplays中取出指定token的ActivityRecord
            //Will send result to " + resultTo + " " + sourceRecord
            if (sourceRecord != null) {
                if (requestCode >= 0 && !sourceRecord.finishing) {
                    resultRecord = sourceRecord;
                }
            }
        }
        final int launchFlags = intent.getFlags();
        if ((launchFlags & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0 && sourceRecord != null) { //sourceRecord可能为null？
            //将结果目标从源activity转移到正在启动的新activity，包括任何失败
            if (requestCode >= 0) {     // <0
                SafeActivityOptions.abort(options);
                return ActivityManager.START_FORWARD_AND_REQUEST_CONFLICT;
            }
            resultRecord = sourceRecord.resultTo;
            if (resultRecord != null && !resultRecord.isInStackLocked()) {
                resultRecord = null;
            }
            resultWho = sourceRecord.resultWho;
            requestCode = sourceRecord.requestCode;
            sourceRecord.resultTo = null;
            if (resultRecord != null) {
                resultRecord.removeResultsLocked(sourceRecord, resultWho, requestCode);
            }
            if (sourceRecord.launchedFromUid == callingUid) {
                //新activity从与流中的前一个activity相同的uid启动，并要求将其结果转发回先前的。
                //在这种情况下，activity充当了两者之间的蹦床，因此，我们还希望将其launchedFromPackage更新为与前一个活动相同。
                //注意这是安全的，因为我们知道这两个包来自同一个uid；调用者也可以提供相同的包名本身。
                //这特别处理在应用程序中启动一个intent picker/chooser重定向到用户选择的activity的流的情况，我们希望最后一个activity认为它已经由之前的app activity启动。
                callingPackage = sourceRecord.launchedFromPackage;
            }
        }
        if (err == ActivityManager.START_SUCCESS && intent.getComponent() == null) {
            // 我们找不到一个能处理给定intent的类。到此为止吧!
            err = ActivityManager.START_INTENT_NOT_RESOLVED;
        }
        if (err == ActivityManager.START_SUCCESS && aInfo == null) {
            // 我们找不到intent中指定的特定类。也是到此结束吧。
            err = ActivityManager.START_CLASS_NOT_FOUND;
        }
        //语音相关
        if (err == ActivityManager.START_SUCCESS && sourceRecord != null && sourceRecord.getTask().voiceSession != null) {
            //如果此activity是作为语音会话的一部分启动的，我们需要确保这样做是安全的。如果接下来的activity也是语音部分，只有当它明确表示支持语音类别时，我们才能启动它，或者它是呼叫应用程序的一部分。
            if ((launchFlags & FLAG_ACTIVITY_NEW_TASK) == 0 && sourceRecord.info.applicationInfo.uid != aInfo.applicationInfo.uid) {
                intent.addCategory(Intent.CATEGORY_VOICE);
                if (!mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent, resolvedType)) {
                    //Activity being started in current voice task does not support voice: 
                    err = ActivityManager.START_NOT_VOICE_COMPATIBLE;
                }
            }
        }
        if (err == ActivityManager.START_SUCCESS && voiceSession != null) {
            //如果呼叫者正在开始一个新的语音会话，只要确保目标实际上允许它以这种方式运行。
            if (!mService.getPackageManager().activitySupportsIntent(intent.getComponent(), intent, resolvedType)) {
                //Activity being started in new voice task does not support: 
                err = ActivityManager.START_NOT_VOICE_COMPATIBLE;
            }
        }
        final ActivityStack resultStack = resultRecord == null ? null : resultRecord.getStack();
        if (err != START_SUCCESS) {     //到目前为止 err==START_SUCCESS
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, RESULT_CANCELED, null);
            }
            SafeActivityOptions.abort(options);
            return err;
        }
        //能否继续下去的检查，是否要不中断
        boolean abort = !mSupervisor.checkStartAnyActivityPermission(intent, aInfo, resultWho,
                        requestCode, callingPid, callingUid, callingPackage, ignoreTargetSecurity,
                        inTask != null, callerApp, resultRecord, resultStack);
        abort |= !mService.mIntentFirewall.checkStartActivity(intent, callingUid, callingPid, resolvedType, aInfo.applicationInfo);
        //合并两个选项包，而realCallerOptions优先。
        ActivityOptions checkedOptions = options != null ? options.getOptions(intent, aInfo, callerApp, mSupervisor) : null;
        if (allowPendingRemoteAnimationRegistryLookup) {
            checkedOptions = mService.getActivityStartController().getPendingRemoteAnimationRegistry().overrideOptionsIfNeeded(callingPackage, checkedOptions);
        }
        if (mService.mController != null) {
            try {
                //我们提供给观察者的intent去掉了额外的数据，因为它可以包含私有信息。
                Intent watchIntent = intent.cloneFilter();
                abort |= !mService.mController.activityStarting(watchIntent, aInfo.applicationInfo.packageName);
            } catch (RemoteException e) {
                mService.mController = null;
            }
        }
        mInterceptor.setStates(userId, realCallingPid, realCallingUid, startFlags, callingPackage);
        if (mInterceptor.intercept(intent, rInfo, aInfo, resolvedType, inTask, callingPid, callingUid, checkedOptions)) {
            // 活动开始被拦截，例如，因为目标用户当前处于安静模式(关闭工作)或目标应用程序被挂起
            intent = mInterceptor.mIntent;
            rInfo = mInterceptor.mRInfo;
            aInfo = mInterceptor.mAInfo;
            resolvedType = mInterceptor.mResolvedType;
            inTask = mInterceptor.mInTask;
            callingPid = mInterceptor.mCallingPid;
            callingUid = mInterceptor.mCallingUid;
            checkedOptions = mInterceptor.mActivityOptions;
        }
        if (abort) {
            if (resultRecord != null) {
                resultStack.sendActivityResultLocked(-1, resultRecord, resultWho, requestCode, RESULT_CANCELED, null);
            }
            // 我们假装调用者确实启动了，但是他们只会得到一个取消结果。
            ActivityOptions.abort(checkedOptions);
            return START_ABORTED;
        }
        //如果权限需要审查后才能运行任何应用程序组件，我们启动审查activity，并在审查完成后传递启动activity的pending intent
        if (mService.mPermissionReviewRequired && aInfo != null) {
            if (mService.getPackageManagerInternalLocked().isPermissionsReviewRequired(aInfo.packageName, userId)) {
                IIntentSender target = mService.getIntentSenderLocked(ActivityManager.INTENT_SENDER_ACTIVITY, callingPackage,
                                callingUid, userId, null, null, 0, new Intent[]{intent},
                                new String[]{resolvedType}, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT, null);
                final int flags = intent.getFlags();
                Intent newIntent = new Intent(Intent.ACTION_REVIEW_PERMISSIONS);
                newIntent.setFlags(flags | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                newIntent.putExtra(Intent.EXTRA_PACKAGE_NAME, aInfo.packageName);
                newIntent.putExtra(Intent.EXTRA_INTENT, new IntentSender(target));
                if (resultRecord != null) {
                    newIntent.putExtra(Intent.EXTRA_RESULT_NEEDED, true);
                }
                intent = newIntent;
                resolvedType = null;
                callingUid = realCallingUid;
                callingPid = realCallingPid;
                rInfo = mSupervisor.resolveIntent(intent, resolvedType, userId, 0,
                                computeResolveFilterUid(callingUid, realCallingUid, mRequest.filterCallingUid));
                aInfo = mSupervisor.resolveActivity(intent, rInfo, startFlags, null /*profilerInfo*/);
                if (DEBUG_PERMISSIONS_REVIEW) {
                    //START u" + userId + " {" + intent.toShortString(true, true, true, false) + "} from uid " + callingUid + " on display "
                                    + (mSupervisor.mFocusedStack == null ? DEFAULT_DISPLAY : mSupervisor.mFocusedStack.mDisplayId));
                }
            }
        }
        // 如果我们有一个临时应用程序，中止启动已解析intent的进程。相反，启动临时安装程序。一旦安装程序完成，它要么启动我们在这里解析的intent(安装错误)，要么启动临时应用程序(安装成功)。
        if (rInfo != null && rInfo.auxiliaryInfo != null) {
            intent = createLaunchIntent(rInfo.auxiliaryInfo, ephemeralIntent, callingPackage, verificationBundle, resolvedType, userId);
            resolvedType = null;
            callingUid = realCallingUid;
            callingPid = realCallingPid;
            aInfo = mSupervisor.resolveActivity(intent, rInfo, startFlags, null /*profilerInfo*/);
        }
        ActivityRecord r = new ActivityRecord(mService, callerApp, callingPid, callingUid,
                 callingPackage, intent, resolvedType, aInfo, mService.getGlobalConfiguration(),
                 resultRecord, resultWho, requestCode, componentSpecified, voiceSession != null, mSupervisor, checkedOptions, sourceRecord);
        if (outActivity != null) {
            outActivity[0] = r;
        }
        if (r.appTimeTracker == null && sourceRecord != null) {
            //如果调用者没有指定一个显式的时间跟踪器，我们希望在它拥有的任何时间下继续跟踪。
            r.appTimeTracker = sourceRecord.appTimeTracker;
        }
        final ActivityStack stack = mSupervisor.mFocusedStack;  //当前接收输入或启动下一个activity的堆栈
        //如果我们正在启动的activity与当前resumed的activity的uid不同，检查是否允许应用程序切换。
        if (voiceSession == null && (stack.getResumedActivity() == null || stack.getResumedActivity().info.applicationInfo.uid != realCallingUid)) {
            //检查是否允许切换，返回false，不允许
            if (!mService.checkAppSwitchAllowedLocked(callingPid, callingUid, realCallingPid, realCallingUid, "Activity start")) {
                mController.addPendingActivityLaunch(new PendingActivityLaunch(r, sourceRecord, startFlags, stack, callerApp));
                ActivityOptions.abort(checkedOptions);
                return ActivityManager.START_SWITCHES_CANCELED;
            }
        }
        if (mService.mDidAppSwitch) {
            //这是自我们停止开关以来第二个允许的开关，现在允许切换。用例:用户按home(switch禁用，切换到home, mDidAppSwitch现在为true)；用户点击一个home图标(来自home，所以允许，我们点击这里，现在允许任何人再次切换)。
            mService.mAppSwitchesAllowedTime = 0;
        } else {
            mService.mDidAppSwitch = true;
        }
        mController.doPendingActivityLaunches(false);   //循环启动mPendingActivityLaunches中的pending activity，内部调用ActivityStarter.startResolvedActivity，最后也是调用下面的startActivity
        return startActivity(r, sourceRecord, voiceSession, voiceInteractor, startFlags, true /* doResume */, checkedOptions, inTask, outActivity);
    做了一系列的intent检查，权限检查等，最后调用另一个 startActivity
    ActivityStarter.startActivity（L1193）：
        int result = START_CANCELED;
        try {
            mService.mWindowManager.deferSurfaceLayout();   //开始延迟布局传递。在执行多个更改时非常有用，但是为了优化性能，应该只执行一次布局传递。这可以被多次调用，一旦最后一个调用者调用了continueSurfaceLayout，就会恢复布图
            result = startActivityUnchecked(r, sourceRecord, voiceSession, voiceInteractor, startFlags, doResume, options, inTask, outActivity);
        } finally {
            // 如果我们无法继续，请将activity与task分离。让一个activity处于不完整的状态可能会导致问题，例如在没有窗口容器的情况下执行操作
            final ActivityStack stack = mStartActivity.getStack();
            if (!ActivityManager.isStartResultSuccessful(result) && stack != null) {    //未启动成功
                stack.finishActivityLocked(mStartActivity, RESULT_CANCELED, null /* intentResultData */, "startActivity", true /* oomAdj */);
            }
            mService.mWindowManager.continueSurfaceLayout();    //递延后恢复布局。
        }
        postStartActivityProcessing(r, result, mTargetStack);   //通知结果给等待activity启动完成的一些东西
        return result;
    ActivityStarter.startActivityUnchecked：
        setInitialState(r, options, inTask, doResume, startFlags, sourceRecord, voiceSession, voiceInteractor); //设置一些全局变量
        computeLaunchingTaskFlags();    //和Launch Mode有关
        computeSourceStack();       //和Stack有关，源Activity是否finish等？
        mIntent.setFlags(mLaunchFlags);     //mLaunchFlags在setInitialState 中有设置
        ActivityRecord reusedActivity = getReusableIntentActivity();    //决定是否应该将新activity插入到现有的task中。如果没有，则返回null，或者返回一个ActivityRecord，其中包含应该添加新activity的task
        int preferredWindowingMode = WINDOWING_MODE_UNDEFINED;
        int preferredLaunchDisplayId = DEFAULT_DISPLAY;
        if (mOptions != null) {
            preferredWindowingMode = mOptions.getLaunchWindowingMode();
            preferredLaunchDisplayId = mOptions.getLaunchDisplayId();
        }
        // 窗口模式和来自LaunchParams的首选启动显示值优先于ActivityOptions中指定的值
        if (!mLaunchParams.isEmpty()) {
            if (mLaunchParams.hasPreferredDisplay()) {
                preferredLaunchDisplayId = mLaunchParams.mPreferredDisplayId;
            }
            if (mLaunchParams.hasWindowingMode()) {
                preferredWindowingMode = mLaunchParams.mWindowingMode;
            }
        }
        if (reusedActivity != null) {       //新启动的App可能为null
            //当设置了NEW_TASK和CLEAR_TASK标志后，task将被重用但仍然需要一个锁任务模式冲突，因为任务被清除否则设备将离开锁定的任务。
            if (mService.getLockTaskController().isLockTaskModeViolation(reusedActivity.getTask(),
                    (mLaunchFlags & (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK)) == (FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK))) {
                //startActivityUnchecked: Attempt to violate Lock Task Mode
                return START_RETURN_LOCK_TASK_MODE_VIOLATION;
            }
            // 如果我们清除顶部并重置标准(默认)启动模式activity，则为true。现有的活动将结束。
            final boolean clearTopAndResetStandardLaunchMode =
                    (mLaunchFlags & (FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
                                    == (FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_RESET_TASK_IF_NEEDED) && mLaunchMode == LAUNCH_MULTIPLE;
            //如果mStartActivity没有相关的任务，将其与重用activity的任务关联起来。如果我们清除顶部和重置一个标准的启动模式活动，不要这样做。
            if (mStartActivity.getTask() == null && !clearTopAndResetStandardLaunchMode) {
                mStartActivity.setTask(reusedActivity.getTask());
            }
            if (reusedActivity.getTask().intent == null) {
                //此任务是由于基于亲缘关系的activity的移动而启动的……现在我们正在启动它，我们可以分配base intent。
                reusedActivity.getTask().setIntent(mStartActivity);
            }
            //这条代码路径会产生一个新的intent，我们要确保把它作为第一次操作，在以后的操作中，activity将被恢复。
            if ((mLaunchFlags & FLAG_ACTIVITY_CLEAR_TOP) != 0 || isDocumentLaunchesIntoExisting(mLaunchFlags)
                            || isLaunchModeOneOf(LAUNCH_SINGLE_INSTANCE, LAUNCH_SINGLE_TASK)) {
                final TaskRecord task = reusedActivity.getTask();
                // 在这种情况下，我们希望从任务中删除所有activity，直到正在启动的activity为止。在大多数情况下，这意味着我们将task重置为初始状态。
                final ActivityRecord top = task.performClearTaskForReuseLocked(mStartActivity, mLaunchFlags);
                //上面的代码可以从任务中删除reusedActivity，导致ActivityRecord删除它对TaskRecord的引用。下面调用setTargetStackAndMoveToFrontIfNeeded需要task引用。
                if (reusedActivity.getTask() == null) {
                    reusedActivity.setTask(task);
                }
                if (top != null) {
                    if (top.frontOfTask) {
                        // Activity别名可能意味着我们对顶部activity使用不同的intent，因此，确保任务现在具有新intent的标识。
                        top.getTask().setIntent(mStartActivity);
                    }
                    deliverNewIntent(top);
                }
            }
            mSupervisor.sendPowerHintForLaunchStartIfNeeded(false /* forceSend */, reusedActivity); //和PowerManager有关
            reusedActivity = setTargetStackAndMoveToFrontIfNeeded(reusedActivity);  //当我们在历史中发现一个现有的匹配的activity记录时，找出应该将哪个任务和activity放在前面。如果需要，也可以清除任务。
            final ActivityRecord outResult = outActivity != null && outActivity.length > 0 ? outActivity[0] : null;
            //如果有一个被重用的activity，而当前结果是蹦床activity，将重用activity设置为结果。
            if (outResult != null && (outResult.finishing || outResult.noDisplay)) {
                outActivity[0] = reusedActivity;
            }
            if ((mStartFlags & START_FLAG_ONLY_IF_NEEDED) != 0) {
                //我们不需要开始一项新的activity，客户说如果是这样就什么都不要做，所以就是这样！对于偏执狂，确保我们正确地恢复了顶层activity。
                resumeTargetStackIfNeeded();
                return START_RETURN_INTENT_TO_CALLER;
            }
            if (reusedActivity != null) {
                setTaskFromIntentActivity(reusedActivity);
                if (!mAddingToTask && mReuseTask == null) {
                    //我们什么都没做……但这是必须的。，（client不要使用该意图）对于偏执狂，确保我们正确地恢复了顶层activity。
                    resumeTargetStackIfNeeded();
                    if (outActivity != null && outActivity.length > 0) {
                        outActivity[0] = reusedActivity;
                    }
                    return mMovedToFront ? START_TASK_TO_FRONT : START_DELIVERED_TO_TOP;
                }
            }
        }
        if (mStartActivity.packageName == null) {
            final ActivityStack sourceStack = mStartActivity.resultTo != null ? mStartActivity.resultTo.getStack() : null;
            if (sourceStack != null) {
                sourceStack.sendActivityResultLocked(-1 /* callingUid */, mStartActivity.resultTo, mStartActivity.resultWho, mStartActivity.requestCode, RESULT_CANCELED, null /* data */);
            }
            ActivityOptions.abort(mOptions);
            return START_CLASS_NOT_FOUND;
        }
        //如果正在启动的activity与当前位于顶部的activity相同，那么我们需要检查它是否应该只启动一次。
        final ActivityStack topStack = mSupervisor.mFocusedStack;
        final ActivityRecord topFocused = topStack.getTopActivity();
        final ActivityRecord top = topStack.topRunningNonDelayedActivityLocked(mNotTop);
        final boolean dontStart = top != null && mStartActivity.resultTo == null
                && top.realActivity.equals(mStartActivity.realActivity) && top.userId == mStartActivity.userId
                && top.app != null && top.app.thread != null && ((mLaunchFlags & FLAG_ACTIVITY_SINGLE_TOP) != 0
                || isLaunchModeOneOf(LAUNCH_SINGLE_TOP, LAUNCH_SINGLE_TASK));
        if (dontStart) {
            // For paranoia, 确保我们已经正确地恢复了top activity。
            topStack.mLastPausedActivity = null;
            if (mDoResume) {
                mSupervisor.resumeFocusedStackTopActivityLocked();
            }
            ActivityOptions.abort(mOptions);
            if ((mStartFlags & START_FLAG_ONLY_IF_NEEDED) != 0) {
                // 我们不需要启动新的activity，客户说如果是这样就什么都不要做，就是这样!
                return START_RETURN_INTENT_TO_CALLER;
            }
            deliverNewIntent(top);
            //不要使用mStartActivity.task 展示吐司。我们不是开始一个新的activity，而是重复使用“top”。mStartActivity中的字段可能没有完全初始化。
            mSupervisor.handleNonResizableTaskIfNeeded(top.getTask(), preferredWindowingMode, preferredLaunchDisplayId, topStack);
            return START_DELIVERED_TO_TOP;
        }
        boolean newTask = false;
        final TaskRecord taskToAffiliate = (mLaunchTaskBehind && mSourceRecord != null) ? mSourceRecord.getTask() : null;
        // 这应该被视为一项新任务吗?
        int result = START_SUCCESS;
        if (mStartActivity.resultTo == null && mInTask == null && !mAddingToTask
                && (mLaunchFlags & FLAG_ACTIVITY_NEW_TASK) != 0) {
            newTask = true;
            result = setTaskFromReuseOrCreateNewTask(taskToAffiliate, topStack);
        } else if (mSourceRecord != null) {
            result = setTaskFromSourceRecord();
        } else if (mInTask != null) {
            result = setTaskFromInTask();
        } else {
            //这不是从现有activity开始的，也不是新任务的一部分…… 只要把它放在top任务，尽管这种情况不应该发生。
            setTaskToCurrentTopOrCreateNewTask();
        }
        if (result != START_SUCCESS) {
            return result;
        }
        mService.grantUriPermissionFromIntentLocked(mCallingUid, mStartActivity.packageName, mIntent, mStartActivity.getUriPermissionsLocked(), mStartActivity.userId);
        mService.grantEphemeralAccessLocked(mStartActivity.userId, mIntent, mStartActivity.appInfo.uid, UserHandle.getAppId(mCallingUid));
        mTargetStack.mLastPausedActivity = null;
        mSupervisor.sendPowerHintForLaunchStartIfNeeded(false /* forceSend */, mStartActivity);
        mTargetStack.startActivityLocked(mStartActivity, topFocused, newTask, mKeepCurTransition, mOptions);
        if (mDoResume) {
            final ActivityRecord topTaskActivity = mStartActivity.getTask().topRunningActivityLocked();
            if (!mTargetStack.isFocusable() || (topTaskActivity != null && topTaskActivity.mTaskOverlay
                            && mStartActivity != topTaskActivity)) {
                //如果activity不可聚焦，我们就不能resume它，但仍然想要确保它成为可见的，因为它已经开始了(这也将触发进入动画)。这方面的一个例子是PIP activity。
                //另外，我们不希望在当前具有覆盖的任务中恢复活动，因为启动活动只需要处于可见暂停状态，直到结束被删除。
                mTargetStack.ensureActivitiesVisibleLocked(null, 0, !PRESERVE_WINDOWS);
                //继续，并告诉窗口管理器执行该activity的应用程序转换，因为应用程序转换将不会通过恢复通道触发。
                mService.mWindowManager.executeAppTransition();
            } else {
                //如果目标堆栈以前不可调焦(该堆栈上以前的top activity不可见)然后，之前任何将堆栈移动到的调用都不会更新有焦点的堆栈。如果现在启动的新activity允许任务堆栈可聚焦，那么确保我们现在相应地更新有焦点的堆栈。
                if (mTargetStack.isFocusable() && !mSupervisor.isFocusedStack(mTargetStack)) {
                    mTargetStack.moveToFront("startActivityUnchecked");
                }
                mSupervisor.resumeFocusedStackTopActivityLocked(mTargetStack, mStartActivity, mOptions);
            }
        } else if (mStartActivity != null) {
            mSupervisor.mRecentTasks.add(mStartActivity.getTask());
        }
        mSupervisor.updateUserStackLocked(mStartActivity.userId, mTargetStack);
        mSupervisor.handleNonResizableTaskIfNeeded(mStartActivity.getTask(), preferredWindowingMode,
                        preferredLaunchDisplayId, mTargetStack);
        return START_SUCCESS;
    startActivityUnchecked主要是赋值了一些全局变量，根据activity的启动模式做了一些操作，和activity task相关的一些操作，比如要启动的activity是否存在于历史task中等
    startActivityUnchecked中最主要的是mSupervisor.resumeFocusedStackTopActivityLocked()，将activity置于前台可见
    
    ActivityStackSupervisor.resumeFocusedStackTopActivityLocked(ActivityStack targetStack, ActivityRecord target, ActivityOptions targetOptions) {
        if (!readyToResume()) {
            return false;
        }
        if (targetStack != null && isFocusedStack(targetStack)) {
            return targetStack.resumeTopActivityUncheckedLocked(target, targetOptions);
        }
        final ActivityRecord r = mFocusedStack.topRunningActivityLocked();
        if (r == null || !r.isState(RESUMED)) {
            mFocusedStack.resumeTopActivityUncheckedLocked(null, null);
        } else if (r.isState(RESUMED)) {
            //启动MoveTaskToFront操作中的任何应用程序过渡
            mFocusedStack.executeAppTransition(targetOptions);
        }
        return false;
    }
    接着看 targetStack.resumeTopActivityUncheckedLocked，确保恢复堆栈中的顶部activity
    
    ActivityStack用于单个activity堆栈的状态和管理
    ActivityStack.resumeTopActivityUncheckedLocked(ActivityRecord prev, ActivityOptions options) {
        prev，先前处于resume的activity，现在处于暂停；从其他地方调用可以为空。
        if (mStackSupervisor.inResumeTopActivity) {
            //不要开始递归
            return false;
        }
        boolean result = false;
        try {
            //防止递归
            mStackSupervisor.inResumeTopActivity = true;
            result = resumeTopActivityInnerLocked(prev, options);
            //当恢复顶部activity时，可能需要暂停顶部activity（例如，返回到锁屏。我们在resumeTopActivityUncheckedLocked中抑制了正常的暂停逻辑，
            //因为顶部activity最后会resume。我们再次调用ActivityStackSupervisor.checkreadyforsleepplocked，以确保发生任何必要的暂停逻辑。
            //在这种情况下，activity将显示无论是否锁定屏幕，跳过对ActivityStackSupervisor.checkreadyforsleepplocked的调用。
            final ActivityRecord next = topRunningActivityLocked(true /* focusableOnly */); //堆栈顶层的activity
            if (next == null || !next.canTurnScreenOn()) {
                checkReadyForSleep();   //锁屏？
            }
        } finally {
            mStackSupervisor.inResumeTopActivity = false;
        }
        return result;
    }
    ActivityStack.resumeTopActivityInnerLocked(ActivityRecord prev, ActivityOptions options)：
        if (!mService.mBooting && !mService.mBooted) {
            // Not ready yet!
            return false;
        }
        //在此堆栈中查找下一个未完成但可聚焦的要继续的最顶部activity。如果它是不可聚焦的，我们将陷入下面的情况，在下一个可聚焦任务中恢复顶部activity。
        final ActivityRecord next = topRunningActivityLocked(true /* focusableOnly */);
        final boolean hasRunningActivity = next != null;
        // TODO: 也许这整个情况可以被移除?
        if (hasRunningActivity && !isAttached()) {
            return false;
        }
        mStackSupervisor.cancelInitializingActivities();    //取消除堆栈顶部以为的所有activity的初始化
        //请记住，我们将如何处理这种暂停/恢复的情况，并确保在我们结束时状态被重置。
        boolean userLeaving = mStackSupervisor.mUserLeaving;
        mStackSupervisor.mUserLeaving = false;
        if (!hasRunningActivity) {
            //堆栈中已经没有activity了，让我们看看其他地方。
            return resumeTopActivityInNextFocusableStack(prev, options, "noMoreActivities");
        }
        next.delayedResume = false;
        //如果最上面的activity是resumed，则什么也不做。
        if (mResumedActivity == next && next.isState(RESUMED) && mStackSupervisor.allResumedActivitiesComplete()) {
            //请确保我们已经执行了任何挂起的转换，因为此时应该没有什么要做的了。
            executeAppTransition(options);
            if (DEBUG_STATES) Slog.d(TAG_STATES, "resumeTopActivityLocked: Top activity resumed " + next);
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        //如果我们正在睡觉，没有resumed activity，而最上面的activity暂停了，那么这就是我们想要的状态。
        if (shouldSleepOrShutDownActivities() && mLastPausedActivity == next && mStackSupervisor.allPausedActivitiesComplete()) {
            // 请确保我们已经执行了任何挂起的转换，因为此时应该没有什么要做的了。
            executeAppTransition(options);
            if (DEBUG_STATES) Slog.d(TAG_STATES, "resumeTopActivityLocked: Going to sleep and all paused");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        //确保拥有此activity的用户已启动。如果没有，我们将保持原样，因为应该有人将另一个用户的activity放到堆栈的顶部。
        if (!mService.mUserController.hasStartedUserState(next.userId)) {
            Slog.w(TAG, "Skipping resume of top activity " + next + ": user " + next.userId + " is stopped");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        //该activity可能正在等待停止，但不再适合它。
        mStackSupervisor.mStoppingActivities.remove(next);
        mStackSupervisor.mGoingToSleepActivities.remove(next);
        next.sleeping = false;
        mStackSupervisor.mActivitiesWaitingForVisibleActivity.remove(next);
        //如果我们当前正在暂停某个activity，那么在暂停之前不要做任何事情。
        if (!mStackSupervisor.allPausedActivitiesComplete()) {
            if (DEBUG_SWITCH || DEBUG_PAUSE || DEBUG_STATES) Slog.v(TAG_PAUSE, "resumeTopActivityLocked: Skip resume: some activity pausing.");
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return false;
        }
        mStackSupervisor.setLaunchSource(next.info.applicationInfo.uid);    //把uid和PowerManager关联起来，和唤醒锁有关，用于统计等？
        boolean lastResumedCanPip = false;
        ActivityRecord lastResumed = null;
        final ActivityStack lastFocusedStack = mStackSupervisor.getLastStack();
        if (lastFocusedStack != null && lastFocusedStack != this) {
            // 那么，我们为什么不用prev呢? 查看方法的参数注释。prev不代表最后一次resumed activity。但是，如果最后一个焦点堆栈不是null，它就会执行。
            lastResumed = lastFocusedStack.mResumedActivity;
            if (userLeaving && inMultiWindowMode() && lastFocusedStack.shouldBeVisible(next)) {
                // 如果这个堆栈是多窗口模式，并且最后一个集中的堆栈仍然是可见的，那么用户不会离开。
                if(DEBUG_USER_LEAVING) Slog.i(TAG_USER_LEAVING, "Overriding userLeaving to false" + " next=" + next + " lastResumed=" + lastResumed);
                    userLeaving = false;
            }
            lastResumedCanPip = lastResumed != null && lastResumed.checkEnterPictureInPictureState("resumeTopActivity", userLeaving /* beforeStopping */);
        }
        //如果设置了resume_while pausing标志，然后继续调度之前要暂停的activity，
        //同时只有在上一个activity无法进入Pip时才恢复新的resume activity，因为我们希望在继续下一个activity之前让Pip活动有机会进入Pip。
        final boolean resumeWhilePausing = (next.info.flags & FLAG_RESUME_WHILE_PAUSING) != 0 && !lastResumedCanPip;
        boolean pausing = mStackSupervisor.pauseBackStacks(userLeaving, next, false);
        if (mResumedActivity != null) {
            if (DEBUG_STATES) Slog.d(TAG_STATES, "resumeTopActivityLocked: Pausing " + mResumedActivity);
            pausing |= startPausingLocked(userLeaving, false, next, false);
        }
        if (pausing && !resumeWhilePausing) {
            if (DEBUG_SWITCH || DEBUG_STATES) Slog.v(TAG_STATES, "resumeTopActivityLocked: Skip resume: need to start pausing");
            //在这一点上，我们希望将即将进行的activity的流程放在LRU列表的顶部，因为我们知道我们很快就会需要它，并且如果恰好处于最后状态，将其杀死将是一种浪费。
            if (next.app != null && next.app.thread != null) {
                mService.updateLruProcessLocked(next.app, true, null);
            }
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            if (lastResumed != null) {
                lastResumed.setWillCloseOrEnterPip(true);
            }
            return true;
        } else if (mResumedActivity == next && next.isState(RESUMED) && mStackSupervisor.allResumedActivitiesComplete()) {
            //如果下一个活动不必等待暂停完成，则当我们暂停以上堆栈时，可以恢复该活动。 因此，除了：除了确保没有执行任何其他操作之外，请确保已执行所有挂起的过渡，因为此时不应该进行任何操作。
            executeAppTransition(options);
            if (DEBUG_STATES) Slog.d(TAG_STATES, "resumeTopActivityLocked: Top activity resumed (dontWaitForPause) " + next);
            if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
            return true;
        }
        //如果最近的活动不是“历史记录”，而是由于设备进入睡眠状态而只是停止而不是停止+完成，则我们需要确保完成操作，因为我们将最重要的新活动置于首位。
        if (shouldSleepActivities() && mLastNoHistoryActivity != null && !mLastNoHistoryActivity.finishing) {
            if (DEBUG_STATES) Slog.d(TAG_STATES, "no-history finish of " + mLastNoHistoryActivity + " on new resume");
            requestFinishActivityLocked(mLastNoHistoryActivity.appToken, Activity.RESULT_CANCELED, null, "resume-no-history", false);
            mLastNoHistoryActivity = null;
        }
        if (prev != null && prev != next) {
            if (!mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(prev) && next != null && !next.nowVisible) {
                mStackSupervisor.mActivitiesWaitingForVisibleActivity.add(prev);
                if (DEBUG_SWITCH) Slog.v(TAG_SWITCH, "Resuming top, waiting visible to hide: " + prev);
            } else {
                //下一个活动已经可见，因此请立即隐藏上一个活动的窗口，以便我们尽快显示新的活动。 我们仅在前一个操作完成时才执行此操作，这应该意味着它在要恢复的操作之上，因此快速将其隐藏是一件好事。 否则，我们要执行允许显示已恢复的活动的正常方法，以便我们可以根据是否发现新的活动是全屏的，来决定是否应该隐藏前一个活动。
                if (prev.finishing) {
                    prev.setVisibility(false);
                    //Not waiting for visible to hide: " + prev + ", waitingVisible=" + mStackSupervisor.mActivitiesWaitingForVisibleActivity.contains(prev) + ", nowVisible=" + next.nowVisible);
                } else {
                    //Previous already visible but still waiting to hide: " + prev + ", waitingVisible=" , nowVisible=" + next.nowVisible);
                }
            }
        }
        // 启动此应用程序的activity，确保该应用程序不再被视为停止。
        try {
            AppGlobals.getPackageManager().setPackageStoppedState(next.packageName, false, next.userId); /* TODO: Verify if correct userid */
        } catch (RemoteException e1) {
        } catch (IllegalArgumentException e) {
            Slog.w(TAG, "Failed trying to unstop package " + next.packageName + ": " + e);
        }
        // 我们正在开始下一个活动，所以告诉窗口管理器上一个活动将很快被隐藏。 这样，它可以知道在计算所需的屏幕方向时忽略它。
        boolean anim = true;
        if (prev != null) {
            if (prev.finishing) {
                //Prepare close transition: prev=" + prev
                if (mStackSupervisor.mNoAnimActivities.contains(prev)) {
                    anim = false;
                    mWindowManager.prepareAppTransition(TRANSIT_NONE, false);
                } else {
                    mWindowManager.prepareAppTransition(prev.getTask() == next.getTask() ? TRANSIT_ACTIVITY_CLOSE : TRANSIT_TASK_CLOSE, false);
                }
                prev.setVisibility(false);
            } else {
                //Prepare open transition: prev=" + prev
                if (mStackSupervisor.mNoAnimActivities.contains(next)) {
                    anim = false;
                    mWindowManager.prepareAppTransition(TRANSIT_NONE, false);
                } else {
                    mWindowManager.prepareAppTransition(prev.getTask() == next.getTask() ? TRANSIT_ACTIVITY_OPEN
                                    : next.mLaunchTaskBehind ? TRANSIT_TASK_OPEN_BEHIND : TRANSIT_TASK_OPEN, false);
                }
            }
        } else {
            if (DEBUG_TRANSITION) Slog.v(TAG_TRANSITION, "Prepare open transition: no previous");
            if (mStackSupervisor.mNoAnimActivities.contains(next)) {
                anim = false;
                mWindowManager.prepareAppTransition(TRANSIT_NONE, false);
            } else {
                mWindowManager.prepareAppTransition(TRANSIT_ACTIVITY_OPEN, false);
            }
        }       //和activity切换动画有关
        if (anim) {
            next.applyOptionsLocked();
        } else {
            next.clearOptionsLocked();
        }
        mStackSupervisor.mNoAnimActivities.clear();
        ActivityStack lastStack = mStackSupervisor.getLastStack();
        if (next.app != null && next.app.thread != null) {  //要启动的activity的进程信息存在的情况，即新App的进程已启动
            //Resume running: " + next + " stopped=" + next.stopped + " visible=" + next.visible);
            //如果上一个activity是半透明的，则强制下一个activity的可见性更新，以便将其添加到WM的打开的应用程序列表中，并可以正确设置过渡动画。 
            //例如，按下“主页”按钮时将焦点对准半透明活动。 在这种情况下，启动器已经可见。 如果我们不将其添加到正在打开的应用程序中，则UpdateTransitToWallpaper（）可能无法将其识别为TRANSIT_WALLPAPER_OPEN动画，并运行一些有趣的动画。
            final boolean lastActivityTranslucent = lastStack != null && (lastStack.inMultiWindowMode() 
                            || (lastStack.mLastPausedActivity != null && !lastStack.mLastPausedActivity.fullscreen));
            //由于我们都在更改可见性并更新配置，因此所包含的逻辑必须同步。 ActivityRecord＃setVisibility最终将导致客户端代码安排布局。 
            //由于布局会检索当前配置，因此我们必须确保以下代码在布局发生之前对其进行更新。
            synchronized(mWindowManager.getWindowManagerLock()) {
                // This activity is now becoming visible.
                if (!next.visible || next.stopped || lastActivityTranslucent) {
                    next.setVisibility(true);
                }
                //计划启动时间来收集慢速应用程序的信息。
                next.startLaunchTickingLocked();
                ActivityRecord lastResumedActivity = lastStack == null ? null :lastStack.mResumedActivity;
                final ActivityState lastState = next.getState();
                mService.updateCpuStats();
                //Moving to RESUMED: " + next + " (in existing)");
                next.setState(RESUMED, "resumeTopActivityInnerLocked");
                mService.updateLruProcessLocked(next.app, true, null);
                updateLRUListLocked(next);
                mService.updateOomAdjLocked();
                //让窗口管理器根据新的activity顺序重新评估屏幕的方向
                boolean notUpdated = true;
                if (mStackSupervisor.isFocusedStack(this)) {
                    //当这是一些要求特定方向的活动或Keyguard被锁定时，我们具有特殊的旋转行为。 确保正确设置所有活动可见性，并在需要时更新转换以获取正确的旋转行为。
                    //否则，以下调用更新方向的操作可能会由于不可见的窗口调整大小而导致错误的配置传递给客户端。
                    // TODO: 在启动一个活动时，一旦正确设置了可见性，就立即删除它。
                    notUpdated = !mStackSupervisor.ensureVisibilityAndConfig(next, mDisplayId, true /* markFrozenIfConfigChanged */, false /* deferResume */);
                }
                if (notUpdated) {
                    //配置更新无法保留活动的现有实例，而是开始了一个新的实例。我们应该完成所有工作，但是只要确保我们的活动仍在最前面，并在发生奇怪的事情时安排另一个运行即可。
                    ActivityRecord nextNext = topRunningActivityLocked();
                    //Activity config changed during resume: " + next + ", new next: " + nextNext)
                    if (nextNext != next) {
                        //重做
                        mStackSupervisor.scheduleResumeTopActivities();
                    }
                    if (!next.visible || next.stopped) {
                        next.setVisibility(true);
                    }
                    next.completeResumeLocked();
                    if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                    return true;
                }
                try {
                    final ClientTransaction transaction = ClientTransaction.obtain(next.app.thread, next.appToken);
                    //交付所有未决结果。
                    ArrayList<ResultInfo> a = next.results;
                    if (a != null) {
                        final int N = a.size();
                        if (!next.finishing && N > 0) {
                            //Delivering results to " + next + ": " a
                            transaction.addCallback(ActivityResultItem.obtain(a));
                        }
                    }
                    if (next.newIntents != null) {
                        transaction.addCallback(NewIntentItem.obtain(next.newIntents, false /* andPause */));
                    }
                    //应用程序将不再停止。如果需要，清除窗口管理器中的app令牌停止状态
                    next.notifyAppResumed(next.stopped);
                    next.sleeping = false;
                    mService.getAppWarningsLocked().onResumeActivity(next);
                    mService.showAskCompatModeDialogLocked(next);
                    next.app.pendingUiClean = true;
                    next.app.forceProcessStateUpTo(mService.mTopProcessState);
                    next.clearOptionsLocked();
                    transaction.setLifecycleStateRequest(ResumeActivityItem.obtain(next.app.repProcState, mService.isNextTransitionForward()));
                    mService.getLifecycleManager().scheduleTransaction(transaction);
                } catch (Exception e) {
                    //哎呀，需要重新启动这个活动!
                    if (DEBUG_STATES) Slog.v(TAG_STATES, "Resume failed; resetting state to " + lastState + ": " + next);
                    next.setState(lastState, "resumeTopActivityInnerLocked");
                    // lastResumedActivity非空意味着存在lastStack。
                    if (lastResumedActivity != null) {
                        lastResumedActivity.setState(RESUMED, "resumeTopActivityInnerLocked");
                    }
                    Slog.i(TAG, "Restarting because process died: " + next);
                    if (!next.hasBeenLaunched) {
                        next.hasBeenLaunched = true;
                    } else if (SHOW_APP_STARTING_PREVIEW && lastStack != null && lastStack.isTopStackOnDisplay()) {
                        next.showStartingWindow(null /* prev */, false /* newTask */, false /* taskSwitch */);
                    }
                    mStackSupervisor.startSpecificActivityLocked(next, true, false);    //重点
                    if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                    return true;
                }
            }
            //从这一点上说，如果出了问题，就没有办法恢复activity。
            try {
                next.completeResumeLocked();
            } catch (Exception e) {
                // 如果抛出任何异常，请放弃此activity并尝试下一个。
                Slog.w(TAG, "Exception thrown during resume of " + next, e);
                requestFinishActivityLocked(next.appToken, Activity.RESULT_CANCELED, null, "resume-exception", true);
                if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
                return true;
            }
        } else {
            //哎呀，需要重新启动这个活动!
            if (!next.hasBeenLaunched) {
                next.hasBeenLaunched = true;
            } else {
                if (SHOW_APP_STARTING_PREVIEW) {
                    next.showStartingWindow(null /* prev */, false /* newTask */, false /* taskSwich */);
                }
                if (DEBUG_SWITCH) Slog.v(TAG_SWITCH, "Restarting: " + next);
            }
            if (DEBUG_STATES) Slog.d(TAG_STATES, "resumeTopActivityLocked: Restarting " + next);
            mStackSupervisor.startSpecificActivityLocked(next, true, true); //重点
        }
        if (DEBUG_STACK) mStackSupervisor.validateTopActivitiesLocked();
        return true;
    resumeTopActivityInnerLocked主要是做了一些状态检查，设置activity可见性、配置更改后的操作等
    resumeTopActivityInnerLocked最主要是调用 mStackSupervisor.startSpecificActivityLocked
    
    ActivityStackSupervisor.startSpecificActivityLocked(ActivityRecord r, boolean andResume, boolean checkConfig) {
        //这个activity的应用程序已经运行了吗?
        ProcessRecord app = mService.getProcessRecordLocked(r.processName, r.info.applicationInfo.uid, true);
        getLaunchTimeTracker().setLaunchTime(r);
        if (app != null && app.thread != null) {
            try {
                if ((r.info.flags&ActivityInfo.FLAG_MULTIPROCESS) == 0 || !"android".equals(r.info.packageName)) {
                    //如果它是标记为可在多个进程中运行的平台组件，则不要添加此组件，因为它实际上是框架的一部分，因此在进程中作为单独的apk进行跟踪没有意义。
                    app.addPackage(r.info.packageName, r.info.applicationInfo.longVersionCode, mService.mProcessStats);
                }
                realStartActivityLocked(r, app, andResume, checkConfig);
                return;
            } catch (RemoteException e) {
                Slog.w(TAG, "Exception when starting activity " + r.intent.getComponent().flattenToShortString(), e);
            }
            //如果抛出了死对象异常，则需要重新启动应用程序。
        }
        mService.startProcessLocked(r.processName, r.info.applicationInfo, true, 0, "activity", r.intent.getComponent(), false, false, true);
    }
    mService.getProcessRecordLocked中是否存在当前要启动activity的进程信息，若没有则开启新进程，若有，则调用realStartActivityLocked
    
    ActivityManagerService.startProcessLocked（L4071）：
        long startTime = SystemClock.elapsedRealtime();
        ProcessRecord app;
        if (!isolated) {    //开启新进程时，传入参数isolated为false，keepIfLarge为true，knownToBeDead为true
            app = getProcessRecordLocked(processName, info.uid, keepIfLarge);   //若不存在进程信息，则返回null，重用进程？
            checkTime(startTime, "startProcess: after getProcessRecord");
            if ((intentFlags & Intent.FLAG_FROM_BACKGROUND) != 0) {     //后台启动
                //如果我们在后台，请检查此进程是否不好。 如果是这样，我们只会默默地失败
                if (mAppErrors.isBadProcessLocked(info)) {
                    if (DEBUG_PROCESSES) Slog.v(TAG, "Bad process: " + info.uid + "/" + info.processName);
                    return null;
                }
            } else {    //非后台启动
                //当用户明确地启动一个进程时，请清除其崩溃计数，以便我们不会使其变坏，直到他们再次看到至少一个崩溃对话框为止；如果该进程很糟糕，则使该进程再次变好。
                if (DEBUG_PROCESSES) Slog.v(TAG, "Clearing bad process: " + info.uid + "/" + info.processName);
                mAppErrors.resetProcessCrashTimeLocked(info);
                if (mAppErrors.isBadProcessLocked(info)) {
                    EventLog.writeEvent(EventLogTags.AM_PROC_GOOD, UserHandle.getUserId(info.uid), info.uid, info.processName);
                    mAppErrors.clearBadProcessLocked(info);
                    if (app != null) {
                        app.bad = false;
                    }
                }
            }
        } else {
            //如果这是一个独立的进程，它就不能重用现有的进程。
            app = null;
        }
        //在以下情况下，我们无需做任何其他事情：
        //(1)有已经存在的 application记录；(2)调用者不认为它已死，或者没有附加线程对象，因此我们知道它不可能崩溃。(3)它被分配了一个pid，因此它要么正在启动，要么已经在运行。
        //startProcess: name=" + processName + " app=" + app + " knownToBeDead=" + knownToBeDead + " thread=" + " pid=" 
        if (app != null && app.pid > 0) {
            if ((!knownToBeDead && !app.killed) || app.thread == null) {
                //我们已经运行了应用程序，或者正在等待它出现(我们有一个pid，但还没有它的线程)，所以保留它。
                if (DEBUG_PROCESSES) Slog.v(TAG_PROCESSES, "App already running: " + app);
                //如果这是进程中的一个新包，则将该包添加到列表中
                app.addPackage(info.packageName, info.versionCode, mProcessStats);
                checkTime(startTime, "startProcess: done, added package to proc");
                return app;
            }
            //application record附加到以前的进程，现在清除它。
            if (DEBUG_PROCESSES || DEBUG_CLEANUP) Slog.v(TAG_PROCESSES, "App died: " + app);
            checkTime(startTime, "startProcess: bad proc running, killing");
            killProcessGroup(app.uid, app.pid);     //杀掉进程，使用了Process.killProcessGroup
            handleAppDiedLocked(app, true, true);   //主要功能，用于从活动管理器中删除已存在的进程，作为该进程消失的结果。清除与进程的所有连接。
            checkTime(startTime, "startProcess: done killing old proc");
        }
        String hostingNameStr = hostingName != null ? hostingName.flattenToShortString() : null;
        if (app == null) {
            checkTime(startTime, "startProcess: creating new process record");
            app = newProcessRecordLocked(info, processName, isolated, isolatedUid);     //内部new了一个ProcessRecord
            if (app == null) {
                Slog.w(TAG, "Failed making new process record for " + processName + "/" + info.uid + " isolated=" + isolated);
                return null;
            }
            app.crashHandler = crashHandler;
            app.isolatedEntryPoint = entryPoint;
            app.isolatedEntryPointArgs = entryPointArgs;
            checkTime(startTime, "startProcess: done creating new process record");
        } else {
            //如果这是进程中的一个新包，则将该包添加到列表中
            app.addPackage(info.packageName, info.versionCode, mProcessStats);
            checkTime(startTime, "startProcess: added package to existing proc");
        }
        //如果系统还没有准备好，那么在它准备好之前不要启动这个进程
        if (!mProcessesReady && !isAllowedWhileBooting(info) && !allowWhileBooting) {
            if (!mProcessesOnHold.contains(app)) {
                mProcessesOnHold.add(app);
            }
            if (DEBUG_PROCESSES) Slog.v(TAG_PROCESSES, "System not ready, putting on hold: " + app);
            checkTime(startTime, "startProcess: returning with proc on hold");
            return app;
        }
        checkTime(startTime, "startProcess: stepping in to startProcess");
        final boolean success = startProcessLocked(app, hostingType, hostingNameStr, abiOverride);
        checkTime(startTime, "startProcess: done starting proc!");
        return success ? app : null;
    startProcessLocked，先通过getProcessRecordLocked从mProcessNames中找到是否存在当前要启动的进程信息，若有，看是否要重用已存在的进程，还是要杀掉之前的进程重新启动
    若进程信息为null，则通过newProcessRecordLocked构造一个新的对象，最后通过startProcessLocked启动进程
    
    ActivityManagerService.startProcessLocked(ProcessRecord app, String hostingType, String hostingNameStr, boolean disableHiddenApiChecks, String abiOverride)：
        if (app.pendingStart) {     //进程还未启动时为false
            return true;
        }
        long startTime = SystemClock.elapsedRealtime();
        if (app.pid > 0 && app.pid != MY_PID) {     //进程的pid不匹配，则从mPidsSelfLocked中移除进程信息？
            checkTime(startTime, "startProcess: removing from pids map");
            synchronized (mPidsSelfLocked) {
                mPidsSelfLocked.remove(app.pid);
                mHandler.removeMessages(PROC_START_TIMEOUT_MSG, app);
            }
            checkTime(startTime, "startProcess: done removing from pids map");
            app.setPid(0);
        }
        if (DEBUG_PROCESSES && mProcessesOnHold.contains(app)) Slog.v(TAG_PROCESSES, "startProcessLocked removing on hold: " + app);
        mProcessesOnHold.remove(app);
        updateCpuStats();   //update cpu stats
        try {
            final int userId = UserHandle.getUserId(app.uid);
            AppGlobals.getPackageManager().checkPackageStartable(app.info.packageName, userId);
            int uid = app.uid;
            int[] gids = null;
            int mountExternal = Zygote.MOUNT_EXTERNAL_NONE;
            if (!app.isolated) {
                int[] permGids = null;
                checkTime(startTime, "startProcess: getting gids from package manager");
                final IPackageManager pm = AppGlobals.getPackageManager();
                permGids = pm.getPackageGids(app.info.packageName, MATCH_DEBUG_TRIAGED_MISSING, app.userId);
                StorageManagerInternal storageManagerInternal = LocalServices.getService(StorageManagerInternal.class);
                mountExternal = storageManagerInternal.getExternalStorageMountMode(uid, app.info.packageName);  //文件挂载，权限？
                //添加共享的应用程序和配置文件GID，以便应用程序可以共享某些资源（如共享库）并访问用户范围的资源
                if (ArrayUtils.isEmpty(permGids)) {
                    gids = new int[3];
                } else {
                    gids = new int[permGids.length + 3];
                    System.arraycopy(permGids, 0, gids, 3, permGids.length);
                }
                gids[0] = UserHandle.getSharedAppGid(UserHandle.getAppId(uid));     //共享
                gids[1] = UserHandle.getCacheAppGid(UserHandle.getAppId(uid));      //缓存
                gids[2] = UserHandle.getUserGid(UserHandle.getUserId(uid));         //用户
                //替换任何无效的gid
                if (gids[0] == UserHandle.ERR_GID) gids[0] = gids[2];
                if (gids[1] == UserHandle.ERR_GID) gids[1] = gids[2];
            }
            checkTime(startTime, "startProcess: building args");
            if (mFactoryTest != FactoryTest.FACTORY_TEST_OFF) {
                if (mFactoryTest == FactoryTest.FACTORY_TEST_LOW_LEVEL && mTopComponent != null
                                && app.processName.equals(mTopComponent.getPackageName())) {
                    uid = 0;
                }
                if (mFactoryTest == FactoryTest.FACTORY_TEST_HIGH_LEVEL && (app.info.flags&ApplicationInfo.FLAG_FACTORY_TEST) != 0) {
                    uid = 0;
                }
            }
            int runtimeFlags = 0;
            if ((app.info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {      //debuggable进程
                runtimeFlags |= Zygote.DEBUG_ENABLE_JDWP;
                runtimeFlags |= Zygote.DEBUG_JAVA_DEBUGGABLE;
                //同时打开可调试应用程序的CheckJNI。 否则打开是很不合适的
                runtimeFlags |= Zygote.DEBUG_ENABLE_CHECKJNI;
            }
            //如果manifest要求或者系统以安全模式启动的话，则以安全模式运行该应用程序
            if ((app.info.flags & ApplicationInfo.FLAG_VM_SAFE_MODE) != 0 || mSafeMode == true) {
                runtimeFlags |= Zygote.DEBUG_ENABLE_SAFEMODE;
            }
            if ("1".equals(SystemProperties.get("debug.checkjni"))) {
                runtimeFlags |= Zygote.DEBUG_ENABLE_CHECKJNI;
            }
            String genDebugInfoProperty = SystemProperties.get("debug.generate-debug-info");
            if ("1".equals(genDebugInfoProperty) || "true".equals(genDebugInfoProperty)) {
                runtimeFlags |= Zygote.DEBUG_GENERATE_DEBUG_INFO;
            }
            String genMiniDebugInfoProperty = SystemProperties.get("dalvik.vm.minidebuginfo");
            if ("1".equals(genMiniDebugInfoProperty) || "true".equals(genMiniDebugInfoProperty)) {
                runtimeFlags |= Zygote.DEBUG_GENERATE_MINI_DEBUG_INFO;
            }
            if ("1".equals(SystemProperties.get("debug.jni.logging"))) {
                runtimeFlags |= Zygote.DEBUG_ENABLE_JNI_LOGGING;
            }
            if ("1".equals(SystemProperties.get("debug.assert"))) {
                runtimeFlags |= Zygote.DEBUG_ENABLE_ASSERT;
            }
            if (mNativeDebuggingApp != null && mNativeDebuggingApp.equals(app.processName)) {
                //启用本机调试器所需的所有调试标志
                runtimeFlags |= Zygote.DEBUG_ALWAYS_JIT;          // Don't interpret anything
                runtimeFlags |= Zygote.DEBUG_GENERATE_DEBUG_INFO; // Generate debug info
                runtimeFlags |= Zygote.DEBUG_NATIVE_DEBUGGABLE;   // Disbale optimizations
                mNativeDebuggingApp = null;
            }
            if (app.info.isPrivilegedApp() && DexManager.isPackageSelectedToRunOob(app.pkgList.keySet())) {
                runtimeFlags |= Zygote.ONLY_USE_SYSTEM_OAT_FILES;
            }
            if (!disableHiddenApiChecks && !mHiddenApiBlacklist.isDisabled()) { //检查隐藏API调用？
                app.info.maybeUpdateHiddenApiEnforcementPolicy(mHiddenApiBlacklist.getPolicyForPrePApps(), mHiddenApiBlacklist.getPolicyForPApps());
                @HiddenApiEnforcementPolicy int policy = app.info.getHiddenApiEnforcementPolicy();
                int policyBits = (policy << Zygote.API_ENFORCEMENT_POLICY_SHIFT);
                if ((policyBits & Zygote.API_ENFORCEMENT_POLICY_MASK) != policyBits) {
                    throw new IllegalStateException("Invalid API policy: " + policy);
                }
                runtimeFlags |= policyBits;
            }
            String invokeWith = null;
            if ((app.info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
                //可调试的应用程序可能在其库目录中包含包装脚本
                String wrapperFileName = app.info.nativeLibraryDir + "/wrap.sh";
                StrictMode.ThreadPolicy oldPolicy = StrictMode.allowThreadDiskReads();
                try {
                    if (new File(wrapperFileName).exists()) {
                        invokeWith = "/system/bin/logwrapper " + wrapperFileName;
                    }
                } finally {
                    StrictMode.setThreadPolicy(oldPolicy);
                }
            }
            String requiredAbi = (abiOverride != null) ? abiOverride : app.info.primaryCpuAbi;
            if (requiredAbi == null) {  
                requiredAbi = Build.SUPPORTED_ABIS[0];
            }
            String instructionSet = null;
            if (app.info.primaryCpuAbi != null) {
                instructionSet = VMRuntime.getInstructionSet(app.info.primaryCpuAbi);
            }
            app.gids = gids;
            app.requiredAbi = requiredAbi;
            app.instructionSet = instructionSet;
            //必须设置每个用户的SELinux上下文
            if (TextUtils.isEmpty(app.info.seInfoUser)) {
                Slog.wtf(TAG, "SELinux tag not defined", new IllegalStateException("SELinux tag not defined for " + app.info.packageName + " (uid " + app.uid + ")"));
            }
            final String seInfo = app.info.seInfo + (TextUtils.isEmpty(app.info.seInfoUser) ? "" : app.info.seInfoUser);
            //启动进程。它将成功并返回包含新进程的PID的结果，否则将引发RuntimeException
            final String entryPoint = "android.app.ActivityThread";     //将以反射方式启动 ActivityThread main函数
            return startProcessLocked(hostingType, hostingNameStr, entryPoint, app, uid, gids,
                            runtimeFlags, mountExternal, seInfo, requiredAbi, instructionSet, invokeWith, startTime);
        } catch (RuntimeException e) {
            Slog.e(TAG, "Failure starting process " + app.processName, e);
            //尝试启动进程时出了点问题。 一种常见的情况是，由于活动升级而冻结了软件包。 要恢复，请清除与启动此进程相关的所有活动簿记。 
            //（当程序包最初通过KILL_APPLICATION_MSG冻结时，我们已经调用了此方法一次，因此再次使用它没有什么害处。）
            forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid), false,
                            false, true, false, false, UserHandle.getUserId(app.userId), "start failure");
            return false;
        }
    这个startProcessLocked主要用于设置一些runtimeFlags，和共享文件夹相关、一些debug信息、检查是否调用隐藏API等
    最后又调用一个startProcessLocked
    
    ActivityManagerService.startProcessLocked（L4400）：
        app.pendingStart = true;        //设为true，表示要正式启动进程了
        app.killedByAm = false;
        app.removed = false;
        app.killed = false;
        final long startSeq = app.startSeq = ++mProcStartSeqCounter;
        app.setStartParams(uid, hostingType, hostingNameStr, seInfo, startTime);
        if (mConstants.FLAG_PROCESS_START_ASYNC) {      //默认为true，具体看配置文件（）
            if (DEBUG_PROCESSES) Slog.i(TAG_PROCESSES, "Posting procStart msg for " + app.toShortString());
            mProcStartHandler.post(() -> {
                try {
                    synchronized (ActivityManagerService.this) {
                        final String reason = isProcStartValidLocked(app, startSeq);    //检查进程是否已被停止？考虑多线程？
                        if (reason != null) {
                            Slog.w(TAG_PROCESSES, app + " not valid anymore," + " don't start process, " + reason);
                            app.pendingStart = false;
                            return;
                        }
                        app.usingWrapper = invokeWith != null || SystemProperties.get("wrap." + app.processName) != null;
                        mPendingStarts.put(startSeq, app);
                    }
                    final ProcessStartResult startResult = startProcess(app.hostingType, entryPoint,
                                app, app.startUid, gids, runtimeFlags, mountExternal, app.seInfo,
                                requiredAbi, instructionSet, invokeWith, app.startTime);
                    synchronized (ActivityManagerService.this) {
                        handleProcessStartedLocked(app, startResult, startSeq);
                    }
                } catch (RuntimeException e) {
                    synchronized (ActivityManagerService.this) {
                        Slog.e(TAG, "Failure starting process " + app.processName, e);
                        mPendingStarts.remove(startSeq);
                        app.pendingStart = false;
                        forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid),
                                    false, false, true, false, false, UserHandle.getUserId(app.userId), "start failure");   //确保进程正确启动？
                    }
                }
            });
            return true;
        } else {
            try {
                final ProcessStartResult startResult = startProcess(hostingType, entryPoint, app,
                            uid, gids, runtimeFlags, mountExternal, seInfo, requiredAbi, instructionSet, invokeWith, startTime);
                handleProcessStartedLocked(app, startResult.pid, startResult.usingWrapper, startSeq, false);
            } catch (RuntimeException e) {
                Slog.e(TAG, "Failure starting process " + app.processName, e);
                app.pendingStart = false;
                forceStopPackageLocked(app.info.packageName, UserHandle.getAppId(app.uid),
                            false, false, true, false, false, UserHandle.getUserId(app.userId), "start failure");
            }
            return app.pid > 0;
        }
    这段主要为ProcessRecord中的几个变量赋初值，然后分了是否要异步启动的两种情况，但都是调用startProcess启动进程的，启动后又做了一些检查工作
    ActivityManagerService.startProcess：
        Trace.traceBegin(Trace.TRACE_TAG_ACTIVITY_MANAGER, "Start proc: " + app.processName);
        checkTime(startTime, "startProcess: asking zygote to start proc");
        final ProcessStartResult startResult;
        if (hostingType.equals("webview_service")) {
            startResult = startWebView(entryPoint, app.processName, uid, uid, gids, runtimeFlags, mountExternal,
                                app.info.targetSdkVersion, seInfo, requiredAbi, instructionSet,
                                app.info.dataDir, null, new String[] {PROC_START_SEQ_IDENT + app.startSeq});
        } else {
            startResult = Process.start(entryPoint, app.processName, uid, uid, gids, runtimeFlags, mountExternal,
                            app.info.targetSdkVersion, seInfo, requiredAbi, instructionSet,
                            app.info.dataDir, invokeWith, new String[] {PROC_START_SEQ_IDENT + app.startSeq});
        }
        checkTime(startTime, "startProcess: returned from zygote!");
        return startResult;
    启动webview_service进程？
    
    Process.start内部调用了zygoteProcess.start函数，ZygoteProcess为与Zygote进程相关的状态？当前处于SystemServer进程
    ZygoteProcess.start内部调用了startViaZygote函数，表示将通过Zygote进程fork出一个子进程
    ZygoteProcess.startViaZygote函数内部主要设置了一些启动参数（argsForZygote），最后又调用了zygoteSendArgsAndGetResult（return zygoteSendArgsAndGetResult(openZygoteSocketIfNeeded(abi), argsForZygote);）
    openZygoteSocketIfNeeded，如果尚未打开socket，则尝试打开到Zygote进程的socket。 如果已经打开，则不执行任何操作。 可能会阻塞并重试。要求持有mLock锁
    在调用 zygoteSendArgsAndGetResult 之前，先调用了 openZygoteSocketIfNeeded 方法开启与Zygote进程间的socket连接，并请求查询支持的ABI列表
    ZygoteProcess.openZygoteSocketIfNeeded（开启socket连接，并请求ABI列表，检查是否支持ABI）：
        if (primaryZygoteState == null || primaryZygoteState.isClosed()) {  //检查连接是否已存在
            try {
                primaryZygoteState = ZygoteState.connect(mSocket);      //进行连接，mSocket名称为 zygote
            } catch (IOException ioe) {
                throw new ZygoteStartFailedEx("Error connecting to primary zygote", ioe);
            }
            maybeSetApiBlacklistExemptions(primaryZygoteState, false);
            maybeSetHiddenApiAccessLogSampleRate(primaryZygoteState);
        }
        if (primaryZygoteState.matches(abi)) {      //检查ABI是否支持
            return primaryZygoteState;
        }
    开启socket连接，并请求ABI列表
    ZygoteState.connect：
        DataInputStream zygoteInputStream = null;
        BufferedWriter zygoteWriter = null;
        final LocalSocket zygoteSocket = new LocalSocket();
        try {
            zygoteSocket.connect(address);      //使用LocalSocket方式连接
            zygoteInputStream = new DataInputStream(zygoteSocket.getInputStream()); //输入流
            zygoteWriter = new BufferedWriter(new OutputStreamWriter(zygoteSocket.getOutputStream()), 256); //输出流
        } catch (IOException ex) {
            zygoteSocket.close();
            throw ex;
        }
        String abiListString = getAbiList(zygoteWriter, zygoteInputStream);     //通过socket向Zygote进程查询支持的ABI列表
        Log.i("Zygote", "Process: zygote socket " + address.getNamespace() + "/" + address.getName() + " opened, supported ABIS: " + abiListString);
        return new ZygoteState(zygoteSocket, zygoteInputStream, zygoteWriter, Arrays.asList(abiListString.split(",")));
    
    //将参数列表发送到Zygote进程，该进程将启动一个新的子进程并返回该子进程的pid。 请注意：本实现将参数列表中的换行符替换为空格
    ZygoteProcess.zygoteSendArgsAndGetResult：
        try {
            //如果任何变量格式错误，请尽早抛出。这意味着我们可以避免对Zygote的部分响应的回复
            int sz = args.size();
            for (int i = 0; i < sz; i++) {
                if (args.get(i).indexOf('\n') >= 0) {   //检查传入的参数列表中参数格式
                    throw new ZygoteStartFailedEx("embedded newlines not allowed");
                }
            }
            //请看 com.android.internal.os.SystemZygoteInit.readArgumentList()
            //目前，Zygote进程的连线格式为：参数数量（本质上为argc）；多个以新行分隔的参数字符串等于count
            //在Zygote进程读取这些信息后，它将返回子进程的pid或-1（失败时），然后是布尔值以指示是否使用了包装程序（wrapper进程）
            final BufferedWriter writer = zygoteState.writer;
            final DataInputStream inputStream = zygoteState.inputStream;
            writer.write(Integer.toString(args.size()));        //参数个数
            writer.newLine();
            for (int i = 0; i < sz; i++) {
                String arg = args.get(i);
                writer.write(arg);      //参数
                writer.newLine();       //准备向Zygote写参数，每写一个参数换一行
            }
            writer.flush();
            //这里有超时吗？
            Process.ProcessStartResult result = new Process.ProcessStartResult();
            //始终从输入流中读取整个结果，以避免在流中留下字节，以免将来的进程偶然发现
            result.pid = inputStream.readInt();     //子进程id，-1为fork失败
            result.usingWrapper = inputStream.readBoolean();    //是否使用 Wrapper进程
            if (result.pid < 0) {
                throw new ZygoteStartFailedEx("fork() failed");
            }
            return result;
        } catch (IOException ex) {
            zygoteState.close();
            throw new ZygoteStartFailedEx(ex);
        }
    使用socket向Zygote进程发送参数信息，Zygote进程中fork子进程，并返回是否fork成功的结果，最后返回结果
    后续转入Zygote进程执行
    
#### 转入Zygote进程执行

    当前处于Zygote进程执行
    在ZygoteServer.runSelectLoop中：
        ...
        ZygoteConnection connection = peers.get(i);     //遍历列表，拿出已经建立的连接
        final Runnable command = connection.processOneCommand(this);    //读取传来的参数，并fork子进程，返回一个runnable，其中执行ActivityThread的main方法
        if (mIsForkChild) {     //是否处于子进程的标志，由子进程fork成功后，在子进程中设置（processOneCommand中调用的zygoteServer.setForkChild()）
            //我们在子进程中。如果processOneCommand尚未调用“ exec”，则我们应该始终在此阶段运行命令。
            if (command == null) {
                throw new IllegalStateException("command == null");
            }
            return command;
        } else {
            //我们在Zygote中——我们不应该有任何命令运行
            if (command != null) {
                throw new IllegalStateException("command != null");
            }
            if (connection.isClosedByPeer()) {
                connection.closeSocket();
                peers.remove(i);
                fds.remove(i);
            }
        }
        ...
    子进程fork成功，则return command，退出了runSelectLoop循环，并在之后执行了command（在ZygoteInit的main方法的最后）
    
    processOneCommand 从命令套接字读取一个启动命令。如果成功，则将fork一个子进程，并在子进程中返回一个调用该子程序main方法（或等效方法）的Runnable。
    在父进程（Zygote）中始终返回null。如果客户端关闭套接字，则会设置EOF条件，调用者可以通过调用ZygoteConnection.isClosedByPeer进行测试。
    ZygoteConnection.processOneCommand（与AMS通信，读取参数，返回fork结果）：
        String args[];
        Arguments parsedArgs = null;
        FileDescriptor[] descriptors;
        args = readArgumentList();  //依次读取从上面的zygoteSendArgsAndGetResult中写入的参数
        descriptors = mSocket.getAncillaryFileDescriptors();    //fd
        //readArgumentList仅在达到EOF并且没有可用数据读取时才返回null。仅当远程socket断开连接时才会发生这种情况
        if (args == null) {
            isEof = true;
            return null;
        }
        int pid = -1;
        FileDescriptor childPipeFd = null;
        FileDescriptor serverPipeFd = null;
        parsedArgs = new Arguments(args);   //解析输入参数，构造函数内部调用了parseArgs，包括ABI列表的查询请求解析
        if (parsedArgs.abiListQuery) {      //表示这次接收到的是ABI查询请求，第一行参数为1，第二行为--query-abi-list
            handleAbiListQuery();           //回复支持的ABI列表，以字节数组形式发送，先发送字节个数，然后是字节内容
            return null;                    //返回当前函数，等待client的下次参数输入
        }
        if (parsedArgs.preloadDefault) {    //是否请求开始预加载默认资源和类。这个参数只有在Zygote处于惰性预加载模式(当它以——enable-lazy-preload开始时)
            handlePreload();                //预加载
            return null;
        }
        if (parsedArgs.preloadPackage != null) {    //需要预加载的APK路径
            handlePreloadPackage(parsedArgs.preloadPackage, parsedArgs.preloadPackageLibs,
                    parsedArgs.preloadPackageLibFileName, parsedArgs.preloadPackageCacheKey);   //抛异常，Zygote不支持包预加载
            return null;
        }
        if (parsedArgs.apiBlacklistExemptions != null) {    //API黑名单豁免
            handleApiBlacklistExemptions(parsedArgs.apiBlacklistExemptions);    //隐藏API豁免处理
            return null;
        }
        if (parsedArgs.hiddenApiAccessLogSampleRate != -1) {    //记录对事件日志的隐藏API访问的采样率
            handleHiddenApiAccessLogSampleRate(parsedArgs.hiddenApiAccessLogSampleRate);
            return null;
        }
        if (parsedArgs.permittedCapabilities != 0 || parsedArgs.effectiveCapabilities != 0) {
            throw new ZygoteSecurityException("Client may not specify capabilities: " +
                "permitted=0x" + Long.toHexString(parsedArgs.permittedCapabilities) + ", effective=0x" + Long.toHexString(parsedArgs.effectiveCapabilities));
        }
        applyUidSecurityPolicy(parsedArgs, peer);   //设置uid、gid？
        applyInvokeWithSecurityPolicy(parsedArgs, peer);    //安全检查，invokeWith与App的debuggable有关，只有可debug且其nativeLibraryDir下有包装脚本的App才不为null
        applyDebuggerSystemProperty(parsedArgs);    //debug设置
        applyInvokeWithSystemProperty(parsedArgs);  //给invokeWith设置系统属性
        int[][] rlimits = null;
        if (parsedArgs.rlimits != null) {
            rlimits = parsedArgs.rlimits.toArray(intArray2d);
        }
        int[] fdsToIgnore = null;
        if (parsedArgs.invokeWith != null) {
            try {
                FileDescriptor[] pipeFds = Os.pipe2(O_CLOEXEC);     //pipe设置？
                childPipeFd = pipeFds[1];
                serverPipeFd = pipeFds[0];
                Os.fcntlInt(childPipeFd, F_SETFD, 0);
                fdsToIgnore = new int[]{childPipeFd.getInt$(), serverPipeFd.getInt$()};
            } catch (ErrnoException errnoEx) {
                throw new IllegalStateException("Unable to set up pipe for invoke-with", errnoEx);
            }
        }
        //为了避免将描述符泄漏到Zygote子进程，native代码必须先在子进程中关闭两个Zygote socket描述符，然后再从Zygote root切换到UID和正在启动的应用程序特权
        //为了避免在关闭两个LocalSocket对象时出现“错误的文件描述符”错误，通过dup2()调用释放了Posix文件描述符，该调用关闭了socket并将打开的描述符替换为 /dev/null
        int [] fdsToClose = { -1, -1 };
        FileDescriptor fd = mSocket.getFileDescriptor();
        if (fd != null) {
            fdsToClose[0] = fd.getInt$();
        }
        fd = zygoteServer.getServerSocketFileDescriptor();
        if (fd != null) {
            fdsToClose[1] = fd.getInt$();
        }
        fd = null;
        //fork子进程，这些参数为startViaZygote中输入的参数argsForZygote，内部调用native方法，最终应该是使用fork()系统调用
        pid = Zygote.forkAndSpecialize(parsedArgs.uid, parsedArgs.gid, parsedArgs.gids,
                parsedArgs.runtimeFlags, rlimits, parsedArgs.mountExternal, parsedArgs.seInfo,
                parsedArgs.niceName, fdsToClose, fdsToIgnore, parsedArgs.startChildZygote, parsedArgs.instructionSet, parsedArgs.appDataDir);
        try {
            if (pid == 0) {     //0表示fork成功，后续在子进程中执行
                // in child
                zygoteServer.setForkChild();    //设置ZygoteServer的mIsForkChild为true
                zygoteServer.closeServerSocket();   //关闭ServerSocket
                IoUtils.closeQuietly(serverPipeFd); //
                serverPipeFd = null;
                return handleChildProc(parsedArgs, descriptors, childPipeFd, parsedArgs.startChildZygote);  //startChildZygote为false，Process.start中传入的
            } else {    //pid不为0，大于0为子进程pid，小于0表示fork失败，当前在Zygote进程中
                //在父进程中。pid <0表示失败，将在handleParentProc中进行处理
                IoUtils.closeQuietly(childPipeFd);
                childPipeFd = null;
                handleParentProc(pid, descriptors, serverPipeFd);   //在Zygote中的后续处理
                return null;
            }
        } finally {
            IoUtils.closeQuietly(childPipeFd);
            IoUtils.closeQuietly(serverPipeFd);
        }
    processOneCommand中，先接收由SystemServer发送的查询ABI列表请求，并返回所支持的ABI列表；
    然后接收由zygoteSendArgsAndGetResult发送的进程相关参数，接着fork子进程，使用handleChildProc在子进程中作后续处理，handleParentProc作当前Zygote进程的后续处理
    handleParentProc中主要是对子进程进行包装？若包装成功？则pid值做了改变，并且usingWrapper为true，最后，通过socket向SystemServer进程的ZygoteProcess回复子进程pid和usingWrapper值
    handleParentProc回复以后，会在processOneCommand中在此读取值，此次值为null，表示连接断开，然后关闭socket连接
    在子进程中的后续处理，ZygoteConnection.handleParentProc：
        
#### 在子进程中的后续处理
    
    当前为App进程
    ZygoteConnection.handleParentProc：
        //在我们到达这里时，native代码已关闭两个实际的Zygote socket连接，并使用/dev/null替换了它们。LocalSocket对象仍然需要正确关闭。
        closeSocket();      //关闭mSocket
        if (descriptors != null) {
            try {
                Os.dup2(descriptors[0], STDIN_FILENO);
                Os.dup2(descriptors[1], STDOUT_FILENO);
                Os.dup2(descriptors[2], STDERR_FILENO);
                for (FileDescriptor fd: descriptors) {
                    IoUtils.closeQuietly(fd);
                }
            } catch (ErrnoException ex) {
                Log.e(TAG, "Error reopening stdio", ex);
            }
        }
        if (parsedArgs.niceName != null) {
            Process.setArgV0(parsedArgs.niceName);
        }
        //postFork事件的结束
        if (parsedArgs.invokeWith != null) {        //invokeWith和debug有关，大多数情况下该项应该为null？
            //命令行执行？
            WrapperInit.execApplication(parsedArgs.invokeWith, parsedArgs.niceName, parsedArgs.targetSdkVersion,
                            VMRuntime.getCurrentInstructionSet(), pipeFd, parsedArgs.remainingArgs);
            //不应该到这里
            throw new IllegalStateException("WrapperInit.execApplication unexpectedly returned");
        } else {
            if (!isZygote) {    //isZygote为false，在Process.start中明确传入的值
                return ZygoteInit.zygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, null /* classLoader */);
            } else {
                return ZygoteInit.childZygoteInit(parsedArgs.targetSdkVersion, parsedArgs.remainingArgs, null /* classLoader */);
            }
        }
    
    zygoteInit，通过Zygote进程启动时调用的主要函数。如果使用Zygote startup对nativeFinishInit()中的native代码进行合理化处理，则可以将其与main()统一起来
    ZygoteInit.zygoteInit：
        RuntimeInit.redirectLogStreams();   //将System.out和System.err重定向到Android log
        RuntimeInit.commonInit();           //一些初始设置，比如给Thread设置pre handler和default handler、时区初始化、日志、trace、userAgent等
        ZygoteInit.nativeZygoteInit();      //启动Binder线程池，对应native源码位于/frameworks/base/core/jni/AndroidRuntime.cpp中
        return RuntimeInit.applicationInit(targetSdkVersion, argv, classLoader);
    RuntimeInit.applicationInit：
        nativeSetExitWithoutCleanup(true);  //如果应用程序调用System.exit()，请立即终止该进程，而无需运行任何shutdown hooks。无法正常关闭Android应用程序。其中，Android运行时shutdown hook会关闭Binder驱动程序，这可能导致剩余的运行线程在进程实际退出之前崩溃
        //我们希望对堆的利用采取积极的态度，以避免占用过多不需要的内存
        VMRuntime.getRuntime().setTargetHeapUtilization(0.75f);
        VMRuntime.getRuntime().setTargetSdkVersion(targetSdkVersion);
        final Arguments args = new Arguments(argv);     //构造函数中解析参数，startClass 要调用的类名，startArgs 其main方法参数
        //其余参数传递给起始类的静态main方法
        return findStaticMain(args.startClass, args.startArgs, classLoader);
    findStaticMain中，最后返回一个内部以反射方式执行startClass main方法的runnable，这个startClass为前面传入的ActivityThread
    ZygoteProcess.startViaZygote中，在最后给argsForZygote传入startClass和startArgs的值
    这个返回的runnable将在ZygoteInit的main方法的最后执行
    
下面将转入[Application启动流程](profile/Android/Application启动流程.md)