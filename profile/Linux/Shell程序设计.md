
## Shell程序设计

#### Shell简介

    运行时解释执行的
    不适合用来完成时间紧迫型和处理器忙碌型的任务
    两种shell：bash、csh，位于内核之外
    常用shell有：
        sh，源于UNIX早期版本最初的shell
        csh、tcsh、zsh，C shell及其变体
        ksh、pdksh，商业版本UNIX的默认shell
        bash，
    
    标准文件描述符：
        0，代表一个程序的标准输入
        1，标准输出
        2，标准错误输出
    
    Shell只是将linux命令组合在一起，实现特定功能
    比如：文本、文件处理任务等
    
    UNIX不关心脚本文件是否有扩展名，一般使用.sh或其他后缀
    检查文件是否是脚本程序的最好方法是使用file命令，如：file xx.c

    可执行脚本文件一般最后放到环境变量PATH下的bin目录中，如：home下创建bin、/usr/local/bin等
    为防止意外修改脚本，应去掉脚本程序的写权限

#### Shell语法

    脚本程序第一行是：#!/bin/sh
    表示指明shell路径
    
    注释是以#开头

##### 变量

    变量不需要事先做出声明，只是通过使用它们来创建它们
    默认情况下，所有变量都被看作字符串并以字符串来存储，即使被赋值为数值
    区分大小写
    $变量，$加变量名访问它的内容
    
    ab=hello        //等号两边不能有空格
    echo $ab        //echo将变量内容输出到终端
    ab="code hacker"    //字符串包含空格，必须用引号括起来
    echo $ab
    
    read ab     //read命令将用户输入赋值给一个变量，ab参数保存用户输入数据，回车时read命令结束
    echo $ab
    
    脚本文件中的参数以空白字符分割， a b ab d
    若想在一个参数中包含一个或多个空白字符，就必须给参数加上引号
    ab=here
    echo "hi, $ab"，输出：hi, here
    echo 'hi, $ab'，输出：hi, $ab
    echo \$ab， 输出：$ab
    echo '$ab' now equals $ab，输出：$ab now equals here
    
    环境变量：
        环境变量已经存在，直接使用
        $HOME   $PATH   $PS1（命令提示符，$）
        $PS2（二级提示符，提示后续输入，通常是 >）
        $IFS（输入域分隔符，shell读取输入时，它给出用来分割单词的一组字符，空格、制表符、换行符）    
        $0（shell脚本文件名字）   $#（传递给脚本的参数个数）
        $$（shell脚本的进程号）
    
    参数变量：
        参数变量是脚本程序的输入参数，若未输入，则为空
        $1, $2, ...（脚本程序的参数）
        $*（在一个变量中列出所有参数，各个参数之间用环境变量IFS中的第一个字符分隔开）
        $@（是$*的一种变体，不使用IFS环境变量），用于访问脚本程序的参数

##### 条件

    test或[，shell的布尔判断命令，或者[命令]
    test -f filename，检查一个文件是否存在
    if [ -f fred.c ]    //同上，检查文件是否存在
    then
    ..
    fi
    test命令的退出码表明条件是否被满足
    
    test命令可使用的条件类型归为3类：
        字符串比较：
            str1 ！= str2，不同则为true
            -n str， 字符串不空则为true
            -z str， 字符串为null则为true
        算术比较：
            ！expression，取反，表达式为false结果为true
            一下都是 exp1 xx exp2 这种形式，xx如下：
            -eq（两表达式相等则为true）， -ne（两表达式不等则为true）
            -gt（大于）， -ge（大于等于）
            -lt（小于）， -le（小于等于）
        文件条件测试：
        test -d file，以下都是这种形式
            -a，文件存在
            -d，文件是一个目录则为true
            -e，文件存在
            -f，文件是普通文件
            -g，文件的set-group-id位被设置则为true
            -r，文件可读
            -s，文件大小不为0
            -u，文件的set-user-id位被设置
            -w，文件可写
            -x，文件可执行
        set-gid和set-uid标志只对可执行的二进制文件有用
    
    test的具体使用，可参考 help test

##### 控制结构

    if condition
    then
        statements
    else
        statements
    fi
    condition 为条件，statements为条件满足时要执行的一系列命令
    
    if condition; then
        statements
    elif condition; then
        statements
    fi
    
    bash使用echo -n命令去除换行符，或使用printf，去掉每行后面的换行符
    如：echo -n "please input: "，:后面留一空格
    
    for var in values       //values 是任意字符串的集合，或者 $(ls f*.sh)，使用通配符扩展
    do
        statements
    done
    for循环适合于对一系列字符串进行循环处理

    while condition; do
        statements
    done
    while循环适用于重复执行一个命令序列
    
    until condition
    do
        statements
    done
    until与while相似，循环反复执行直到条件为true，而不是在条件为true时反复执行
    循环至少需要执行一次，使用while循环；如果可能根本都不需要执行循环，使用until循环
    
    case var in
      pattern [ | pattern] ...) statements;;    //双分号结尾，标记前一个语句的结束和后一个模式的开始
      pattern [ | pattern] ...) statements;;
      ...
    esac
    将变量的内容和模式进行匹配，然后根据匹配的模式去执行不同的代码
    
    命令列表：执行某个语句之前同时满足好几个不同的条件
    AND：statement1 && statement2 && statement3 && ...   //前一条命令为true，才执行后一条，全部命令都为true才执行语句
    OR：statement1 || statement2 || statement3 || ...    //顺序执行，如果一条命令返回true，则停止
    混合使用：statement1 && statement2 || statement3 ...
    
    语句块：若要在只允许使用单个语句的地方使用多条语句，可使用{}构造语句块
    statement && {
        grep "" 
        cat ...
        ...
    }

##### 函数

    function_name () {
        statements
    }
    function_name
    调用函数前先进行定义，即把函数定义放到函数调用之前
    执行时只需写函数名：function_name
    函数调用时，脚本程序的位置参数（$*、$@、$#、$1等）会被替换为函数的参数，函数执行完毕后，这些参数会恢复为原先的值
    return返回数字值： return 1
    return返回字符串：
        函数将字符串保存在一个变量中，该变量可以在函数结束后被使用；
        或者echo一个字符串并捕获其结果， foo(){echo JAY;}  result="$(foo)"
    函数局部变量：使用local关键字声明， local ab="abc";
    局部变量和全局变量重名，局部变量会覆盖后者，仅限于函数的作用范围内
    函数中没有使用return命令，则函数返回的就是执行的最后一条命令的退出码
    函数参数是在调用函数时跟在其后面的，如： foo "a" "b"

##### 命令

    shell脚本内可执行两类命令：外部命令和内部命令
    
    break命令：跳出循环（for while until）
    :命令：
        是一个空命令，用于简化条件逻辑，相当于true的别名， while :，实现一个无限循环，代替 while true
        用在变量的条件设置中，$(var:=value)，若没有:，shell将把var当作一条命令来处理
    continue命令：使for while until循环跳到下一次循环继续执行
    .命令：
        用于在当前shell中执行命令
        在当前上下文中执行命令，可以使用它将变量和函数定义结合进脚本程序，类似c的#include指令
        . ./，使用点命令执行，每个脚本程序都在当前shell中执行
    echo命令：同printf，去掉换行符--echo -n（适用于linux bash） 或 echo -e "out\c"（不同UNIX版本有不同）
    eval命令：对参数进行求值（内置命令）
        foo=10 x=foo y='$'$x echo $y，输出 $foo
        foo=10 x=foo eval y='$'$x echo $y，输出 10
    exec命令：
        将当前shell替换为一个不同的程序，exec wall "fish"，用wall命令替换当前shell，exec命令后面的代码都不会执行
        修改当前文件描述符，exec 3< afile，使文件描述符3被打开以便从文件afile中读取数据
    exit n命令：使脚本以退出码n结束运行，0表示成功，1-125错误代码，126文件不可执行，127命令未找到，128以上出现一个信号
    export命令：将作为它参数的变量导出到子shell中，并使之有效，把参数创建为一个环境变量
        foo="first"  export bar="second"，输出脚本export1.sh
        在export2.sh中调用export1，并echo foo、 echo bar，将只会输出bar
        set -a或set -o allexport，将导出它之后声明的所有变量
    expr命令：将它的参数当作一个表达式来求值
        x=`expr $x + 1`，反引号使x取值为命令expr的执行结果
        x=$(expr $x + 1)，同上，用$()替换反引号
        $(($x + 1))，同上，| & = > >= %等
    printf命令：printf "format string" param1 param2
        格式字符串，除了% \之外的所有字符串都将按原样输出
        \转义，\a 报警（响铃或蜂鸣），\b退格，\c取消进一步输出，等等
        %d 输出十进制，%c 输出字符，%s 输出字符串，%% 输出%
        更多查看 man 1 printf
        printf "%s %d\t%s" "Hi here" 15 people
    return命令：使函数返回，有一个数值参数 return 3，若没指定参数，return命令默认返回最后一条命令的退出码
    set命令：为shell设置参数变量
        set $(date)  echo month is $2，把date命令的输出设置为参数列表，然后通过位置参数获取
        控制shell的执行方式：set -x，让一个脚本程序跟踪显示它当前执行的命令
    shift命令：把所有参数变量左移一个位置，$2变$1，以此类推，$1将丢弃，$0保持不变
        while [ "$1" != "" ]; do
            echo "$1"; shift        //依次扫描位置参数
        done
    trap命令：用于指定在接收到信号后将要采取的行动
        在脚本程序被中断时完成清理工作
        使用信号的名字，定义在signal.h中，使用信号名时省略SIG前缀， trap -l 查看信号编号和名称
        trap command signal，command是接收到指定信号时将要采取的行动，signal是要处理的信号名
        trap - signal，重置某个信号的处理方式到其默认值
        trap '' signal，忽略某个信号
        trap，不带参数，将列出当前设置的信号及其行动的清单
    信号：
        HUP(1)，挂起，通常因终端掉线或用户退出引发
        INT(2)，中断，通常因Ctrl C引发
        QUIT(3)，退出，通常因Ctrl \引发
        ABRT(6)，中止，通常因严重的执行错误而引发
        ALRM(14)，报警，通常用来处理超时
        TERM(15)，终止，通常系统关机时发送
        更多参见 man 7 signal
    unset命令：从环境中删除变量或函数，不能删除shell本身定义的只读变量（如IFS）
        foo="ss hh"  unset foo  echo $foo，只打印换行符，foo变量被从环境中删除
    find命令：用于搜索文件
    grep命令：通用正则表达式解析器，在文件中搜索字符串

##### 正则表达式

    ^（行的开头）、$（行的结尾）、.（任意单个字符）、[]（字符范围，范围内任何一个字符都可被匹配）、^[]（不匹配指定范围内的字符）
    特殊匹配模式：
        [:alnum:]，字母与数字字符
        [:alpha:]，字母
        [:ascii:]，ASCII字符
        [:blank:]，空格或制表符
        [:cntrl:]，ASCII控制字符
        [:digit:]，数字     
        [:graph:]，非控制、非空格字符
        [:lower:]，小写字母
        [:print:]，可打印字符
        [:punct:]，标点符号字符
        [:space:]，空白字符，包括垂直制表符
        [:upper:]，大写字母
        [:xdigit:]，16进制数字
    ?（最多匹配一次，0或1）、*（必须匹配>=0次，0或多次）、+（必须匹配>=1次，1或多次）
    {n}（必须匹配n次）、{n,}（必须匹配n次或n次以上）、{n,m}（匹配次数n到m之间，包括n、m）
    
    grep e$ words.txt，查找以e结尾的行
    grep a[[:blank:]] word.txt，查找以字母a结尾的单词
    grep Th.[[:space:]] words.txt，查找以Th开头的3个字母组成的单词
    grep -E [a-z]\{10\} wrods.txt，查找10个字符长的小写字母单词

##### 命令的执行

    在脚本程序中捕获一条命令的执行结果，并把命令的输出放到一个变量中
    可使用：$(command) 或 `command`
    echo current users are $(who)
    whoishere=$(who)
    
    算术扩展：
        x=$(($x+1)) //可代替expr命令，执行比expr快
        两对圆括号用于算术替换，一对圆括号用于命令的执行和获取输出
    参数扩展：
        ${var}temp，在变量名后附加额外的字符，不加{}的话，vartemp会被认为是一个变量
        ${param:-default}，param为空，就把它设置为default的值
        ${#param}，给出param的长度
        ${param%word}，从param的尾部开始删除与word匹配的最小部分，返回剩余部分
        ${param%%word}，从param的尾部开始删除与word匹配的最长部分，返回剩余部分
        ${param#word}，从param的头部开始删除与word匹配的最小部分，返回剩余部分
        ${param##word}，从param的头部开始删除与word匹配的最长部分，返回剩余部分
        
    ${foo:=bar}，检查foo是否存在且不为空，不为空返回它的值，否则赋值为bar并返回
    ${foo:?bar}，foo不存在或设置为空时，输出foo:bar并异常终止脚本
    ${foo:+bar}，foo存在且不为空时返回bar
    
    cjpeg image.gif > image.jpg，将GIF转换为JPEG
    脚本批量处理：
        for image in *.gif
        do
            cjpeg $image > ${image%%gif}jpg
        done

##### here文档

    向一条命令传递输入，允许一条命令在获得输入数据时就好像是在读取一个文件或键盘一样，实际上是从脚本程序中得到输入数据
    here文档以两个连续的 << 开始，紧跟着一个特殊的字符序列，该序列将在文档结尾处再次出现
    给cat提供输入数据：
        cat <<!FUNKY!
        hello 
        this is a here 
        document
        !FUNKY!
    常见用途是在脚本程序中输出大量的文本，避免用echo
    ed行编辑器

##### 调试脚本程序

    出现错误时，shell一般都会打印出错的行的行号，可添加echo显示变量的内容进行测试
    对复杂错误可设置各种shell选项，调用shell时加上命令行选项或使用set命令：
        sh -n <script>  set -o noexec   set -n，只检查语法错误，不执行命令
        sh -v <script>  set -o verbose  set -v，执行命令之前回显它们
        sh -x <script>  set -o xtrace   set -x，处理命令之后回显它们
        sh -u <script>  set -o nounset  set -u，若使用了未定义的变量，就给出出错消息
    set命令：-o启用设置，+o取消设置
    set -x，set +x，启用、取消，输出+个数为变量扩展的层次
    设置PS4变量，修改+为其他字符
    
    捕获EXIT信号，在脚本退出时查看程序的状态：
        trap 'echo exiting: critical var = $critical_var' EXIT，在脚本开始处添加

##### 图形化：dialog工具

    仅在linux控制台上有用
    dialog工具、gdialog工具，可将对dialog工具的调用替换为对gdialog工具的调用
    dialog --msgbox "Hello World" 9 18，显示对话框
    类型：
        --checklist，复选框，每个选项都可被选中， --checklist text height width list-h [tag text status] ...
        --infobox，信息框，显示消息后对话框立刻返回 --infobox text height width
        --inputbox，输入框，输入文本，    --inputbox text height width [initial string]
        --menu，菜单，选择列表中的一项，     --menu text height width menu-h [tag item]...
        --msgbox，消息框，向用户显示一条消息， --msgbox text height width
        --radiolist，单选框，选择一项，   --radiolist text height width list-h [tag text status] ...
        --textbox，文本框，带有滚动条显示文件内容， --textbox filename  height width
        --yesno，是/否框，允许用户提问，用户可选择是否， --yesno text  height width
        进度框、密码框等等，参考 man
        所有对话框类型相同的参数有：--title 标题、--clear（清屏）等等    
    dialog --title "check me" --checklist "Pick Num" 15 25 3 1 "one" "off" 2 "two" "on" 3 "three" off
    
    环境变量 $? 的内容就是前一个命令的退出状态