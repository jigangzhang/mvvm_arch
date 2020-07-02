
## POSIX线程

    在进程中创建新线程
    在一个进程中同步线程之间的的数据访问
    修改线程的属性
    在同一个进程中，从一个线程中控制另一个线程

#### 什么是线程

    一个程序中的多个执行路线就叫做线程，thread
    线程是一个进程内部的一个控制序列
    进程执行fork调用时将创建出该进程的一份新副本，它拥有自己的变量和PID，时间调度也是独立的，它的执行几乎完全独立于父进程
    在进程中创建一个新线程时，新的执行线程将拥有自己的栈（有自己的局部变量），但与它的创建者共享全局变量、文件描述符、信号处理函数和当前目录状态
    POSIX标准
    NGPT，New Generation POSIX Thread，下一代POSIX线程
    NPTL，Native POSIX Thread Library，本地POSIX线程库

#### 线程的优点和缺点

    新线程的创建代价要比新进程小得多
    使用线程的优点：
        让程序看起来好像是在同时做两件事情是很有用的
        一个混杂着输入、计算和输出的程序，可以将这几个部分分离为3个线程来执行，从而改善程序执行的性能
        线程之间的切换需要操作系统做的工作要比进程之间的切换少得多，因此多个线程对资源的需求要远小于多个进程
    缺点：
        编写多线程程序需要非常仔细的设计
        对多线程程序的调试要比对单线程程序的调试困难的多，线程之间的交互非常难于控制
        单处理器上的多线程不一定运行得更快

#### 第一个线程程序

    线程有关的函数库调用，绝大多数以pthread_开头
    为使用这些函数库调用必须定义宏_REENTRANT，包含头文件pthread.h，编译时使用-lpthread来链接线程库
    errno、fputs等通常用一个全局性区域来缓存输出数据，在多线程中很容易被改变
    可重入的例程可解决上面的问题
    可重入部分通常只使用局部变量，这使得每次对该代码的调用都将获得它自己的唯一的一份数据副本
    宏_REENTRANT告诉编译器需要可重入功能，它必须位于任何 #include 之前
    _REENTRANT做的事情：
        对部分函数重新定义它们的可安全重入的版本，这些函数的名字一般不会改变，只在函数名后面添加_r字符串，如gethostbyname变为gethostbyname_r
        stdio.h中原来以宏的形式实现的一些函数将变为可安全重入的函数
        在errno.h中定义的变量errno现在将成为一个函数调用，能够以一种多线程安全的方式来获得真正的errno值
    
    int pthread_create(pthread_t *thread, pthread_attr_t *attr, void *(*start_routine)(void *), void *arg);     （pthread.h）
    创建一个新线程
    thread指针，线程被创建时，其指向的变量中将被写入一个标识符，用该标识符来引用新线程
    attr 用于设置线程的属性，一般不需要特殊的属性，只需设置该参数为NULL
    最后两个参数分别是线程将要启动执行的函数和传递给该函数的参数
    void *(*start_routine)(void *)，表示传递一个函数地址，
    该函数以一个指向void的指针为参数，返回值也是一个指向void的指针，因此可以传递任何类型的参数并返回任何类型的指针
    fork后，父子进程将在同一位置继续执行下去
    新线程必须明确地提供给它一个函数指针，新线程将在这个新位置开始执行
    调用成功返回0，失败返回错误代码，手册页有详细说明
    
    void pthread_exit(void *retval);    （pthread.h）
    终止线程执行，如同进程结束时调用exit一样
    作用是终止调用它的线程并返回一个指向某个对象的指针，不能用它来返回一个局部变量的指针，因为线程调用该函数后，局部变量就不再存在了
    retval为向父线程返回的数据指针
    
    int pthread_join(pthread_t th, void **thread_return);
    作用等价于进程中用来收集子进程信息的wait函数
    等到指定的线程终止后才返回
    th指定将要等待的线程，第二个参数是一个指针，它指向另一个指针，而后者指向线程的返回值
    thread_return为指向线程返回值的指针
    成功返回0，失败返回错误代码
    
    调用pthread_exit时传入的retval数据，将在调用pthread_join时返回到thread_return中
    编译时需要定义宏_REENTRANT，在少数系统上，可能还需要定义宏_POSIX_C_SOURCE（一般不需要定义它）
    使用NPTL实现
    编译：
        cc -D_REENTRANT -I/usr/include/nptl xxx.c -o xxx -L/usr/lib/nptl -lpthread
    系统默认使用NPTL线程库，那么编译时就无需-I和-L选项：
        cc -D_REENTRANT xxx.c -o xxx -lpthread

#### 同时执行

    验证两个线程的执行是同时进行的
    线程同步函数
    轮询技术，效率低
    除局部变量外，所有其他变量都将在一个进程中的所有线程之间共享

#### 同步

    控制线程执行和访问代码临界区域的方法
    信号量、互斥量
    
    用信号量进行同步：
        有两组接口函数用于信号量：一组取自POSIX的实时扩展，用于线程；一组称为系统V信号量，常用于进程的同步
        信号量是一个特殊类型的变量，可被增加或减少，对其的关键访问被保证是原子操作，即多个线程改变一个信号量的值时，系统保证所有的操作都将依次进行
        多个线程对普通变量的冲突操作所导致的结果将是不确定的
        二进制信号量，只有0、1两种取值
        计数信号量，有更大的取值范围
        信号量一般常用来保护一段代码，使其每次只能被一个执行线程运行，使用二进制信号量
        允许有限数目的线程执行一段指定的代码，需要使用计数信号量
        计数信号量是二进制信号量的一种逻辑扩展，两者实际调用的函数都一样
        
        int sem_init(sem_t *sem, int pshared, unsigned int value);  （semaphore.h）
        创建信号量，初始化sem指向的信号量对象，设置它的共享选项，并给它一个初始的整数值
        pshared控制信号量的类型，0表示是进程的局部信号量，否则这个信号量可以在多个进程之间共享
    
        int sem_wait(sem_t *sem);
        int sem_post(sem_t *sem);   （semaphore.h）
        控制信号量的值，sem为上面初始化的信号量
        sem_post作用是以原子操作的方式给信号量的值加1
        原子操作是指如果两个线程同时给一个信号量加1，它们之间不会互相干扰，不会引起冲突，信号量的值总是会被正确的加2
        sem_wait以原子操作的方式将信号量的值减1，但它会等待直到信号量有个非0值才会开始减法操作
        对值为0的信号量调用sem_wait，这个函数就会等待，直到有其他线程增加该信号量的值使其不再是0为止
        在单个函数中就能原子化地进行测试和设置
    
        int sem_destroy(sem_t *sem);
        作用是用完信号量后对它进行清理，清理该信号量拥有的所有资源，如果要清理的信号量正被一些线程等待，就会收到一个错误
        成功返回0
        
        sem_wait将信号量减1，如果信号量为0则等待，直到不为0时再减1，即此时信号量为0？
        在多线程程序中，需要对时序考虑得非常仔细
    
    用互斥量进行同步：
        互斥量：另一种在多线程中的同步访问方法，允许锁住某个对象，使得每次只能有一个线程访问它
        控制关键代码的访问，必须在进入这段代码前锁住一个互斥量，然后在完成操作之后解锁它
        
        int pthread_mutex_init(pthread_mutex_t *mutex, const pthread_mutexattr_t *mutexattr);
        int pthread_mutex_lock(pthread_mutex_t *mutex);
        int pthread_mutex_unlock(pthread_mutex_t *mutex);
        int pthread_mutex_destroy(pthread_mutex_t *mutex);      （pthread.h）
        成功返回0，失败返回错误代码，但并不设置errno，必须对函数的返回代码进行检查
        mutexattr属性参数允许设置互斥量的属性，而属性控制着互斥量的行为，属性类型默认为fast
        fast属性的缺点：试图对一个已经加了锁的互斥量调用pthread_mutex_lock，程序就会被阻塞，而拥有互斥量的这个线程正是现在被阻塞的线程，所以互斥量就不会被解锁了，进入死锁状态
        通过改变互斥量属性解决上面问题，可以让它检查这种情况并返回一个错误，或者让它递归的操作，给同一个线程加上多个锁，必须在后面执行同等数量的解锁操作
        属性一般传NULL，使用默认行为，改变属性相关资料参考pthread_mutex_init手册页
    
        pthread_mutex_lock，对互斥量加锁，如果已经被锁住，这个调用将被阻塞直到它被释放为止
        pthread_mutex_unlock，解锁互斥量，如果有另一个地方在申请加锁，则它被通知并执行
        要避免轮询的方式，可用信号量避免轮询

#### 线程的属性

    想要让线程向创建它的线程返回数据，可用使用pthread_join对线程进行同步，接收线程返回的信息
    脱离线程（detached thread）：线程不需要向创建它的线程返回数据，在运行完毕后直接终止
    脱离线程可以通过修改线程属性或调用pthread_detach的方法来创建
    
    int pthread_attr_init(pthread_attr_t *attr);    （pthread.h）
    初始化一个线程属性对象，成功返回0，失败返回错误代码
    pthread_attr_destroy，对属性对象进行清理和回收，被回收的对象就不能被再次使用，除非重新初始化
    
    int pthread_attr_setdetachstate(pthread_attr_t *attr, int detachstate);
    int pthread_attr_getdetachstate(const pthread_attr_t *attr, int *detachstate);
    int pthread_attr_setschedpolicy(pthread_attr_t *attr, int policy);
    int pthread_attr_getschedpolicy(const pthread_attr_t *attr, *int policy);
    int pthread_attr_setschedparam(pthread_attr_t *attr, const struct sched_param *param);
    int pthread_attr_getschedparam(const pthread_attr_t *attr, const struct sched_param *param);
    
    int pthread_attr_setinheritsched(pthread_attr_t *attr, int inherit);
    int pthread_attr_getinheritsched(const pthread_attr_t *attr, int *inherit);
    
    int pthread_attr_setscope(pthread_attr_t *attr, int scope);
    int pthread_attr_getscope(const pthread_attr_t *attr, int *scope);
    int pthread_attr_setstacksize(pthread_attr_t *attr, int scope);
    int pthread_attr_getstacksize(const pthread_attr_t *attr, int *scope);
    更多见pthread.h
    detachedstate，这个属性允许我们无需对线程进行重新合并，PTHREAD_CTEATE_JOINABLE（默认属性，允许两个线程重新合并）和PTHREAD_CTEATE_DETACHED（不能调用pthread_join来获得另一个线程的退出状态）
    schedpolicy，这个属性控制线程的调度方式，取值为SCHED_OTHER（默认属性）、SCHED_RP（使用循环调度机制（round-robin））和SCHED_FIFO（先进先出策略），后两者只能用于以root权限运行的进程，实时调度
    schedparam，这个属性和schedpolicy结合使用，可以对以SCHED_OTHER策略运行的线程的调度进行控制
    inheritsched，可取两个值 PTHREAD_EXPLICIT_SCHED（默认属性，表示调度由属性明确地设置）和PTHREAD_INHERIT_SCHED（新线程将沿用其创建者所使用的参数）
    scope，这个属性控制一个线程调度的计算方式，目前只支持PTHREAD_SCOPE_SYSTEM
    stacksize，这个属性控制线程创建的栈大小，单位为字节，只在定义宏_POSIX_THREAD_ATTR_STACKSIZE的实现版本中才支持，Linux实现线程时默认使用的栈很大
    
    设置脱离状态属性和PTHREAD_CTEATE_DETACHED可以允许线程独立地完成工作，而无需原先的线程等待它
    调度属性的设置：使用sched_get_priority_max和sched_get_priority_min 查找可用的优先级级别

#### 取消一个线程

    线程可以被要求终止时改变其行为
    让一个线程要求另一个线程终止
    int pthread_cancel(pthread_t thread);   （pthread.h）
    请求一个线程终止，发送请求取消它
    
    int pthread_setcancelstate(int state, int *oldstate);
    接收取消请求的一端，用来设置自己的取消状态
    state取值可以是：PTHREAD_CANCEL_ENABLE（允许线程接收取消请求）、PTHREAD_CANCEL_DISABLE（忽略取消请求）
    oldstate指针用于获取先前的取消状态，可传NULL
    
    int pthread_setcanceltype(int type, int *oldtype);
    取消请求被接受时，用它来设置取消类型
    type取值：PTHREAD_CANCEL_ASYNCHRONOUS（使在接收到取消请求后立即采取行动）、PTHREAD_CANCEL_DEFERRED（使在接收到取消请求后一直等待直到线程执行下面函数之一后才采取行动）
    pthread_join、pthread_cond_wait、pthread_cond_timedwait、pthread_testcancel、sem_wait或sigwait
    可能阻塞的系统调用也可以成为取消点
    oldtype保存先前的状态，可以传NULL
    默认情况下，线程在启动时的取消状态为PTHREAD_CANCEL_ENABLE，取消类型为PTHREAD_CANCEL_DEFERRED

#### 多线程

    线程组
    子线程运行完毕，直到父线程调用pthread_join后（此时才收到子线程的返回数据），它才真正结束