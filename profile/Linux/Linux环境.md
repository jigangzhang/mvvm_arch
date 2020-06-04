
## Linux环境

    多用户多任务环境，共享内存、磁盘空间和CPU周期等机器资源
    同一程序会有多个实例同时运行
    这些程序互不干扰、能正确运行、不产生冲突（同时写同一个文件等）
    程序运行的环境

#### 程序参数

    C程序从main函数开始
    int main(int argc, char *argv[]);
    argc程序参数的个数，argv代表参数自身的字符串数组
    main(); 无参
    shell提供参数给main，将shell命令行分解成单词放入argv数组
    参数个数argc 包括程序名本身，argv数组也包含程序名为其第一个元素argv[0]，可使用引号包含空格，如：'a b c'，为一个参数
    X/Open规范，定义了命令行选项的标准用法（工具语法指南），同时定义了在C中提供命令行开关的标准接口：getopt函数
    
    getopt：
        支持需要关联值和不需要关联值的选项
        int getopt(int argc, char *const argv[], const char *optstring);    （unistd.h）
        extern char *optarg;
        extern int optind, opterr, optopt;
        getopt将传递给main函数的argc和argv作为参数，并接受一个选项指定符字符串optstring，告诉getopt哪些选项可用，以及它们是否有关联
        optstirng是一个字符列表，每个字符代表一个单字符选项，如果一个字符后面紧跟一个:，则表明该选项有一个关联值作为下一个参数，bash中的getopts命令执行类似的功能
        
        getopt(argc, argv, "if:lr"); 允许选项 -i -f -l -r，-f选项后要紧跟一个文件名参数
        使用相同的参数，但以不同的顺序来调用命令将改变程序的行为
        getopt返回值是argv数组中的下一个选项字符，循环调用getopt就可以一次得到每个选项
        getopt的行为：
            如果选项有一个关联值，则外部变量optarg指向这个值
            如果选项处理完毕，getopt返回-1，特殊参数--将使getopt停止扫描选项
            如果一个选项无法识别，则返回?，并把它保存到外部变量optopt中
            如果一个选项要求有一个关联值（如-f），但用户未提供，通常将返回一个?
            如果选项字符串的第一个字符设置为:，那么在用户未提供值的情况下返回:，而不是?
        外部变量optind被设置为下一个待处理参数的索引，getopt利用它记录自己的进度
        GNU版本，getopt是否会在第一个非选项参数处停下来，由环境变量POSIXLY_CORRECT控制，被设置就会停下
        POSIX规范，opterr变量是非0值，getopt就会向stderr打印出错信息
    
    getopt_long：
        GNU C函数库包含的getopt另一个版本，接受以--开始的长参数
        有关联值的长选项可以按格式--option=value作为单个参数给出
        ./exec --initialize --list 'arg ment a' --file fred.c -q
        ./exec --init -l 'arg ment a' --file=fred.c -q
        长选项可以缩写
        int getopt_long(int argc, char *const argv[], const char *optstring, struct option options[], int *arg);    
        options是一个结构数组，描述了每个长选项并告诉getopt_long如何处理它们
        arg，变量指针，可以作为optind的长选项版本使用，对每个识别的长选项，它在长选项数组中的索引就写入该变量
        长选项数组类型为 struct option，该数组必须以一个包含全0的结构结尾
        长选项结构在getopt.h中定义，该头文件必须与常量_GNU_SOURCE一同包含，该常量启用getopt_long功能
        struct option {
            const char *name;   //长选项名字，缩写也可以接受，不能与其他选项混淆
            int has_arg;        //该选项是否带参数，0 不带参数，1必须有一个参数，2有一个可选参数
            int *flag;          //为NULL表示找到该选项时，getopt_long返回在成员val里给出的值，否则返回0，并将val的值写入flag指向的变量        
            int val;            //getopt_long为该选项返回的值
        }
        其他参考getopt的手册页

#### 环境变量

    能用来控制shell脚本和其他程序行为的变量
    可以用它们配置用户环境
    echo $HOME，用户环境
    UNIX规范为各种应用定义了标准环境变量：终端类型、默认的编辑器、时区等
    
    char *getenv(const char *name); （stdlib.h）访问环境变量
    int putenv(const char *string);     设置环境变量
    getenv以给定的名字搜索环境中的一个字符串，返回与该名字相关的值，变量不存在返回null，变量存在但无关联值，返回空字符串
    getenv返回的字符串是存储在getenv提供的静态空间中，必须将它复制到另一个字符串中，以免被后续getenv调用覆盖
    putenv以 name=value 格式的字符串为参数，将该字符串加到当前环境中，失败返回-1，错误变量errno为ENOMEM表示可以内存不足而不能扩展环境
    环境仅对程序本身有效，在程序里做的改变不会反映到外部环境中，因为变量的值不会从子进程传播到父进程（shell）
    
    环境变量的用途：
        设置环境变量的方式有：
            在默认环境中设置；通过登录shell读取的.profile文件来设置；
            使用shell专用的启动文件（rc）；在shell命令行上对变量进行设置（JAVA=/java/xxx，临时改变）
        export xx
        通过改变环境变量，来指定所有的数据库等（CDDB=xxx）
    
    environ变量：
        程序可以通过environ变量直接访问这个字符串数组
        extern char **environ;  （stdlib.h）以null结尾的字符串数组

#### 时间和日期

    定时备份、定时拒绝执行、记录运行时间、改变运行方式等
    格林尼治时间（GMT）1970年1月1日0点，UNIX纪元的起点；MS-DOS纪元始于1980年，其他系统使用其他的纪元起始时间
    32位时间将在2038年回绕，使用大于32位的time_t类型
    time_t time(time_t *tloc);  （time.h）
    返回底层的时间值，秒数，tloc不是空指针的话，time会把返回值写入tloc指针指向的位置
    
    double difftime(time_t time1, time_t time2);  （time.h）
    计算两个时间值之间的差，以浮点数返回
    
    struct tm *gmtime(const time_t *timeval);
    把底层时间值分解为一个结构，该结构包含（tm结构体）：
        int tm_sec，秒 0~61
        int tm_min，分 0~59
        int tm_hour，小时 0~23
        int tm_mday，月份中的日期 1~31
        int tm_mon，月份 0~11（一月份为0）
        int tm_year，年，从1900年开始计算
        int tm_wday，星期几，0~6（周日为0）
        int tm_yday，年份中的日期，0~365
        int tm_isdst，是否夏令时
    tm_sec的范围允许临时闰秒或双闰秒
    gmtime按GMT返回时间（GMT为世界标准时间，或UTC），在GMT之外的时区返回的时间是不对的
    gmtime作用是同步全球各地的所有系统，使不同时区同一时刻创建文件有相同的创建时间
    
    struct tm *localtime(const time_t *timeval);    （time.h）
    返回的结构中包括的值已根据当地时区和是否采用夏令时做了调整，返回本地时间
    
    time_t mktime(struct tm *timeptr);
    把已分解的tm结构体转换为原始的time_t时间值，不能转换返回-1
    
    char *asctime(const struct tm *timeptr);    
    char *ctime(const time_t *timeval);
    asctime返回字符串，表示tm结构给出的时间和日期，格式：Sun Jun 9 12:34:45 2020\n\0，26个字符的固定格式
    ctime等效于 asctime(localtime(timeval))，转换为更易读的本地时间
    
    size_t strftime(char *s, size_t maxsize, const char *format, const struct tm *timeptr);
    对日期和时间字符串的格式，类似sprintf，格式化timeptr，将结果放在字符串s中（maxsize字符长）
    转换控制符：
        %a，星期几的缩写   %A，星期几的全称
        %b，月份的缩写    %B，月份的全称
        %c，日期和时间    %d，月份中的日期，1~31
        %H，小时，00~23    %I，12小时制的小时，01~12
        %j，年份中的日期，001~366    %m，年份中的月份，01~12
        %M，分钟，00~59    %p，a.m. 或 p.m.（下午）
        %S，秒，00~61      %u，星期几，1~7（周一为1）
        %U，一年中的第几周，01~53（周日为第一天）
        %V，一年中的第几周，01~53（周一为第一天）
        %w，星期几，0~6（周日为0）
        %x，本地格式的日期      %X，本地格式的日期
        %y，年份减去1900        %Y，年份
        %Z，时区名              %%，字符%
    date命令输出的普通日期相当于strftime： %a %b %d %H:%M:%S %Y
    
    char *strptime(const char *buf, const char *format, struct tm *timeptr);
    以一个代表日期和时间的字符串为参数buf，创建表示同一日期和时间的tm结构
    format构建方式同strftime，类似sscanf函数，查找可识别字段，写入对应的变量中
    星期几和月份缩写和全称都行，都匹配%a，strftime对小于10的数字总以0开头
    strptime返回指向转换过程处理的最后一个字符后面的那个字符，不能转换的字符就在该出停下
    %R，是strptime中对%H:%M的缩写
    
    GNU库默认未声明strptime函数，需要明确请求使用X/Open标准功能，需要在time.h前加上 #define _XOPEN_SOURCE

#### 临时文件

    用处如：数据库应用删除记录时使用临时文件，该文件收集需要保留的数据库条目，在处理结束后，这个临时文件就变成新的数据库，原来文件则被删除
    char *tmpnam(char *s);  （stdio.h）
    生成一个唯一的文件名
    返回一个不与任何已存在文件同名的有效文件名，s不为空，文件名也会写入s
    对tmpnam的后续调用会覆盖存放返回值的静态存储区，如果tmpnam被多次调用，就要给它传递一个字符串参数
    s长度至少要有L_tmpnam个字符（一般20），tmpnam可最多被调用TMP_MAX次（至少几千次），每次返回一个不同文件名
    
    FILE *tmpfile(void);    （stdio.h）
    临时文件给它命名的同时打开它，另一个程序可能会创建出与tmpnam返回的文件名同名的文件，tmpfile可避免
    返回一个文件流指针，指向一个唯一的临时文件，以读写方式打开（w+的fopen），对它的所有引用全部关闭时，该文件会被自动删除，出错返回空指针并设置errno
    
    char *mktemp(char *template);
    int mkstemp(char *template);
    与tmpnam类似，不同之处在于可以为临时文件名指定一个模板，模板可以让你对文件的存放位置和名字有更多的控制
    mktemp以给定的模板为基础创建一个唯一的文件名
    template必须是一个以6个x字符结尾的字符串，mktemp用有效文件名字符的一个唯一组合来替换这些x字符
    mktemp返回指向生成字符串的指针，不能生成返回空指针
    mkstemp类似于tmpflie，同时创建并打开一个临时文件，文件名的生成方法和mktemp一样，返回一个打开的、底层文件描述符
    
    应该使用 tmpfile 和 mkstemp，不要使用tmpnam和mktemp

#### 用户信息

    除init程序以外，所有的Linux程序都是由其他程序或用户启动的
    Linux运行的每个程序实际上都以某个用户的名义在运行，都有一个关联的UID
    当一个程序的SUID位被置位时，它的运行就好像是由该可执行文件的属主启动的
    执行su命令时，程序的运行就好像是由root启动的，它验证访问权限，将UID改为目标账户的UID值并执行该账户的登录shell
    su还可以允许一个程序的运行就好像是由另一个用户启动的
    UID类型是-uid_t，通常是一个小整数，一般情况下，用户的UID大于100，小于100的是系统预定义的
    
    uid_t getuid(void);     （sys/types.h，unistd.h）
    uid_t getgid(void);
    返回程序关联的UID、GID，通常是启动程序的用户UID
    char *getlogin(void);
    返回与当前用户关联的登录名
    
    /etc/passwd包含一个用户账号数据库，每行对应一个用户（包含）：
        用户名、加密口令、用户标识符（UID）、组标识符（GID）、全名、家目录和默认shell
    /etc/shadow密码文件，保存加密口令，passwd不再包含任何加密口令信息
    struct passwd *getpwuid(uid_t uid);     （sys/types.h，pwd.h）
    struct passwd *getpwnam(const char *name);
    passwd结构体（pwd.h）：
        char *pw_name， 用户登录名
        uid_t *pw_uid， UID号
        gid_t *pw_gid， GID号    
        char *pw_dir， 用户家目录
        char *pw_gecos/pw_comment， 用户全名
        char *pw_shell， 用户默认shell
    返回指向与某个用户对应的passwd结构，通过UID或用户登录名参数来确定，出错返回空指针并设置errno
    
    void endpwent(void);    （pwd.h，sys/types.h）
    struct passwd *getpwent(void);
    void setpwent(void);
    getpwent，依次返回每个用户的信息数据项，到达文件尾时，返回一个空指针
    endpwent，终止getpwent的处理过程
    setpwent，重置读指针到密码文件的开始位置，这样下一个getpwent调用将重新开始一个新的扫描
    
    uid_t geteuid(void);    （sys/types.h，unistd.h）
    gid_t getgid(void);
    gid_t getegid(void);
    int setuid(uid_t uid);  //只有超级用户才能调用
    int setgid(gid_t gid);  //只有超级用户才能调用

#### 主机信息

    int gethostname(char *name, size_t namelen);    （unistd.h）
    获取网络名（系统安装网络组件才行），把机器的网络名写入name字符串，该字符串至少有namelen长，成功返回0，否则-1
    
    int uname(struct utsname *name);    （sys/utsname.h）
    把主机信息写入name指向的结构，utsname定义在sys/utsname.h中：
        char sysname[]，操作系统名
        char nodename[]，主机名
        char release[]，系统发行级别
        char version[]，系统版本号
        char machine[]，硬件类型
    成功返回一个非负整数，失败返回-1并设置errno
    
    long gethostid(void);   （unistd.h）  //主机的唯一标识符
    返回与主机对应的一个唯一值，可用它确保程序只能在拥有合法许可证的机器上运行（Linux返回一个基于该机器因特网地址的值）

#### 日志

    su程序会把某个用户尝试得到超级用户权限但失败的事实记录下来
    通常这些日志信息被记录在系统文件中，这些系统文件被保存在专用于此目的的目录中
    /usr/adm 或 /var/log 保存系统日志
    对于Linux，如：
        /var/log/messages，包含所有系统信息
        /var/log/mail，    包含来自邮件系统的其他日志信息
        /var/log/debug，   包含调试信息
    查看 /etc/syslog.conf（rsyslog.conf） 或 /etc/syslog-ng/sys-log-ng.conf 来检查系统配置
    
    UNIX规范通过syslog函数为所有程序产生日志信息提供一个接口：
        void syslog(int priority, const char *message, arguments...);   （syslog.h）
        向系统的日志设施（facility）发送一条日志信息，每条信息都有一个priority参数，该参数是一个严重级别与一个设施值的按位或
        严重级别控制日志信息的处理方式，设施值记录日志信息的来源
        设施值包括：
            LOG_USER（默认值），指出消息来自一个用户应用程序
            LOG_LOCAL0 ~ LOG_LOCAL7，它们的含义由本地管理员指定
        严重级别（按优先级递减）：
            LOG_EMERG，      紧急情况
            LOG_ALERT，      高优先级故障，如数据库奔溃
            LOG_CRIT，       严重错误，如硬件故障
            LOG_ERR，        错误
            LOG_WARNING，    警告
            LOG_NOTICE，     需要注意的特殊情况
            LOG_INFO，       一般信息
            LOG_DEBUG，      调试信息
        根据系统配置，LOG_EMERG信息可能会广播给所用用户，LOG_ALERT可能会EMAIL给管理员，LOG_DEBUG可能被忽略，其他信息写入日志文件
    syslog创建的日志信息包含消息头和消息体，消息头根据设施值及日期和时间创建，消息体根据message参数创建，参数类似printf中的格式字符串，%m可用于插入与errno当前值对应的出错消息字符串
    日志格式： 时间  全名？  程序名: message
    
    void closelog(void);
    void openlog(const char *ident, int logopt, int facility);
    void setlogmask(int maskpri);
    openlog：
        改变日志信息的表示方式
        ident字符串会添加在日志信息的前面，可以通过它指明哪个程序创建了这条信息
        facility参数记录一个将被用于后续syslog调用的默认设施值，其默认值是LOG_USER
        logopt参数：
            对后续syslog调用的行为进行配置，是0或多个参数的按位或
            LOG_PID，在日志信息中包含进程标识符，是系统分配给进程的唯一值
            lOG_CONS，如果信息不能被记录到日志文件，就把它们发送到控制台
            lOG_ODELAY，第一次调用syslog时才打开日志设施
            lOG_NDELAY，立即打开日志设施，而不是第一次记录时
        会分配并打开一个文件描述符，通过它来写日志，可以调用closelog来关闭它
        调用syslog前无需调用openlog，因为syslog会根据需要自行打开日志设施
    setlogmask，设置一个日志掩码，通过它控制日志信息的优先级，优先级未在日志掩码中置位的后续syslog调用都将被丢弃（可以通过它关闭LOG_DEBUG等消息）
    LOG_MASK(priority)，为日志信息创建一个掩码，创建一个只包含一个优先级的掩码
    LOG_UPTO(priority)，创建一个由指定优先级之上的所有优先级（包含）构成的掩码
    调试信息的启用，查看系统中针对syslog或syslog-ng的文档
    
    pid_t getpid(void);     （sys/types.h，unistd.h）
    pid_t getppid(void);
    分别返回调用进程和调用进程的父进程的进程标识符（PID）

#### 资源和限制

    硬件方面的物理性限制（内存）、系统策略的限制（允许使用的CPU时间）、具体实现的限制（整数的长度或文件名的最大字符数等）
    UNIX规范定义了一些可由应用程序决定的限制
    limits.h定义了许多代表操作系统方面限制的显式常量：
        MANE_MAX，文件名中的最大字符数（特定于文件系统），pathconf函数？？
        CHAR_BIT，char类型值的位数
        CHAR_MAX，char类型的最大值
        INT_MAX， int类型的最大值
    
    sys/resource.h提供了资源操作方面的定义，包括对程序长度、执行优先级和文件资源等方面限制进行查询和设置的函数：
        int getpriority(int which, id_t who);
        int setpriority(int which, id_t who, int priority);
        int getrlimit(int resource, struct rlimit *r_limit);
        int setrlimit(int resource, const struct rlimit *r_limit);
        int getrusage(int who, struct rusage *r_usage);
        id_t，（整数类型）用于用户和组标识符
        rusage结构体，用来确定当前程序已耗费CPU时间：
            struct timeval ru_utime，使用的用户时间
            struct timeval ru_stime，使用的系统时间
        timeval结构体（sys/time.h），包含tv_sec和tv_usec（秒和微妙）
        程序耗费的时间分为用户时间（程序执行自身的指令所耗费的时间）和系统时间（操作系统为程序执行所耗费的时间，即执行输入输出操作的系统调用或其他系统函数所花费的时间）
        
        getrusage将CPU时间信息写入参数r_usage，参数who如下选项：
            RUSAGE_SELF，    仅返回当前程序的使用信息
            RUSAGE_CHILDREN，还包括子进程的使用信息
        每个运行的程序都有一个与之关联的优先级，优先级越高的程序将分配到更多的CPU可用时间
        普通用户只能降低其程序的优先级，而不能升高
        getpriority和setpriority确定和更改程序的优先级（自己和其他程序）
        被优先级函数检查或更改的进程可以用进程标识符、组标识符或用户来确定
        which指定了对待who参数的方式（which取值）：
            PRIO_PROCESS，who参数是进程标识符
            PRIO_PGRP，   who参数是进程组
            PRIO_USER，   who参数是用户标识符
        priority = getpriority(PRIO_PROCESS, getpid()); //确定当前进程的优先级
    
        默认优先级是0，正数优先级用于后台任务，只在没有其他更高优先级的任务准备运行时才执行
        负数优先级使一个程序运行更频繁，获得更多的CPU可用时间
        优先级的有效范围是 -20~+20，数值越高，执行优先级反而越低
        getpriority成功时返回有效的优先级，失败返回-1并设置errno，-1本身是一个优先级，应在调用getpriority前将errno设置为0，在函数返回时检查errno是否仍为0
        setpriority成功返回0，否则返回-1
        
        getrlimit、setrlimit读取和设置系统资源方面的限制
        rlimit结构体描述资源限制（sys/resource.h）：
            rlim_t rlim_cur，当前的软限制
            rlim_t rlim_max，硬限制
        rlim_t，整数类型，描述资源级别
        软限制是一个建议性的最后不要超越的限制，超越可能会导致库函数返回错误
        硬限制如果被超越，可能会导致系统通过发送信号的方式来终止程序的运行
        CPU时间限制被超越系统会发送SIGXCPU信号，数据长度限制被超越系统会发送SIGSEGV信号
        程序可以把软限制设置为小于硬限制的任何值，可以减小自己的硬限制，超级用户权限才能增加硬限制
        resource参数指定那些系统资源进行限制（在sys/resource.h中定义）：
            RLIMIT_CORE， 内核转储（core dump）文件的大小限制（字节）
            RLIMIT_CPU，  CPU时间限制（秒为单位）
            RLIMIT_DATA， 数据段限制（字节为单位）
            RLIMIT_FSIZE，文件大小限制（字节为单位），-1 未限制
            RLIMIT_NOFILE，可以打开的文件数限制
            RLIMIT_STACK，栈大小限制（字节）
            RLIMIT_AS，   地址空间（栈和数据）限制（字节）
    
    当资源限制被超越时，一些系统（如Linux2.2及后续版本）会通过发送信号SIGXFSZ的方式终止程序
    其他一些POSIX兼容的系统可能只是让资源限制被超越的函数返回一个错误