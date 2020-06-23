
## MySQL

[MySQL手册](https://dev.mysql.com/doc/refman/8.0/en/)

    两个最著名的开源RDBMS应用软件：PostgreSQL、MySQL
    RDBMS（关系型数据库管理系统）：Relational Database Management System
    其他商业产品：Oracle、Sybase、DB2、SQL Server（WIndows）
    MySQL在某些场景下可能需要购买商业许可证
    MySQL手册文档地址：https://dev.mysql.com/doc/refman/8.0/en/

#### 安装

    rpm -qa | grep mysql，查看是否有安装mysql
    whereis mysql，查询mysql对应的文件夹
    find / -name mysql
    
    wget -i -c https://repo.mysql.com/mysql80-community-release-el8-1.noarch.rpm：
        下载最新的rpm包
    yum install -y mysql80-community-release-el8-1.noarch.rpm：
        安装MySQL源
        执行成功后会在/etc/yum.repos.d/目录下生成两个repo文件mysql-community.repo及 mysql-community-source.repo
    yum repolist enabled | grep "mysql.*-community.*"：
        查看mysql相关资源，当前启用的Mysql仓库  
    yum repolist all | grep mysql：
        查看当前MySQL Yum Repository中所有MySQL版本（每个版本在不同的子仓库中）
    yum-config-manager --disable mysql80-community
    sudo yum-config-manager --enable mysql57-community    
        切换版本
        除了使用yum-config-manager之外，还可以直接编辑/etc/yum.repos.d/mysql-community.repo文件，enabled=0禁用，enabled=1启用
    yum -y install mysql-community-server（centos7 及其他使用）：
    yum install mysql-server（centos8 使用）
        安装mysql服务
        
    systemctl list-unit-files|grep mysqld：
        检查是否已经设置为开机启动MySQL服务    
    systemctl enable --now mysqld：
        启动MySQL服务并使它在启动时自动启动
    systemctl status mysqld：
        检查MySQL服务器是否正在运行，及其他状态
    ps -ef|grep mysql：    
        查看是否启动MySQL服务
    systemctl start mysqld.service；
        启动服务
    sudo systemctl restart mysqld：
        重启服务    
    sudo mysql_secure_installation：
        该脚本执行一些与安全性相关的操作并设置MySQL根密码
        按照脚本指示进行
    mysql -uroot -p：登录进入mysql
    
    use mysql;
    update user set host='%' where user='root';
    flush privileges;
    将用户root设置为任意IP可登录

    sudo firewall-cmd --add-port=3306/tcp --permanent
    sudo firewall-cmd --reload
    开启系统防火墙的3306端口
    
    /etc/my.cnf文件，添加以下配置：
        [mysqld]
        skip-name-resolve
    关闭MySQL主机查询dns，MySQL会反向解析远程连接地址的dns记录，如果MySQL主机无法连接外网，则dns可能无法解析成功，导致第一次连接MySQL速度很慢
    
    配置文件：/etc/my.cnf
    日志文件：/var/log/mysql/mysqld.log

#### 安装后的配置

    mysql -u root -p mysql，登录进入mysql，打印欢迎信息等
    mysqladmin -u root -p version，查看正在运行的服务器状态，版本信息等
    mysqladmin -u root -p variables，检查正在运行的服务器中的所有配置选项
        变量，datadir MySQL存储数据的路径，have_innodb 是否支持InnoDB存储引擎
    存储引擎，用于数据存储的底层实现程序，最常见的有InnoDB和MyISAM，其他 memory引擎不使用永久存储
    CSV引擎使用逗号分隔的变量文件
    最新版本默认就是InnoDB，（default-storage-engine=INNODB）
    一些变量可通过编辑 my.cnf配置文件，在mysqld 一节下配置
    如：[mysqld]
        default-storage-engine=INNODB
        innodb_data_home_dir=/home/mysql/data
        innodb_data_file_path=ibdata1:10M:autoextend
        innodb_log_group_home_dir=/home/mysql/logs
    改变数据文件存放地址，日志文件存放地址，设置数据文件的初始大小为10M，允许它自扩充
    更多可在 mysql官网的在线手册查看
    
    mysqladmin -u root password newpassword，设定初始密码
    SET password=PASSWORD('xxxx');  使用sql设置密码
    use mysql   切换数据库
    select user, host from user;    查看用户权限表
    grant all on *.* to user@localhost identified by 'xxx';   //为user创建一个本地登录
    grant all on *.* to user@'192.168.0.0/255.255.255.0' identified by 'xxx';   //为user创建一个C类子网的登录，掩码确定允许的IP地址范围
    grant all on *.* to user@'%.god.com' identified by 'xxx';   //为user创建一个能从域名下的任何机器登录
    创建用户使用create user，grant用于授权
    create user user@'%.god.com' identified by 'xxxx';

#### 安装后的故障修复

    查看服务是否启动：使用ps 或 systemctl
    查看日志
    服务在运行但无法连接：
        检查数据库是否存在，查看数据库目录（centos是/var/lib/mysql）下默认的权限数据库是否存在
        检查MySQL启动脚本（在/etc/init.d目录下）和配置文件my.cnf找到数据库目录位置
        在目录下查看是否有名为mysql的数据库

#### MySQL管理

    show、describe（查看表结构）、explain
    命令：
        除mysqlshow命令外，其他所有MySQL命令都可以使用 -u -p -h 这3个标准参数
        -u，用户名，默认情况下（不使用-u），mysql工具会把当前Linux用户名作为MySQL用户名，-u指定不同的用户名
        -p，密码，使用-p 系统会提示输入密码，不使用-p，MySQL命令将假设不需要密码
        -h，主机名，用于连接位于不同主机上的MySQL服务器
        密码最后不要跟在 -p 后面，因为可以被ps命令或历史查看到
        
        myisamchk命令：
            检查和修复使用默认MYISAM表格式的任何数据表，MYISAM表格式由MySQL自身支持
            通常，myisamchk应该以安装时创建的mysql用户身份来运行，且在数据表所处的目录中运行
            首先，su mysql，然后进入数据库名称对应的目录，在运行命令
            myisamchk -e -r *.MYI，  -c 检查表以发现错误，-e 执行扩展检查， -r 修复发现的错误
            对InnoDB类型的数据表没有效果
        
        mysql命令：
            mysql -uroot -p db_name，登录进入控制台，并默认进入数据库db_name
            use db_name，切换数据库
            mysql -u root --password=xxx db_name < sqlcommands.sql，
                捆绑命令到一个输入文件并从命令行读取，必须在命令行上指定密码
            支持标准的SQL92命令集
            特定的命令：
                edit or \e，编辑命令，进入编辑器，编辑器由环境变量$EDITOR决定
                exit or quit or \q，退出MySQL客户端
                go or \g，执行命令
                source <filename> or \.，从指定文件执行SQL
                status or \s，显示服务器状态信息
                system <command> or \！，执行一个系统命令
                tee <filename> or \T，把所有输出的副本添加到指定文件中
                use <database> or \u，使用指定的数据库
            每个数据库都是一个基本独立的表格集，可以建立不同的数据库，指定不同的用户
            数据库mysql由安装时自动创建，用于保存用户和权限这样的数据
        
        mysqladmin：
            快速进行MySQL数据库管理的主要工具
            create <db_name>，创建一个新数据库
            drop <db_name>，删除一个新数据库
            password <new_password>，修改密码
            ping，检查服务器是否正在运行
            reload，重载控制权限的grant表
            status，提供服务器的状态
            shutdown，停止服务器
            variables，显示控制MySQL操作的变量及其当前值
            version，提供服务器版本号以及它持续运行的时间
        mysqladmin -u name -p status，使用方式
        mysqladmin | less，查看完整选项列表
        
        mysqlbug：
            用于生成一个发送给MySQL维护者的错误报告
    
        mysqldump：
            以SQL命令集的形式将部分或整个数据库导出到一个单独文件中，该文件能被重新导入MySQL或其他的SQL RDBMS
            接受标准用户和密码信息作为参数，也接受数据库名和表名作为参数
            --add-drop-table，添加SQL命令到输出文件，以在创建表的命令之前丢弃任何表
            -e，使用扩展的insert语法，加快转储数据的加载速度
            -t，只转储表中的数据，而不是用来创建表的信息
            -d，只转储表结构，而不是实际数据
        默认将数据发送到标准输出，可重定向到文件
        用于迁移数据或快速备份，远程备份
        
        mysqlimport：
            用于批量将数据导入到一个表中
            从一个输入文件中读取大量的文件数据
            只有唯一的参数是一个文件名和一个数据库名
            将把数据导入到数据库中与文件名（不包括任何文件扩展名）相同的表中
            必须确认文本文件与将要填入数据的表拥有相同的列数，并且数据类型是兼容的，默认应以tab分割符分开
            
        通过文本文件执行SQL命令，只需运行mysql命令，并将输入重定向到一个文件即可
        
        mysqlshow：
            快速了解MySQL安装及其组成数据库的信息
            无参数，列出所有可用的数据库
            以一个数据库为参数，列出该数据库中的表
            以数据库和表名为参数，列出表中的列
            以数据库、表和列为参数，列出指定列的详细信息
    
    创建用户、赋予权限：
        grant命令：
            grant <privilege> on <object> to <user> [identified by user-password] [with grant option];
            可授予的特权值：
                alter， 改变表和索引
                create， 创建数据库和表
                delete， 从数据库中删除数据
                drop， 删除数据库和表
                index， 管理索引
                insert， 在数据库中添加数据
                lock tables， 允许锁定表
                select， 提取数据
                update， 修改数据
                all，    以上所有
            一些命令的其他选项：
                create view，授予用户创建视图的权限
            权限列表查阅MySQL版本文档
            授予权限的对象（Object）：
                dbname.tablename
                *.*，通配符，代表每个数据库中的每个对象
                foo.*，代表数据库foo中的每个表
            指定用户不存在，就会以指定的权限被创建，应该指定用户和主机
            %，代表通配符，与*作用完全一样，如：user@'%'
            限制IP：user@'192.168.1.1'，限制在特定1台工作站，user@'192.168.0.0/255.255.255.0'，扩大范围至192网络中的所有机器
            grant all on db.* to user@'%' identified by 'xxx';
            下划线，是一种匹配任意单个字符的模式，小心使用
            with grant option，用于创建二级管理员，允许一个新创建的用户将授予他的权限赠予其他用户
        
        revoke命令：
            剥夺用户权限
            revoke <a_privilege> on <an_object> from <a_user>;
            revoke INSERT on foo.* from user@'%';
            不能删除用户
        删除用户：
            use mysql
            delete from user where user = "name"
            flush privileges;   //重载权限表
            
    密码：
        以root连接到MySQL服务器
        user mysql
        select host, user, password from user;  //最新版本已不存在password字段
        update user set password = password('xxx') where user = 'name'; //新版本不存在password字段，故修改失败
    
    创建数据库：
        grant all on *.* to user@'%' identified by 'xxx';   //授予权限
        create database db_name;
        use db_name;
        
    数据类型：
        布尔类型，BOOL，有TRUE、FALSE值，也可以持有NULL值（未知）
        字符类型：
            前三个是标准的，后三个是MySQL特有的
            CHAR， 单字符
            CHAR(N)， 正好有N个字符的字符串，如果必要会以空格字符填充，限制为255个字符
            VARCHAR(N)， N个字符的可变长数组，限制为255个字符
            TINYTEXT， 类似于VARCHAR(N)
            MEDIUMTEXT， 最长为65535个字符的文本字符串
            LONGTEXT， 最长为2^32-1个字符的文本字符串
        数值类型：    
            分为整型和浮点型
            TINYINT， 整型，8位数据类型
            SMALLINT， 整型，16位数据类型
            MEDIUMINT， 整型，24位数据类型
            INT， 整型，32位数据类型，标准类型
            BIGINT， 整型，64位有符号数据类型        
            FLOAT(P)， 浮点型，精度至少为P位数字的浮点数
            DOUBLE(D,N)， 浮点型，有符号双精度浮点数，有D为数字和N位小数
            NUMERIC(P,S)， 浮点型，总长为P位的真实数字，小数点后有S位数字，不double不同，这是一个准确的数，适合用来储存货币值，但处理效率会低一点
            DECIMAL(P,S)， 浮点型，与numeric同义
            一般情况下，建议使用int、double、numeric，它们最接近于标准SQL类型，其他类型都是非标准的
        时间类型：
            DATE， 存储从1000年1月1日~9999年12月31日之间的日期
            TIME， 存储从-838:59:59~838:59:59之间的时间
            TIMESTAMP，存储从1970年1月1日~2037年之间的时间戳
            DATETIME， 存储从1000年1月1日~9999年12月31日最后一秒之间的日期
            YEAR， 存储年份，两位数的年份值将被自动转换为四位数的年份
    
    创建表：
        一个数据库可以包含的表格数是不受限制的
        创建数据库对象的完整SQL语法被称为DDL（data definition language，数据定义语言）
        create table <table_name> {
        column type [NULL | NOT NULL] [AUTO_INCREMENT] [PRIMARY KEY]
        [, ...]
        [, PRIMARY KEY (column [, ...])]
        }
        drop table <table_name>， 删除表
        关键字：
            AUTO_INCREMENT，当在该列中写入NULL值时，它都会自动把一个自动分配的递增数字填入列数据中，
                    它可以通过MySQL来自动为表中的行分配一个唯一的数字，只能用于属于主键的列，其他数据库中，由一个序列值来管理
            NULL， 表示 未知的、无关的
            NOT NULL， 表示不能存储NULL值
            PRIMARY KEY，指出此列的数据必须是唯一的，该表每行中对应该列的值都应不同，每个表只能有一个主键
        create table children {
            no integer auto_increment not null primary key,
            name varchar(30),
            age integer,
            //primary key(no)       另一种主键定义
        };
        insert into children(name, age) values("jack", 18);
        select * from children;
    
    图形化工具：
        MySQL管理器（MySQL Administrator）
        MySQL查询浏览器（MySQL Query Browser）
        Red Hat中对应的软件包是 mysql-gui-tools和mysql-administrator

#### 使用C语言访问MySQL数据

    连接：
        1、初始化一个连接句柄；2、实际进行连接
        MYSQL *mysql_init(MYSQL *);     （mysql.h）
        传NULL返回一个指向新分配的连接句柄结构的指针，传递一个已有结构，它将被重新初始化，出错返回NULL
        
        MYSQL *mysql_real_connect(MYSQL *connection, const char *server_host, 
            const char *sql_user_name, const char *sql_password, const char *db_name,
            unsigned int port_number, const char *unix_socket_name, unsigned int flags);
        connection必须指向已被mysql_init初始化过的结构，server_host即可以是主机名，也可以是IP地址，本机可指定localhost来优化连接类型
        user_name登录名为NULL，则假设登录名为当前Linux用户的登录ID，密码为NULL，只能访问无需密码的数据
        port_number为0，socket_name为NULL，除非改变了MYSQL安装的默认设置（默认使用合适的值）
        flags对一些定义的位模式进行OR操作，使得改变使用协议的某些特性，参考使用手册
        
        void mysql_close(MYSQL *connection);
        关闭连接，如果连接是由mysql_init建立，MYSQL结构会被释放，指针失效无法再次使用
        
        int mysql_options(MYSQL *connection, enum option_to_set, const char *argument);
        仅能在mysql_init和mysql_real_connect之间调用，可以设置一些选项，一次只能设置一个选项
        最常用的选项：
            enum                        实际参数类型
            MYSQL_OPT_CONNECT_TIMEOUT   const unsigned int *    ，连接超时之前的等待秒数
            MYSQL_OPT_COMPRESS          None，使用NULL           ，网络连接中使用压缩机制
            MYSQL_INIT_COMMAND          const char *            ，每次连接建立后发送的命令
        调用成功返回0
    编译、链接：
        gcc -I/usr/include/mysql xxx.c -L/usr/lib64/mysql -lmysqlclient -o xxx
        -I，指定mysql.h头文件所在路径
        -L，指定头文件实现库所在路径（/usr/lib，或/usr/lib64）
        -l，指定需要使用的库（头文件的实现库）
    
    错误处理：
        unsigned int mysql_errno(MYSQL *connection);
        char *mysql_error(MYSQL *connection);
        调用mysql_errno，传递MYSQL结构，返回错误码，通常都是非0值，未设定错误码，将返回0
        每次调用库都会更新错误码，只能得到最后一个执行命令的错误码，上面两个函数不会导致错误码更新
        mysql_errno返回值就是错误码，在头文件errmsg.h或mysqld_error.h中定义（在Include目录），前者报告客户端错误，后者关注服务端错误
        
        mysql_error返回文本错误信息，这些信息被写入一些内部静态内存空间，需要复制使用
        
    执行SQL语句：
        int mysql_query(MYSQL *connection, const char *query);
        query为SQL语句（不带分号），成功返回0
        mysql_real_query，用于包含二进制数据的查询
        
        不返回数据的SQL语句：
            UPDATE、DELETE和INSERT
            my_ulonglong mysql_affected_rows(MYSQL *connection);
            用于检查受查询影响的行数，返回受之前执行的update、insert或delete查询影响的行数（返回值使用%lu打印）
            对于mysql_系列函数，返回值0表示没有行受到影响，正数是实际的结果，一般表示受影响的行数
            受影响的行为实际发生改变的行数，而不是where匹配的行数（传统使用where匹配的行数）
            可以使用mysql_real_connect的CLIENT_FOUND_ROWS（flags）标志获得传统的报告
            
        发现插入的内容：
            ID自增长，插入数据后如何知道刚插入的数据ID？
            LAST_INSERT_ID()，MYSQL提供的函数
            无论何时向auto_increment列插入值，MYSQL都会基于每个用户对最后分配的值进行跟踪
            用户程序可以通过专用函数LAST_INSERT_ID()发现该值，这个函数的作用有点像是表中的虚拟列
            select LAST_INSERT_ID();
            每次插入一行，MYSQL就分配一个新的id值并且跟踪它，使得可以用LAST_INSERT_ID()来提取它
            在本次会话中是唯一的
            插入一行后，使用LAST_INSERT_ID()获取分配的ID，然后使用mysql_use_result()从执行的select语句中获取数据并打印
            
        返回数据的语句：
            数据是使用select语句提取的
            1、执行查询；2、提取数据；3、处理数据；4、必要的清理工作
            使用mysql_query发送SQL语句，使用mysql_store_result或mysql_use_result提取数据，
            接着，使用一系列mysql_fetch_row来处理数据，最后使用mysql_free_result是否查询占用的内存资源
            
            MYSQL_RES *mysql_store_result(MYSQL *connection)，一次返回一行数据
            MYSQL_RES *mysql_store_result(MYSQL *connection);
            从select（或其他返回数据的语句）中提取所有数据，立刻保存在客户端中返回的所有数据，返回指向结果集结构的指针，失败返回NULL
            mysql_store_result调用成功后，需要调用mysql_num_rows得到返回记录的数目，如果没有返回行值为0
            my_ulonglong mysql_num_rows(MYSQL_RES *result);
            返回mysql_store_result结果集中的行数
            
            MYSQL_ROW *mysql_fetch_row(MYSQL_RES *result);
            从mysql_store_result的结果集中提取一行，并放到一个行结构中，数据用完或发生错误时返回NULL
            void mysql_data_seek(MYSQL_RES *result, my_ulonglong offset);
            用来在结果集中进行跳转，设置将被下一个mysql_fetch_row操作返回的行，offset为0~结果集行数-1
            MYSQL_ROW_OFFSET mysql_row_tell(MYSQL_RES *result);
            返回一个偏移值，用来表示结果集中的当前位置，不是行号，不能用于mysql_data_seek
            MYSQL_ROW_OFFSET mysql_row_seek(MYSQL_RES *result, MYSQL_ROW_OFFSET offset);
            使用上面的返回值，将在结果集中移动当前位置，并返回之前的位置
            void mysql_free_result(MYSQL_RES *result);
            是否占用的内存
            
            使用mysql_use_result，必须反复调用mysql_fetch_row直到提取了所有的数据，如果没有从mysql_use_result中得到所有数据，那么程序中后续的提取数据操作可能会返回遭到破坏的信息
            
        处理返回的数据：
            MYSQL返回两种类型的数据：
                1、从表中提取的信息，也就是列数据
                2、关于数据的数据，即所谓的元数据（metadata），例如列名和类型
            unsigned int mysql_field_count(MYSQL *connection);
            提供了一些关于查询结果的基本信息，返回结果集中的字段（列）数目
            MYSQL_FIELD *mysql_fetch_field(MYSQL_RES *result);
            同时将元数据和数据提取到一个新的结构中，需要重复调用，直到返回NULL为止
            MYSQL_FIELD结构（mysql.h中定义）：
                char *name，         列名，为字符串
                char *table,         列所属的表名，对多表查询很有用，对于结果中可计算的值如MAX 所对应的表名为空字符串
                char *def,           如果调用mysql_list_fields，它将包含该列的默认值
                enum enum_field_types type，列类型
                unsigned int length，    列宽，定义表时指定的
                unsigned int max_length，使用mysql_store_result，它将包含以字节为单位的提取的最长列值的长度，使用mysql_use_result，将不会被设置
                unsigned int flags， 关于列定义的标志，与值无关，如：NOT_NULL_FLAG、PRI_KEY_FLAG...
                unsigned int decimals，  小数点后的数字个数，仅对数字字段有效
            列类型有：
                FIELD_TYPE_DECIMAL
                FIELD_TYPE_LONG
                FIELD_TYPE_STRING
                FIELD_TYPE_VAR_STRING
            预定义宏：IS_NUM，判断字段类型是否为数字时使用
            MYSQL_FIELD_OFFSET mysql_field_seek(MYSQL_RES *result, MYSQL_FIELD_OFFSET offset);
            覆盖当前的字段编号，列号，随每次mysql_fetch_field调用而自动增加，offset为0，将跳回第一列
            
        更多的函数：
            char *mysql_get_client_info(void);          //返回客户端使用的库的版本信息
            char *mysql_get_host_info(MySQL *connection);      //返回服务器连接信息
            char *mysql_get_server_info(MySQL *connection);      //返回当前连接的服务器的信息
            char *mysql_info(MySQL *connection);        //返回最近执行的查询的信息，但仅对一些查询类型有效（如INSERT和UPDATE，否则返回NULL）
            int mysql_select_db(MySQL *connection, const char *dbname); //若用户有权限，则切换为指定数据库，成功返回0
            int mysql_shutdown(MySQL *connection, enum mysql_enum_shutdown_level); //若用户有权限，关闭连接的数据库服务器，成功返回0
