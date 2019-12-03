

###### Android开发通用框架设计思路（MVVM）：

    一、框架引用：
        MVVM框架：DataBinding（数据绑定）、LifeCycle（生命周期）、LiveData/RxJava（数据）、
        ViewModel、Room（数据库）、Paging（分页）  、WorkManager（工作排程）、Glide（图像）、
        Retrofit（HTTP）、RxPermission（权限） ...

    二、网络封装：
        Retrofit CallAdapter.Factory封装：
          1、onStart 网络请求前（考虑多个接口同时请求的情况下loading框的显示）
          2、loadAllComplete 请求结束后（包括请求成功、失败、服务异常、网络异常等情况后调用，考虑
          多个接口全部返回后再取消loading框，其他情况...）
          3、onNetError 网络异常情况，IO异常算netError？（考虑网络异常时的界面显示处理，
          Toast提示等场景；有多个接口同时请求时的问题，异常统一处理、单独处理等）
          4、onServerError (500 >= code < 600) 服务异常（考虑服务端异常时界面处理，Toast提示等场景；
          有多个接口同时请求时的问题，异常统一处理、单独处理等）
          5、onClientError (400 >= code < 500) 客户端异常（考虑客户端异常时界面处理，Toast提示等场景；
          有多个接口同时请求时的问题，异常统一处理、单独处理等）
          6、onSuccess 请求成功（接口请求成功的处理，包括有无数据返回等等情况）
          7、onCookie cookie处理
          8、unexpectedError 未知异常（除了IO异常和上述已知 code的情况）
          9、onCancel 取消网络请求（包括主动取消、Activity销毁时取消等情况，可防止Activity泄漏等）
          session？？

    三、MVVM架构模式封装：
        1、分层：
            Model层：
                    --数据类（DataSource）
                    --数据源（Repository）：包括网络数据、本地数据，以LiveData通知界面更新
            UseCase层：
                    --封装业务逻辑，业务与View分离；
                    --UseCase从整理数据源的数据，只给ViewModel提供所需数据；
            ViewModel层：
                    --ViewModel（枢纽）：处理数据的获取（Repository）与分发（LiveData）
                    --DataBinding（ViewModel）：处理数据的双向绑定
                    使用 RxJava 的 Flowable 作为数据源, 在 ViewModel 的 Flowable.subscribe( )里调用 LiveData#setValue()
                    LiveData 就是真实值的包装，长期可观察的对象，不可以也不应该不断指向新的对象
                    把 ViewModel 里 LiveData 的引用传递给 Repo，LiveData 更多是要在 ViewModel 和 View 这边去发挥作用，而 Repo 本身是可以被更多地方使用，例如几个 Repo 之间可以互相调用，也即现在比较普遍的模块化框架下几个模块之间的功能互相 invoke，这种情况下我们并不需要 LiveData，所以直接传LiveData不太好？
            View层：Activity、Fragment等内容显示；
                    --View：持有ViewModel、DataBinding

        2、生命周期控制：
            使用LifeCycle将生命周期相关业务分离出去（比如：监听器的绑定解除等）；

        3、ViewModel：
            生命周期敏感库：在横竖屏切换等 savedInstanceState的情况下不用处理处理

        4、Base封装：
            列表页base封装；
            将数据请求页面相关的回调封装到DataBinding的ViewModel中？？？
            savedInstanceState，ViewModel不能处理的数据的处理

        5、动画：
        6、ImageLoader：
        7、工具类：
            Format、Convert、Check、Net、SharedPreference、Span、App等

        8、依赖注入：使用Kotlin
        9、换肤：
        10、回调函数：
        11、字体适配：SP适用于字体大小跟随用户字体大小设置。DP，一些特殊情况,不想跟随系统字体变化，可以使用DP。

    四、性能优化：
        AspectJ、ASM、ReDex
        1、内存优化：
            内存DUMP，使用 AndroidStudio自带的分析工具-profile，或者是MemoryAnalyzer分析hprof文件，
            或者使用HaHa库，自定义代码分析（如：重复图片分析等，参考：https://github.com/jigangzhang/Chapter04）
            
        2、奔溃优化：
            奔溃监控可使用 Bugly、阿里啄木鸟等监控平台
            
        3、卡顿优化：
            https://www.jianshu.com/p/7e9ca2c73c97
            通过DDMS 使用Systrace（Trace.traceBegin/end的使用）、traceview定位并分析具体情况；
            或插桩、埋点等方式
        
        4、启动优化；
            IdleHandler（https://www.wanandroid.com/wenda/show/8723）
            onCreate，onStart，onResume中耗时较短但非必要的代码可以放到IdleHandler中执行，减少启动时间
        
        5、IO优化：
        
        6、存储优化：
        
        7、网络优化：
            流量监控等
        
        8、电量优化：
        
        9、UI渲染优化：
            Layout Inspector 查看 View 层级；开发者选项中的GPU过渡绘制
        
        10、包体积优化：
            
            