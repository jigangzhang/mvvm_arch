## Handler消息传递流程分析

UML类图
<br><img src="handler.png" width="500" height="300"/><br>
 整个请求流程中涉及到的主要类如上图。

* 基本使用步骤：
    >1. Looper.prepare();
    >2. init handler; 
    >3. Looper.loop();//陷入死循环

* 准备阶段：<br>
    Looper.prepare();
    
    通过ThreadLocal为当前线程获取或初始化Looper
    
* 消息分发：<br>
    Looper.loop();
    
    在死循环中从消息队列中获取消息进行处理：<br>
    将消息发送到对应的handler，由handler进行处理 -->
    `msg.target.dispatchMessage(msg);`
        最后回收msg --> `msg.recycleUnchecked();`
        
* 发送消息：<br>
    post/sendMessage 时将参数组装成一个完整的Message后加入 mQueue队列中。
    
* 消息处理：<br>
    由Looper分发消息至Handler：
        先由msg自带的callback处理，若没有则由Handler.Callback处理，若没有实现该接口，
        最后由Handler内部的handleMessage处理。        
    ```
    public void dispatchMessage(Message msg) {
        if (msg.callback != null) {
            handleCallback(msg);
            } else {
                if (mCallback != null) {
                    if (mCallback.handleMessage(msg)) {
                        return;
                    }
                }
            handleMessage(msg);
        }
    }
    ```

#### 同步屏障

    消息队列的同步屏障是由 MessageQueue.postSyncBarrier设置的，以当前时间为准，使Message的target 为 null 插入到消息队列
    MessageQueue.next中获取Message时，先看是否有同步屏障（Message的target为null），若有，则只处理异步消息（队列中无异步消息则会等待，nativePollOnce）
    MessageQueue.removeSyncBarrier，移除同步屏障，之后就可以处理同步消息了（next方法不会返回同步屏障对应的Message，所以需要主动移除）
    同步屏障的设置为：mHandler.getLooper().getQueue().postSyncBarrier()
    
    同步屏障只在ViewRootImpl中有使用，不对外开放（使用@hide）
    ViewRootImpl.scheduleTraversals()：
        mTraversalBarrier = mHandler.getLooper().getQueue().postSyncBarrier();      //设置同步屏障
        mChoreographer.postCallback(Choreographer.CALLBACK_TRAVERSAL, mTraversalRunnable, null);    //发送异步消息
    
    对屏幕刷新需要优先处理，否则会有卡顿现象，使用同步屏障优先处理异步消息（消息队列的优先级）
    屏幕刷新是在接收到VSYNC信号后才开始的