## Kotlin

    静态类型，编译期间确定所有表达式的类型
    可空类型
    函数类型，函数式编程
    数据类，创建不可变对象的简明语法

#### 函数式编程

    核心概念：
        头等函数：把函数当作值使用，可以用变量保存它，把它当作参数传递，或者当作其他函数的返回值
        不可变性：使用不可变对象，保证了它们的状态在其创建之后不能再变化
        无副作用：使用的是纯函数，不会修改其他对象的状态，也不会和外面的世界交互
    lambda表达式
    多线程安全：不可变数据结构和纯函数，保证不会发生多线程修改

#### Kotlin基础

    函数和变量：
        函数：
            fun funcName(a: Int): Int { return if (a>b) a else b }
            if是表达式，不是语句
            表达式有值，且能作为另一个表达式的一部分；
            语句总是包围着代码块中的顶层元素，没有自己的值；java中的控制结构都是语句，kotlin中除了循环以外大多数控制结构都是表达式
            表达式函数体：函数体是由单个表达式构成的，去掉花括号和return语句，还可以省略返回类型
        变量：
            val str : String， 关键字+变量名+变量类型（可以省略）
            val（不可变引用，对于java的final）、var（可变引用）
            应该尽可能地使用val关键字，不可变引用、不可变对象及无副作用的函数更接近函数式编程风格（且线程安全）
            val只能初始化一次，可以根据条件使用不同的值初始化
        字符串格式化：（字符串模板）
            "hello, $name"， "hello, ${if(a>b) a else "cc"}"

    类和属性：
        值对象： class Person(val name: String)，public是默认的可见性
        属性：
            java中字段和其访问器的组合被叫作属性，只读属性只有getter
            kotlin隐藏了getter和setter
        自定义访问器：
            val isA: Boolean
                get() {
                    return a=b
                }
        源码布局：目录和包
            每个kotlin文件都以package语句开头，文件中的所有声明都在包中
            相同的包可以直接使用，不同的包需要导入 import
            import可以导入任何种类的声明
            一个kotlin文件可以包含多个类，即一个kotlin文件可以作为一个java的包（一个文件就可以是一个包）
            包层级结构不需要遵循目录层级结构
            推荐将多个类放进同一个文件中，特别是很小的类

    枚举和 when：
        声明枚举类：
            enum class Color(val r: Int, val g: Int, val b: Int) {
                RED(255, 0, 0), BLUE(0, 0, 255), INDIGO(75, 0, 130);//kotlin中唯一使用分号的地方
                fun rgb() = (r*256 + g)*256 + b
            }

        使用 when 处理枚举类：
            对应java的switch，但是更强大
            可以直接返回when表达式的表达式体函数
            when(color) {
                RED, YELLOW -> "warm"
                BLUE -> "cold"
            }
            when的实参可以是任何对象
            when(setOf(c1, c2)) {
                setOf(RED, YELLOW) -> ORANGE    //会创建额外的对象
                else -> XXX
            }
            when可以不带参数
            when {
                (a==b)||(a==c) -> ORANGE    //不会创建额外的对象
                else -> XXX
            }

        智能转换：合并类型检查和转换
            Expr接口，只是一个标记接口，用来给不同种类的表达式提供一个公共的类型
            class Num(val value: Int): Expr，：后面跟接口，标记类实现了接口
            a is b，类型检查，类似instanceOf
            a as b，显式的类型转换
            if(a is Sum)
                return eval(a.left)
            在类型检查之后不再需要显式的类型转换，由编译器执行，这种行为称为 智能转换
            智能转换的属性必须是val，且不能有自定义的访问器
        用when 代替 if：
            when与智能转换结合
            when(e) {
                is A -> e.value
                is B -> "b"
                else -> ""
            }
        代码块作为 if 和 when 的分支：
            代码块作为分支体，代码块中的最后一个表达式就是结果
            when(e) {
                is A -> {
                    println("logging")
                    e.value
                }
            }
            代码块中最后的表达式就是结果，在所有使用代码块并期望得到一个结果的地方都成立
            对try、catch、lambda表达式都适用
            对常规函数不成立

    迭代事物：while 循环和 for 循环
        while 循环：同java
        迭代数字：区间和数列
            区间，1..10，是包含或者闭合的，值是区间的一部分
            数列，迭代区间中所有的值的区间称作数列
            for (i in 1..100)，for (i in 'A'..'Z')，字符区间
            for (i in 100 downTo 1 step)，步长为2，方向倒序
            for (i in 0 until 100)，半闭合区间，不包含100，到100-1
            downTo、step、until
        迭代map：
            val bt = TreeMap<Char, String>()
            bt[key] = value
            for ((key, value) in bt)，for循环展开迭代中的集合的元素
            for ((index, element) in list.withIndex())，使用展开语法跟踪当前项的下标
        使用 in 检查集合和区间的成员：
            fun isLetter(c: Char) = c in 'a'..'z' || c in 'A'..'Z'
            fun isNotDigit(c: Char) = c !in '0'..'9'
            等同Java的 < > 比较操作符
            in、!in 也适用于when表达式
            in 也适用于实现Comparable接口的类 以及 集合（Set等）

    Kotlin中的异常：
        throw Exception()，抛出异常，throw结构是一个表达式，能作为另一个表达式的一部分使用
        val per = if (xx) num else throw Exception()，条件成立per被初始化，不成立抛出异常且不初始化变量
        try catch 和 finally：
            try {
            } catch(e) {} finally {}
            函数声明后不用写上 throws，即受检异常不需要显式地处理
            Kotlin不区分受检异常和未受检异常，可以不处理异常
        try 作为表达式：
            val num = try { 10 } catch(e) { return }，return 或 null
            和 if when 一样，表达式体

#### 函数的定义与调用

    在Kotlin中创建集合：
        val set = hashSetOf(1, 3, 90)
        val list = arrayListOf(1, 3, 99)
        val map = hashMapOf(1 to "one", 5 to "five")，to 是个普通函数
        list.last()，set.max()，kotlin扩展函数

    函数调用：
        命名参数：
            func(sep = "", prefix = " ", postfix = ".")
            调用函数时可以显式的标明一些参数的名称
            调用Java函数时不能采用命名参数
        默认参数值：
            声明函数时可以指定参数的默认值，这样可以避免创建重载的函数
            fun func(sep = "", prefix = " ", postfix = ".")
            常规调用语法必须按参数顺序来给定参数，命名参数更灵活
            @JvmOverloads，注解方法时会为该方法生成Java重载函数（从最后一个开始省略参数）
        消除静态工具类：顶层函数和属性
            Java工具类不适用类的概念
            Kotlin中把这些东西放到代码文件的顶层，不用从属于任何的类
            包外访问需要 import
            编译成Java类为 代码文件名作为类名，方法为静态方法，在Java中访问时 文件名.function()
            修改文件生成的类名使用@JvmName注解，要放在文件开头，包名前面
            eg: @file:JvmName("className")
                package xxx
                fun func()
            调用时： className.func()

            顶层属性：
            和函数一样，属性也放到文件顶层
            会存储到静态字段中
            val可定义常量
            const修饰（使用于所有基本数据类型以及String类型）
            const val xx 等同于 Java代码：public static final String xx

    给别人的类添加方法：扩展函数和属性
        扩展函数：
            就是一个类的成员函数，定义在类的外面
            fun String.lastChar() = this.get(this.length-1)
            类的名称称为接收者类型，调用扩展函数的对象叫作接收者对象
            可以像调用类的普通成员函数一样去调用扩展函数
            扩展函数中可以直接访问被扩展的类的其他方法和属性（不能访问私有或受保护的成员）
        导入和扩展函数：
            导入单个的函数，import strings.lastChar
            *
            使用关键字as 修改导入的类或函数名称，import strings.lastChar as last
            as 用来解决命名冲突
        从Java中调用扩展函数：
            扩展函数实质上是静态函数，把调用调用对象作为第一个参数
            扩展函数被声明为顶层函数，被编译为一个静态函数，文件名+函数名调用（UtilKt.func()）
        作为扩展函数的工具函数：
            可使用泛型作工具函数等
            使用泛型指定类型等
        不可重写的扩展函数：
            类的重写成员函数是可以的，但不能重写扩展函数
            扩展函数是静态函数，调用重名的扩展函数时，由该变量的静态类型决定，而不是变量的运行时类型
            静态函数不存在重写
            注意：一个类的成员函数和扩展函数有相同的签名，成员函数会被优先使用（从而使扩展函数失效）
        扩展属性：
            val String.lastChar: Char
                get() = get(length-1)
            必须定义getter函数，因为没有默认的get实现，初始化也不行，因为没有地方存储初始值
            var、val 根据实际请求设置可变性
            Java调用时 StringUtilKt.getLastChar("Java")

    处理集合：可变参数、中缀调用和库的支持
        可变参数（vararg）、中缀表示法、解构声明（把组合值展开到多个变量中）
        扩展Java集合的API：
            last、max、创建集合的函数
        可变参数：让函数支持任意数量的参数
            fun listOf<T>(vararg values: T): List<T> {...}
            vararg修饰符（可变参，将任意个数参数值打包到数组中）
            可变参传递数组时需要显式地解包数组（Java可按原样传递）
            解包数组被称为展开运算符（*），eg：listOf("args: ", *args)
            可以单个调用中组合数组的值和某些固定值（Java不支持）
        键值对的处理：中缀调用和解构声明
            mapOf(1 to "one", 2 to "two")
            to 是一种特殊的函数调用，称为中缀调用
            1.to("one") 等价于 1 to "one"
            中缀调用可以与只有一个参数的函数一起使用，需要使用infix修饰符标记
            infix fun Any.to(other: Any) = Pair(this, other)
            直接用Pair的内容初始化两个变量：
                val (number, name) = 1 to "one"，称为解构声明
                用to函数创建一个Pair，然后用解构声明来展开
            解构声明的使用：for((index, item) in collection.withIndex())
            to函数可以创建一对任何元素

    字符串和正则表达式的处理：
        分割字符串：
            Java的split不支持 . \ | - ，它把正则表达式作为参数
            Kotlin重载该方法，需要显式地传递正则，或者是支持指定多个分隔符
        正则表达式和三重引号的字符串：
            val regex = """(.+)/(.+)\.(.+)""".toRegex()，
            val mr = regex.matchEntire("xxxxx")， val (dir, name, ext) = mr.destructured
            Kotlin中正则表达式的使用
            三重引号的字符串中不需要对任何字符进行转义，包括\
        多行三重引号的字符串：
            三重引号可以包含任何字符，包括换行符等
            可以向字符串添加前缀标记边距的结尾，然后调用trimMargin(".")删除每行中的前缀和前面的空格
            包含换行（不是 \n），不必转义字符\
            使用字符串模板必须使用嵌入式表达式，eg："""${'$'}99.9"""

    让代码更整洁：局部函数和扩展
        在函数中嵌套提取的函数
        fun outFunction() {
            fun innerFunction() {
            }
            innerFunction()//局部函数调用
            innerFunction()
        }
        局部函数可以访问所在函数中的所有参数和变量
        类的API只能包含必需的方法
        扩展函数也可以被声明为局部函数，不建议使用多层嵌套

#### 类、对象和接口

    Kotlin接口可以包含属性声明
    Kotlin的声明默认是final和public的
    Kotlin嵌套类默认不是内部类（没有包含对其外部类的隐式引用）

    类继承结构：
        Kotlin中的接口：
            interface Clickable {
                fun click()
                fun showOff() = xxxx //默认实现（Java8需要default关键字）
            }
            与Java8中的相似，可以包含抽象方法以及非抽象方法的实现，不能包含状态
            class Button : Clickable {
                override fun click() = xxx
            }
            : 代替了extends和implement关键字，一个类可实现多个接口，但只能继承一个类
            override修饰符（强制使用）类似Java的注解
            同时实现的多个接口中有同名方法时必须得显式实现（使用 super<Clickable>.showOff() 表明想要调用哪个父类的方法）
            Java实现Kotlin接口时必须为所有的方法都提供自己的实现（即使是默认方法也要）
        open、final和abstract修饰符：默认为final
            没有特别需要在子类中被重写的类和方法应该被显式地标注为final
            Java类和方法默认是open的，Kotlin中默认都是final的
            需要被继承的类和被重写的属性或方法要使用open修饰符标示
            open class Rich : Clickable {
                open fun animate() {}
                final override fun click() {}
            }
            重写了的成员（方法、属性）同样默认是open的，需要显式地标注final（禁止重写）
            抽象类（类似Java抽象类）：
            abstract class Animated {
                abstract fun animate()
            }
            抽象成员始终是open的，不需要显式地使用open，非抽象函数默认不是open的
            接口中的成员始终是open的，不能将其声明为final
            类中的访问修饰符：
            final（不能被重写）、open（可以被重写）、abstract（必须被重写）、override（重写的成员）
        可见性修饰符：默认为public
            public（所有地方可见）、protected（子类中可见）
            private（类中可见或文件中可见）、internal（表示只在模块内部可见）
            一个模块就是一组一起编译的Kotlin文件
            Kotlin允许在顶层声明中使用private可见性，包括类、函数和属性，这些声明就只在声明的文件中可见
            类的扩展函数不能访问它的private和protected成员
        内部类和嵌套类：默认是嵌套类
            Kotlin的嵌套类不能访问外部类的实例
            内部类需要使用inner修饰符（持有外部类的引用）
            嵌套类：class A，内部类：inner class A。同Java的静态内部类和内部类
            内部类访问外部类，使用 this@Outer
            class Outer {
                inner class Inner {
                    fun getOuterRef() = this@Outer
                }
            }
        密封类：定义受限的类继承结构
            sealed类，为父类添加一个sealed修饰符，对子类做出严格的限制，所有的直接子类必须嵌套在父类中
            sealed class Expr {
                class Num(): Expr()
                class Sum(): Expr()
            }
            sealed标记为密封的，将所有可能的类作为嵌套类列出
            sealed隐含这个类是open的
            密封类不能在类外部拥有子类

    声明一个带非默认构造方法或属性的类：
        主构造方法（在类体外部声明）和从构造方法（在类体内部声明），初始化语句块
        初始化类：主构造方法和初始化语句块
            主构造方法：class User(val nickname: String)，表明构造方法的参数，以及定义属性
            class User constructor(name: String) {
                val nickname: String
                init { nickname = name }
            }
            constructor关键字用来声明一个主构造方法或从构造方法
            init关键字用来引入一个初始化语句块，类被创建时执行的代码
            如果主构造方法没有注解或可见性修饰符，可以去掉constructor关键字
            构造方法中的 val 意味着相应的属性会用构造方法的参数来初始化
            可以为构造方法参数提供一个默认值，也可以显式地为某些参数标明名称，同普通函数使用一样
            如果所有参数都有默认值，编译器会生成一个无参构造方法
            class TUser(nickname: String) : User(nickname) {...}
            主构造方法需要初始化父类，基类提供父类构造方法的参数
            open class Button {}，不带参数，会有默认构造方法
            class SButton: Button()，必须显式地调用父类的构造方法（带有括号），接口不需要括号（这就是区别）
            class Service private constructor() {}，不被其他代码实例化
            Java的静态工具类可以使用顶层函数，单例可以使用对象声明（伴生对象）
        构造方法：用不同的方式来初始化父类
            open class View {
                constructor(ctx: Context, attr: Attr) {...}
            }
            class Button : View {
                constructor(ctx: Context): this(ctx, NULL) {...}
                constructor(ctx: Context, attr: Attr): super(ctx, attr) {...}
            }
            如果类没有主构造方法，那每个从构造方法必须初始化基类（super）或委托（this）给另一个这样做了的构造方法
        实现在接口中声明的属性：
            interface User {
                val nickname: String        //抽象属性声明
            }
            实现接口的类需要提供一个取得属性值的方法（getter）
            class PUser(override val nickname: String) : User   //主构造方法属性
            class SBUser(val email: String) : User {
                override val nickname: String
                    get() = email.sub()     //自定义getter
            }
            class FBUser(val id: Int): User {
                override val nickname = getFBName(id)       //属性初始化
            }
            接口还可以包含具有getter和setter的属性，只要它们没有引用一个支持字段（具有默认getter的属性可以被继承，可以不重写）
        通过getter或setter访问支持字段：
            存储值的属性和具有自定义访问器在每次访问时计算值的属性
            val address: String
                set(value: String) {field = value ...}
                get() {...  return field}
            使用field则含有支持字段，不适用field，则支持字段不会被呈现出来
            val 就是getter，var 则是两个访问器
        修改访问器的可见性：
            访问器的可见性默认与属性可见性相同
            val counter: Int = 0
                private set         //修改可见性，不能在类外部修改这个属性
            lateinit修饰符，这个属性将初始化推迟到构造方法被调用过后
            惰性初始化属性，委托属性的一部分
            const修饰符，注解的使用

    编译器生成的方法：数据类和类委托
        通用对象方法：
            toString、equals、hashCode
            重写，override
            对象相等性，equals，Kotlin中的 == 代表equals
            ===运算符与Java中的 == 一样（比较对象引用）
            is、!is （同instanceof）
            hashCode 通常与 equals 一起被重写
            如果两个对象相等，它们必须有着相同的hash值
            hashCode 重写一般为： value.hashCode() * 31 + num
        数据类：自动生成通用方法的实现
            data class Client(val name: String, val num: Int)
            Kotlin为 data 类自动生成 toString、equals、hashCode
            equals和hashCode会将所有在主构造方法中声明的属性纳入考虑
            没有在主构造方法中声明的属性将不会加入到相等性检查和哈希值计算
            数据类和不可变性：copy()方法
            推荐使用val 让数据类实例不可变
            Kotlin生成的创建副本的方法，copy()
                fun copy(name: String = this.name, num: Int = this.num) = Client(name, num)
            copy的同时可以修改某些属性的值
        类委托：使用 by 关键字
            装饰器模式，向不可被修改的类扩展其功能，拥有一个原始类实例，组合和转发
            Kotlin将委托作为一个语言级别的功能，可以使用by关键字将接口的实现委托到另一个对象
            class DCollection(innerList: Collection = ArrayList()) : Collection by innerList {
                //可以重写需要修改的方法
                override fun add(element: T): Boolean {
                    //do something
                    return innerList.add(element)
                }
            }
            by将DCollection的实现委托给 innerList，可以重写需要实现的方法

    object 关键字：将声明一个类与创建一个实例结合起来
        object关键字定义一个类并同时创建一个实例（对象），使用场景：
            对象声明是定义单例的一种方式
            伴生对象可以持有工厂方法和其他与这个类相关，但调用时不依赖类实例的方法，通过类名来访问
            对象表达式用来替代Java的匿名内部类
        对象声明：创建单例易如反掌
            对象声明将类声明与该类的单一实例声明结合到了一起
            object Singleton {
                fun do() {}
            }
            对象声明通过object关键字引入，对象声明不允许构造方法（主、从构造方法），其他与类一样（属性、方法、初始化语句块等）
            对象声明在定义的时候就立即创建了，不需要在其他地方调用构造方法
            使用方式：Singleton.do()，方法、属性都直接使用对象名+点的方式
            可以在类中声明对象，这样的对象同样只有一个单一实例（多个类实例共用一个声明对象？）
            data class Person(val name: String) {
                object NameComparator: Comparator<Person> {...}
            }
            Kotlin中的对象声明被编译成通过静态字段来持有它的单一实例的类（字段名始终都是INSTANCE，通过INSTANCE使用单例）
        伴生对象：工厂方法和静态成员的地盘
            Kotlin的类不能有静态成员（static）
            顶层函数和对象声明（包括静态方法和静态字段）作为static的替代
            顶层函数不能访问类的private成员，但是类中的对象声明可以
            class A {
                companion object {
                    fun bar() {...}
                }
            }
            调用方式：A.bar()
            companion，使可以通过容器类名称（A）直接访问声明对象的方法和属性
            适合使用工厂方法
        作为普通对象使用的伴生对象：
            伴生对象是一个声明在类中的普通对象，可以有名字、实现接口、有扩展函数或属性
            class P() {
                companion object Loader {
                    fun from(): P {}
                }
            }
            在伴生对象中实现接口，可以直接将包含它的类的名字当作实现了该接口的对象实例来使用
            class P() {
                companion object: JSONFactory {...}
            }
            fun loadFromP(factory: JSONFactory): P {...}
            loadFromP(P)    //直接使用容器类名 P
            伴生对象会被编译成 类中的一个引用了它实例的静态字段，若伴生对象没有名字，静态字段默认为Companion
            @JvmStatic、@JvmField 注解声明static字段（Java使用）
            伴生对象扩展
            如果类C有一个伴生对象，在C.Companion上定义一个扩展函数func，那可以通过C.func()来调用
            class P() {
                companion object {}
            }
            fun P.Companion.fromJSON(json: String): P {}
            P.fromJSON(json)    //调用
        对象表达式：改变写法的匿名内部类
            object声明匿名对象，替代了Java中的匿名内部类
            window.addMouseListener(object: MouseAdapter() {
                override fun clicked(){...}
            })
            匿名对象可以实现多个接口或者不实现接口
            匿名对象不是单例，每次对象表达式被执行都会创建一个新的对象实例
            对象表达式能修改在创建对象的作用域中定义的变量，Java中访问被限制在final变量

#### Lambda编程

    lambda，本质上就是可以传递给其他函数的一小段代码
    Lambda表达式和成员引用
        Lambda简介：作为函数参数的代码块
            函数式编程，把函数当作值来对待，直接传递函数（不需要先声明类传递类实例）
            lambda表达式可以直接传递代码块作为函数参数
            lambda被当作只有一个方法的匿名对象的替代品
        Lambda和集合：
            避免重复代码，使用库函数
            maxBy函数，可以在任何集合上调用
            成员引用，list.maxBy(Person::age)
        Lambda表达式的语法：
            val sum = {x: Int, y: Int -> x + y}，Kotlin的lambda表达式始终用花括号包围，实参没有用()括起来
            可以把lambda表达式存储在一个变量中，把变量当作普通函数对待（通过实参调用），如：sum(1, 2)
            lambda表达式的简化：
            1、如果lambda表达式是函数调用的最后一个实参，它可以放到括号外边，如：list.maxBy() {...}
            2、当lambda是函数唯一的实参时，还可以去掉调用代码中的空括号，如：list.maxBy {...}
            3、如果lambda参数的类型可以被推导出来，那就不需要显式地指定它，如：list.maxBy { p -> p.name }
            4、使用默认参数it代替命名参数（如果lambda只有一个参数且这个参数类型可以推断处理，就会生成it），如：list.maxBy{ it.name }
                仅在实参名称没有显式指定时才会生成默认名称it
            lambda表达式中的最后一个表达式就是它的结果
        在作用域中访问变量：
            在函数内部使用lambda，它可以访问这个函数的参数，还有在lambda之前定义的局部变量
            Kotlin的lambda中允许访问非final变量，还可以修改这些变量
            从lambda内访问外部变量，称这些变量被lambda捕捉
            捕捉可变变量的实现细节：final变量的值会被拷贝下来，而可变变量的值被作为Ref类的一个实例被存储下来，Ref变量是final的，实际值被存储在其字段中，这样就可以在lambda内修改
        成员引用：
            把函数转换成一个值，就可以传递它，使用::运算符来转换，eg：val getAge = Person::age
            这种表达式称为成员引用，创建一个调用单个方法或者访问单个属性的函数值（方法或属性），后面不能加括号
            成员引用和lambda具有一样的类型，可以互换使用
            fun salute() = println("xxx")
            run(::salute)，引用顶层函数（不是类的成员）
            可以用构造方法引用 存储或延期执行创建类实例的动作，构造方法引用的形式是在::后指定类名
            class P(val name: String)，val createP = ::P，val p = createP("jec")
            扩展函数也可以使用成员引用的方式
            绑定引用
    集合的函数式API
        函数式编程风格，通用的库函数
        基础：filter和map
            filter和map是集合操作的基础，很多集合操作都是借助它们达成的
            filter函数遍历集合并选出应用给定lambda后会返回true的那些元素（过滤符合条件的元素）
            list.filter{ it%2==0 }，返回值是个集合
            map函数对集合中的每一个元素应用给定的函数并把结果收集到一个新集合（转换每个元素）
            map的结果是新集合，但包含的元素个数不变
            使用这些运算符时注意不要重复运算等操作
        all、any、count 和 find：对集合应用判断式
            all、any函数，用于检查集合中所有元素是否都符合某个条件（或是否存在符合的元素）
            count函数，检查有多少元素满足判断式
            find函数，返回第一个符合条件的元素（同firstOrNull）
            all 是否所有元素都满足，any 集合中是否至少存在一个匹配的元素
        groupBy：把列表转换成分组的map
            把所有元素按照不同的特征分成不同的分组
            list.groupBy{...}，结果是一个map，key是元素分组依据的键，value是元素
            list.groupBy(String::first)，把字符串按照首字母分组，first为扩展函数
        flatMap和flatten：处理嵌套集合中的元素
            books.flatMap {it.authors}.toSet()
            flatMap首先对集合中的每个元素做变换（或映射，通过给定的函数），然后把多个列表合并（平铺）成一个列表
            flatten函数，只是平铺一个集合（把多个列表合并成一个）
    惰性集合操作：序列
        链式集合操作的代码的性能
        map、filter这些函数会及早地创建中间集合，而序列可以避免创建这些临时中间对象
        list.asSequence().map().filter{}.toList()
        把初始集合转换成序列，序列支持和集合一样的API，最后把结果序列转换回列表
        序列不会创建中间集合
        Kotlin惰性集合操作的入口就是sequence接口，表示一个可以逐个列举元素的元素序列
        sequence只提供 iterator 这一个方法，用来从序列中获取值
        扩展函数asSequence把任意集合转换成序列，toList用来做反向的转换，序列只能用于迭代元素，其他操作需要使用集合
        大型集合链式操作时要使用序列（性能），对数据量很小的集合使用及早操作更高效（不使用序列）
        序列的操作是惰性的

        执行序列操作：中间和末端操作
            序列操作分为两类，中间的和末端的
            中间操作返回的是另一个序列，新序列知道如何变换原始序列中的元素
            末端操作返回的是一个结果，结果可能是集合、元素、数字或者从初始集合的变换序列中获取的任意对象
            map、filter为中间操作，toList为末端操作
            中间操作始终都是惰性的，它们只有在获取结果的时候才会被应用（末端操作被调用的时候）
            末端操作触发执行了所有的延期计算
            链式操作的执行顺序：集合上，先在每个元素上调用第一个操作符，然后在结果集合上调用下一个；
                序列上，所有操作是按顺序应用在每一个元素上，处理完第一个元素再接着第二个，以此类推
            操作符的顺序也会影响性能（操作执行的次数）
            流和序列（类似），Java8的流可以在多个CPU上并行执行流操作（map、filter等）
        创建序列：
            asSequence，把集合转换为序列
            generateSequence函数，给定序列中的前一个元素，它会计算出下一个元素
            generateSequence(0) {it+1}.takeWhile{it<=100}.sum()
            惰性序列，获取结果时才会触发执行计算
    使用Java函数式接口：
        Java中只有一个抽象方法的接口被称为函数式接口，或者SAM接口，SAM代表单抽象方法
        Kotlin拥有完全的函数类型
        把lambda当作参数传递给Java方法：
            可以把lambda传给认为期望函数式接口的方法
            编译器会自动把lambda转换成一个接口实例（匿名类）
            在显式声明对象时，每次调用都会创建一个新实例
            使用lambda时，如果lambda没有访问任何定义它的函数的变量，那么相应的匿名类实例可以在多次调用之间重用
            lambda被传给 inline标记的函数（内联函数）是不会创建任何匿名类的
        SAM构造方法：显示地把lambda转换成函数式接口
            SAM构造方法是编译器生成的函数，用于执行从lambda到函数式接口实例的显式转换
            Runnable { ... }，这就是SAM构造方法，否则就需要使用对象声明来实例化接口
            val listener = OnClickListener { v -> ...}，SAM转换
    带接收者的lambda：with 与 apply
        Kotlin标准库中的with函数和apply函数
        Kotlin的lambda的独特功能：在lambda函数体内可以调用一个不同对象的方法，而且无需借助任何额外限定符
        with 函数：
            用它对同一个对象执行多次操作，而不需要反复把对象名称写出来
            with(target) {this.xx()  xx() xx()}
            with内部可以直接使用target的方法（或使用this指代）
            with实际上是 with(target, {...})
            带接收者的lambda和扩展函数，this指向的是函数接收者
            带接收者的lambda是类似扩展函数的定义行为的方式
            方法名称冲突时，可以使用 this@outClass.function()，这种方式指明要调用的类
            with返回的值是执行lambda代码的结果，最后一个表达式的值
        apply 函数：
            apply与with几乎一模一样，唯一的区别是apply始终返回接收者对象
            TextView(context).apply{
                text = "xxxx"
                textSize = 20.0
            }.setGravity(0)     //apply返回对象
            带接收者的lambda是构建DSL的好工具
            with、apply，带接收者的lambda？？ P134

#### Kotlin 的类型系统

    可空性：
        是Kotlin中避免空指针的特性
        Kotlin中是把运行时的错误转变为编译期的错误
        可空类型：
            Kotlin对可空类型的显式支持，null也是一种类型（空类型）
            fun func(s: String?)，表示可以传所有可能的实参，包括null
            ? 表示参数可为空，可以加在任何类型的后面表示这个类型的变量可以存储null引用
            调用可空类型的方法，要使用 ?. 或 !!.，与非空类型之间不能赋值、传参等
            可空类型可以和null进行比较，并且在比较发生的作用域内把这个值当作非空来对待
            if(s != null) s.length，直接当作非空对象操作
            处理可空性的工具
        类型的含义：
            类型就是数据的分类，决定了该类型可能的值，以及该类型的值上可以完成的操作
        安全调用运算符：?.
            允许把一次null检查和一次方法调用合并成一个操作
            foo?.bar() --> if(foo!=null) foo.bar() else null，为null的话这次调用不会发生
            上面的调用的结果也是可空类型
        Elvis运算符：?:
            提供代替null的默认值的运算符，被称作Elvis运算符（null合并运算符）
            var s: String?， val t: String = s ?: ""
            Elvis运算符接收两个运算符，第一个不为null，结果就是第一个（s），如果第一个为null，结果就是第二个（""）
            与return、throw一起使用，return、throw放在右边
        安全转换：as?
            安全转换运算符
            as? 运算符尝试把值转换为指定的类型，如果值不是合适的类型就返回null
            foo as? Type， Type or null
            val other = o as? Person ?: return false
        非空断言：!!
            非空断言可以把任何值转换成非空类型，null值会抛出异常
            可以把可空实参转换为非空
            val value = foo.bar ?: return，为空则提前返回，可用来代替非空断言
        let 函数：
            将一个可空值作为实参传递给一个只接收非空值的函数的应用场景
            let函数做的事情就是把一个调用它的对象变成lambda表达式的参数
            结合安全调用语法，它能有效地把调用let函数的可空对象转变成非空类型
            foo?.let { it... }，let内调用函数
        延迟初始化的属性：
            一个属性最终是非空的，但不能使用非空值在构造方法中初始化
            lateinit修饰符声明属性可以延迟初始化
            延迟初始化的属性都是var，因为需要在构造方法外修改它的值
            lateinit var service: Service
            如果延迟初始化的属性在被初始化之前就访问了，则会抛出异常
            依赖注入使用lateinit
        可空类型的扩展：
            定义可空类型的扩展函数，更强大的处理null值的方式
            允许接收者为null的调用，并在该函数中处理处理null，只有扩展函数才能做到（普通成员函数当实例为null时不会调用方法）
            不需要安全访问，可以直接调用为可空接收者声明的扩展函数，这个函数会处理可能的null值
            fun foo(s: String?) = s.isNullOrBlank()
            let函数必须使用安全调用符
        类型参数的可空性：
            即使不用问号结尾，类型参数也能是可空的
            Kotlin中所有泛型类和泛型函数的类型参数默认都是可空的
            fun <T> bar(t: T) { t?.hashCode() }，t可能为null，所以必须使用安全调用
            fun <T: Any> bar(t: T) { t.hashCode() }，指定非空上界，那么泛型会拒绝可空值作为实参
        可空性和Java：
            来自Java代码的类型是另一种可空性的特例
            根据Java类型的注解，Java类型会在Kotlin中表示为可空类型和非空类型（@Nullable、@NotNull）
            没有注解的情况下，Java类型会变成Kotlin中的平台类型
            平台类型：
                平台类型本质上就是Kotlin不知道可空性信息的类型，可以当作可空，也可以是非空类型处理
                使用Java代码时最好根据情况加上null检查
                String!，错误信息中出现这种表示它来自Java代码的平台类型
            继承：
                Kotlin中重写Java方法时，可以把参数和返回类型定义成可空的或者是非空的
                根据具体情况使用合适的类型，否则会出错
    基本数据类型和其他基本类型：
        Kotlin不区分基本类型和包装类，Object、Void（Java类型）和Kotlin类型的对应关系
        基本数据类型：Int、Boolean及其他
            Java的基本数据类型的变量直接存储它的值，引用类型的变量存储的是指向包含该对象的内存地址的引用
            Kotlin不区分基本数据类型和包装类型，使用同一个数据类型
            运行时，数字类型会尽可能地使用最高效的方式来表示
            变量、属性、参数和返回类型 大多数情况下会被编译为Java的基本类型，泛型类型参数会被编译为包装类型
            整数类型：Byte、Short、Int、Long
            浮点数类型：Float、Double
            字符类型：Char
            布尔类型：Boolean
        可空的基本数据类型：Int?、Boolean?及其他
            Kotlin中的可空类型（基本类型）会被编译成对应的包装类型
            要高效地存储基本数据类型元素的大型集合，可使用数组，最好不要使用Collection（包装类，产生大量对象）
        数字转换：
            处理数字转换的方式，Kotlin不会自动地把数字从一种类型转换为另一种，必须显式地进行转换
            i.toLong()，每一种基本数据类型都定义有转换函数，还支持双向转换
            toByte()、toShort()、toChar()，双向转换：大范围截取到小范围，小范围扩展到大范围
            equals 类型相同时才能比较
            Long类型：13L，Double类型：0.12、2.0，Float类型：123.4f、.456F
            字符串转换："".toInt()、"".toByte() 等等
            算术运算符可以接收所有适当的数字类型，val l = 4f + 5L
        Any 和 Any?：根类型
            Any类型是Kotlin所有非空类型的超类型（根，包括基本数据类型），Java中Object是所有引用类型的超类型（根）
            Any是非空类型，不可以持有null
            Any?是可空类型的根，可以持有null
            Any对应Java中的Object，包含toString、equals、hashCode，但不能使用wait、notify（可以通过手动转换后调用）
        Unit类型：Kotlin的 void
            Unit类型完成Java中void一样的功能，空返回值
            fun f(): Unit { ... }
            Unit是一个完备的类型，可以作为类型参数，而void不行
            泛型中可以使用Unit，且在函数中会被隐式地返回
            interface Processor<T> { fun process(): T }
            class NRProcessor : Processor<Unit> { override fun process() {} }
            Java中使用Void类型对应上面的情况，但必须有显式的return语句（return null）
            函数式编程中，Unit习惯被用来表示 只有一个实例
        Nothing类型：这个函数永不返回
            不需要返回的函数
            fun fail(msg: String): Nothing {
                throw Exception(msg)
            }
            Nothing类型没有任何值，只有在作为函数返回值类型或者被当作泛型函数的返回值类型参数使用时才有意义
            编译器认为Nothing返回类型的函数从不正常终止

    集合与数组：
        Kotlin以Java集合库为基础构建，通过扩展函数增加特性来增强它
        可空性和集合：
            集合是否可以持有null元素，类型在被当作类型参数时也可以用可空标记
            ArrayList<Int?>，创建包含可空Int值的列表，及Int或null，String.toIntOrNull
            ArrayList<Int>?，包含Int的可空列表，集合是可空的，但元素不能为null
            ArrayList<Int?>?，集合本身和元素都可为null，filterNotNull，过滤null值
        只读集合与可变集合：
            Kotlin的集合把访问集合数据的接口和修改集合数据的接口分开了（与Java集合的不同）
            kotlin.collections.Collection，这个接口只能执行从集合中读取数据的操作（遍历）
            kotlin.collections.MutableCollection，这个接口可以修改集合中的数据（添加、移除、清空操作）
            防御式拷贝（在传递参数前，先拷贝一份再传递）
            只读集合不一定是不可变的（两个不同的引用（只读和可变集合）指向同一个集合对象）
            只读集合并不总是线程安全的
        Kotlin集合和Java：
            每一个Kotlin接口都是其对应Java集合接口的一个实例
            每一种Java集合接口再Kotlin中都有两种表示：一种是只读的，另一种是可变的
            不同类型集合有不同的创建函数：mutableListOf、hashSetOf、sortedMapOf，等等
            setOf、mapOf、listOf 返回的是不可变集合
            Kotlin的不可变集合传递到Java函数时，集合可能会发生改变（要注意这种情况，如：使用可变集合）
        作为平台类型的集合：
            Kotlin处理Java代码中声明的集合
            平台类型的集合本质上就是可变性未知的集合--Kotlin将其视为只读或者可变的
            使用时考虑：集合是否可空？集合中的元素是否可空？会不会修改集合？
            默认情况下，应该优先使用集合而不是数组
        对象和基本数据类型的数组：
            fun main(args: Array<String>) {
                for (i in args.indices) { args[i] }
            }
            Kotlin中的数组是一个带有类型参数的类，其元素类型被指定为相应的类型参数
            创建数组：
                arrayOf、arrayOfNulls（创建给定大小的数组，包含null元素，只能创建可空类型的元素的数组）
                Array构造方法，接收数组大小和一个lambda，调用lambda表达式来创建每一个数组元素，这是使用非空元素类型但不用显式地传递每个元素来初始化数组
                val letters = Array<String>(26) {i-> ('a'+i).toString() }，i是数组元素的下标，返回对应下标的值
            集合转换为数组：list.toTypedArray()
            数组类型的类型参数始终会变成对象类型
            基本数据类型的数组：IntArray，对应Java的int[]；还有ByteArray、CharArray等
            Array<Int>将会是一个装箱类型的数组（Integer[]）
            创建一个基本数据类型的数组：
                构造方法接收size并返回一个使用对应基本数据类型默认值初始化好的数组（IntArray(5)）
                工厂函数接收变长参数的值并创建存储这些值的数组（IntArray、intArrayOf等）
                构造方法，接收大小和用来初始化元素的lambda，IntArray(5) { i->i*i }
                数组或集合用对应的转换函数转换为基本数据类型的数组，如：toIntArray
            和集合相同的用于数组的扩展函数
            filter、map等也适用于数组（包括基本数据类型的数组），这些方法的返回值是列表不是数组
            array.forEachIndexed { index, element -> "$index is: $element" }


#### 运算符重载及其他约定

    通过调用自己代码中定义的函数，来实现特定语言结构
    Kotlin中这些功能与特定的函数命名相关，而不是与特定的类型绑定（约定）
    可以把任意约定方法定义为扩展函数，从而适应任何现有的Java类而不用修改其代码

    重载算术运算符：
        重载二元算术运算：
            data class Point(x:Int, y:Int) {
                operator fun plus(other: Point): Point{
                    return Point(x + other.x, y + other.y)
                }
            }
            Point(1,2)+Point(1,2)，可以使用+直接相加
            用于重载运算符的所有函数都需要用 operator 关键字标记，表示把这个函数作为相应的约定的实现，而不是一个同名函数
            a+b -> a.plus(b)，+运算将会转换为plus函数的调用
            还可以把运算符定义为扩展函数，operator fun Point.plus(p:Point): Point {...}
            Kotlin限定了能重载的运算符（不能定义自己的运算符），二元运算符的对应函数：
                a*b     times,  a/b     div
                a%b     mod,    a+b     plus
                a-b     minus
            运算符的优先级不变，定义运算符不要求两个运算数是相同的类型
            Kotlin运算符不自动支持交换性（a+b，b+a，不同的类型之间），需要两种类型之间都定义运算符
            运算符函数的返回类型也可以不同于任一运算数类型
            可以重载operator函数（同名不同参数类型）
            Kotlin提供用于执行位运算的完整函数列表（中缀调用语法的常规函数）：
                shl---带符号左移，    shr---带符号右移
                ushr--无符号右移，    and---按位与
                or----按位或，        xor---按位异或
                inv---按位取反，用法：print(0 and 1)
        重载复合赋值运算符：
            +=、-=等这些运算符被称为复合赋值运算符
            返回值为Unit，名为plusAssign的函数，对应+=运算符
            minusAssign、timesAssign、modAssign 分别对应 -=、*=、%=，等等
            operator fun <T> MutableCollection<T>.plusAssign(element: T) {
                this.add(element)
            }
            上面的可变列表可以使用 list+=e，这种方式向列表添加元素
            +=，可以被转化为plus 或者 plusAssign函数的调用
            plus和plusAssign会重复，只需定义一个即可
            对不可变类应该只提供返回新值的运算（plus），可变类只需提供plusAssign和类似的运算
        重载一元运算符：
            重载过程同上面
            operator fun Point.unaryMinus(): Point {
                return Point(-x, -y)
            }
            -Point，坐标取反
            重载一元运算符的函数没有参数，所有可以重载的一元运算符：
                +a      unaryPlus，  -a   unaryMinus
                !a      not，        ++a，a++   inc
                --a，a--   dec
            inc、dec的语义与普通数字类型的自增、自减运算符相同
    重载比较运算符：
        Kotlin中可以对任何对象使用比较运算符（==、!=、>、<等）
        支持这些运算符的约定
        等号运算符：equals
            ==运算符会被转换成 equals，!=运算符也会被转换成equals（结果是相反的）
            ==、!= 可以用于可空运算数（会检查运算数是否为null）
            恒等运算符（===）用于检查对象是否相等（是否是同一个对象的引用）
            ===不能被重载
            operator修饰符适用于所有实现或重写它的方法（equals在Any中被标记）
        排序运算符：compareTo
            Kotlin支持Comparable接口（compareTo方法可以按约定调用）
            比较运算符（<、>、<=、>=）的使用将被转换为compareTo，compareTo的返回类型必须为Int
            p1 < p2 等价于 p1.compareTo(p2) < 0
            通过实现Comparable接口就可以使用，operator修饰符已经被用在基类的接口中
            Kotlin标准库的compareValuesBy函数来简化比较
    集合与区间的约定：
        通过下标来获取和设置元素，以及检查元素是否属于当前集合，这些操作都支持运算符语法
        a[b]，下标运算符， in 运算符检查元素是否在区间

        通过下标来访问元素：get 和 set
            Kotlin中可以用访问数组的方式访问和（改变可变）map，如：val value = map[key]，mutableMap[key]=value
            下标运算符是一个约定
            使用下标运算符读取元素会被转换为get运算符方法的调用，写入元素调用set
            operator fun Point.get(index: Int): Int {
                return when(index) {
                    0->x        1->y        else->throw IndexOutOfBoundsException("")
                }
            }
            get的参数可以是任何类型，不只是Int，还可以定义具有多个参数的get方法
            可以定义多个重载的get方法
            set方法用于可变的类
            operator fun MutablePoint.set(index: Int, value: Int) {
                when(index) {
                    0->x        1->y        else->throw IndexOutOfBoundsException("")
                }
            }
            mutableP[1]=44
            x[a, b]=c -> x.set(a, b, c)
            set的最后一个参数用来接收赋值语句右边的值，其他参数作为方括号内的下标
        in 的约定：
            in运算符，用于检查某个对象是否属于集合，对应的函数叫作contains
            operator fun Rectangle.contains(p: Point): Boolean {
                return p.x in lt.x until rb.x && p.y in lt.y until rb.y
            }
            point in rectangle
            in右边的对象将会调用contains函数，左边的对象作为函数参数
            标准库的until函数，用来构建开区间（不包括最后一个点），使用in来检查是否属于这个区间
            10..20，闭区间（包含20）
        rangeTo的约定：
            创建区间，..运算符是调用rangeTo函数的一个简洁用法
            1..20 -> 1.rangeTo(20)，rangeTo返回一个区间
            如果一个类实现了Comparable接口，那么可以直接使用标准库的rangeTo函数：
            operator fun <T: Comparable<T>> T.rangeTo(that: T): ClosedRange<T>
            区间可用来检测其他一些元素是否属于它
            算术运算符的优先级高于rangeTo运算符
        在for循环中使用iterator的约定：
            for(x in list)，将被转换成 list.iterator()
            iterator也是一种约定
            operator fun CharSequence.iterator(): CharIterator
            即对象得支持迭代
            operator fun ClosedRange<S>.iterator(): Iterator<S> = object: Iterator<S> {...}
    解构声明和组件函数：
        解构声明，允许你展开单个复合值，并使用它来初始化多个单独的变量
        val p = Point(10, 20)
        val (x, y) = p  //展开到x、y中
        括号中的就是解构声明，它也用到了约定的原理
        要在解构声明中初始化每个变量，将调用名为 componentN 的函数，N是声明中变量的位置，如：x=p.component1()
        编译器为数据类的主构造方法中声明的属性生成一个componentN函数
        非数据类需要手动声明：
        operator fun component1() = x
        operator fun component2() = y
        解构声明主要使用场景之一就是从一个函数返回多个值（可以定义一个数据类保存要返回的值，将它作为函数的返回类型，调用函数后就可以用解构声明的方式来展开）
        componentN函数在数组和集合上也有定义， val (name, ext) = str.split('.', limit=2)
        解构声明只允许访问一个对象的前5个元素
        标准库的Pair 和 Triple类 也能实现让一个函数返回多个值

        解构声明和循环：
            解构声明还可以用在其他可以声明变量的地方，如：in循环中
            for((key, value) in map) { ... }
    重用属性访问的逻辑：委托属性
        Kotlin中最独特和最强大的功能之一：委托属性
        委托是一种设计模式，操作的对象不用自己执行，而是把工作委托给另一个辅助的对象，把辅助对象称为委托
        委托模式应用于属性时，将访问器的逻辑委托给一个辅助对象
        委托属性的基本操作：
            class Foo {
                var p: Type by Delegate()   //基本语法
            }
            属性p将它的访问器逻辑委托给Delegate对象，通过关键字by对其后的表达式求值来获取这个对象
            by可以用于任何符合属性委托约定规则的对象
            编译器会自动生成一个辅助属性，委托属性的访问都会调用对应委托对象的getValue、setValue方法
            Delegate类必须具有getValue和setValue方法（不是属性的get、set），可以是成员函数或扩展函数
        使用委托属性：惰性初始化和 by lazy()
            惰性初始化，直到第一次访问该属性时才创建对象
            支持属性技术（两个属性，一个保存值，一个访问值，非线程安全）
            class P() {
                val email by lazy { loadEmail() }
            }
            标准库函数lazy返回的委托
            lazy函数返回一个对象，该对象具有一个getValue且签名正确的方法，与by一起使用创建委托属性
            lazy的参数是lambda，用来初始化这个值，默认情况下lazy函数是线程安全的
        实现委托属性（P200）：
            KProperty类型，使用KProperty.name访问属性的名称
            class Person(age:Int, salary:Int) {
                var age: Int by ObservableProperty(age, changeSupport)
                var salary: Int by ObservableProperty(salary, changeSupport)
            }
            by 右边的对象被称为委托，Kotlin会自动将委托存储在隐藏的属性中，并在访问或修改属性时调用委托的getValue和setValue
            by右边的表达式不一定是新创建的实例，也可以是函数调用、另一个属性或任何其他表达式
            被委托的对象需要有getValue和setValue（约定，operator标记的方法），如：Delegates.observable(x,xx)
        委托属性的变换规则：
            class C {
                var prop: Type by Delegate()
            }
            Delegate实例会被保存到一个隐藏的属性中，它被称为<delegate>，编译器也将用一个KProperty类型的对象来代表这个属性，它被称为<property>
            编译器生成的代码：
            class C {
                private val <delegate> = Delegate()
                var prop: Type
                    get() = <delegate>.getValue(this, <property>)
                    set(value: Type) = <delegate>.setValue(this, <property>, value)
            }
            当访问属性时，会调用<delegate>的getValue 和 setValue 函数
        在map中保存属性值：
            委托属性的另一种常见用法是用在有动态定义的属性集的对象中，这样的对象有时被称为自订（expando）对象
            使用委托属性把值存到map中
            class P {
                private val _attr = hashMapOf<String, String>()
                fun setAttr(attrName: String, value: String) { _attr[attrName] = value }
                val name: String by _attr   //把map作为委托属性
            }
            标准库已经在标准Map和MutableMap接口上定义了getValue和setValue扩展函数
        框架中的委托属性：
            委托属性的使用，通过委托可以做很多事情，如 修改数据库等
            委托属性可以用来重用逻辑，这些逻辑控制如何存储、初始化、访问和修改属性值

#### 高阶函数：Lambda作为形参和返回值

    高阶函数--属于自己的，使用lambda作为参数或者返回值的函数
    内联函数--Kotlin的一个强大特性，能够消除lambda带来的性能开销
    声明高阶函数：
        高阶函数就是以另一个函数作为参数或返回值的函数
        函数可以用lambda或者函数引用来表示
        任何以函数作为参数的函数，或者返回值为函数的函数，或者都满足的函数都是高阶函数
        标准库中的filter、map、with等等都是高阶函数
        函数类型：
            val sum = { x: Int, y: Int -> x+y }
            编译器推导出 sum 具有函数类型
            sum的显式类型声明：val  sum: (Int, Int) -> Int = { x, y -> x+y }
            声明函数类型，需要将函数参数类型放在括号中，紧接着是箭头和函数的返回类型：()->Unit
            在声明普通函数时，Unit类型的返回值可以省略，但是函数类型声明总是需要一个显式的返回类型，Unit不能省略
            lambda中的 -> 箭头后面是返回值，没有返回值可以不要箭头
            var funOrNull: ((Int, Int)-> Int)? = null，函数类型的可空变量（变量本身可空，而不是函数类型的返回类型可空）
            var canReturnNull: (Int, Int)-> Int? = {null}，返回值可空的函数类型（注意与上面的区别）
            fun func(url: String, callback: (code: Int, content: String) -> Unit)，高阶函数的声明
            函数参数的参数名可以指定，使用时可以使用指定的名称，也可以改变参数的名称
        调用作为参数的函数：
            fun mathFunc(operation: (Int, Int) -> Int) {
                val result = operation(2, 3) //函数参数的调用
            }
            mathFunc { a, b -> a*b }，函数实现
            fun String.filter(predicate: (Char) -> Boolean): String {
                if (predicate('e'))
                    return "e $this"
            }
            filter函数以一个判断式作为参数
            "ablsc".filter { it in 'a'..'z' }   //it只有一个固定值 e，是在上面的实现中传递的
            调用filter时的lambda只是对filter函数中传递的值的处理（函数内部逻辑），函数类型的参数已经在上面传递了
        在Java中使用函数类：
            函数类型被声明为普通的接口：一个函数类型的变量是FunctionN接口的一个实现
            Kotlin标准库定义的接口对应不同参数数量的函数：Function0<R>（没有参数的函数）、Function1<P1,R>（一个参数的函数）等等，每个接口都有invoke方法，调用该方法就会执行函数
            一个函数类型的变量就是实现对应的FunctionN接口的实现类的实例，invoke方法包含了lambda函数体
            Java8的lambda会被自动转换为函数类型的值
            Java中使用Kotlin标准库中的函数，必须要显式地返回一个Unit类型的值
            CollectionsKt.forEach(list, s -> {
                ...
                return Unit.INSTANCE;
            });
            一个返回void的lambda不能作为返回Unit的函数类型的实参，因为Kotlin中Unit类型是有值的，需要显式地返回它
        函数类型的参数默认值和null值：
            声明函数类型的参数的时候可以指定参数的默认值
            fun <T> Collection<T>.joinStr(prefix: String=" ", transform: (T) -> String = { it.toString()}): String
            声明一个参数为可空的函数类型，需要检查null
            fun foo(callback: (() -> Unit)? = null) {   //声明可空函数参数
                if(callback != null)
                    callback()
                callback?.invoke()  //invoke中需要传递参数
            }
            函数类型是一个包含invoke方法的接口的具体实现，作为普通方法，可以通过安全调用语法被调用
        返回函数的函数：
            定义一个函数选择恰当的逻辑变体并将它作为另一个函数返回
            fun func(num: Int): (String) -> Int {
                return { str -> "0x11$str".toInt() }    //str是要返回的函数的入参
            }
            val shift = func(9)     //返回函数
            val num = shift('11')   //调用返回的函数
            高阶函数是一个改进代码结构和减少重复代码的利器
        通过lambda去除重复代码：
            函数类型和lambda表达式一起组成了一个创建可重用代码的好工具
            函数类型可以帮助去除重复代码
            使用lambda不仅可以抽取重复的数据，也可以抽取重复的行为
            设计模式（策略模式）可以使用lambda表达式来简化
            P217
            高阶函数的性能
    内联函数：消除lambda带来的运行时开销
        lambda表达式会被正常地编译成匿名类（没调用一次lambda表达式就会创建一个额外的类，如果lambda捕捉了某个变量，那么每次调用都会创建一个新的对象，这会带来运行时的额外开销，效率低）
        使用inline修饰符标记一个函数，函数被使用的时候编译器并不会生成函数调用的代码，而是使用函数实现的真实代码替换每一次的函数调用
        内联函数如何运作：
            当函数被声明为inline时，函数体是内联的--函数体会被直接替换到函数被调用的地方，而不是被正常调用
            inline fun <T> synchronized(lock: Lock, action: () -> T): T
            Kotlin标准库中的withLock函数（加锁）
            lambda表达式和synchronized函数的实现都会被内联
            由lambda生成的字节码成为了函数调用者定义的一部分，而不是被包含在一个实现了函数接口的匿名类中
            调用内联函数的时候可以传递函数类型的变量作为参数
            函数类型的变量的lambda代码在内联函数被调用点是不可用的，因此并不会被内联，只有内联函数被内联了，lambda才会被正常调用
            即：传递的函数类型变量的内容不会被内联（复制内容到函数体），只有一个函数调用（见：P220）
        内联函数的限制：
            不是所有使用lambda的函数都可以被内联
            当函数被内联的时候，作为参数的lambda表达式的函数体会被直接替换到最终生成的代码中，这将限制函数体中的对应（lambda）参数的使用
            （lambda）参数如果被直接调用或者作为参数传递给另外一个inline函数，它是可以被内联的
            如果一个函数期望有多个lambda参数，可以选择只内联其中一些参数，因为一个lambda可能会以不允许内联的方式使用
            接收非内联lambda的参数，可以用noinline修饰符来标记
            inline fun foo(inlined: () -> Unit, noinline notInlined: () -> Unit)
        内联集合操作：
            Kotlin标准库中操作集合的函数的性能
            标准库中的集合函数大多都带有lambda参数，它们都是内联函数，大致无性能消耗
            用来处理序列的lambda没有被内联，每一个中间序列被表示成把lambda保存在其字段中的对象
            asSequence只在处理大量数据的集合时有用，小的集合可以用普通的集合操作处理
        决定何时将函数声明成内联：
            使用inline关键字只能提高带有lambda参数的函数的性能
            对于普通的函数调用，JVM已经提供了强大的内联支持（自动将函数调用内联）
            字节码中每一个函数的实现只会出现一次，而Kotlin的内联函数在每个调用的地方都拷贝一次
            将带有lambda参数的函数内联能带来的好处：
                通过内联避免运行时开销，不仅节约函数调用的开销，而且节约了为lambda创建匿名类，以及创建lambda实例对象的开销
                JVM并不会总是将函数调用内联
                内联可以使用一些不可能被普通lambda使用的特性，比如非局部返回
            使用inline的时候，要注意代码长度，内联函数应总是很小的
            应该将与lambda参数无关的代码抽取到一个独立的非内联函数中
        使用内联lambda管理资源：
            资源管理（lambda去重的常见模式），先获取一个资源，完成操作，然后释放资源
            资源可以是 一个文件、一个锁、一个数据库事务等
            使用的这个模式的标准做法是使用try/finally语句，资源在try代码块之前被获取，在finally代码块中被释放
            fun <T> Lock.withLock(action: () -> T): T {
                lock()
                try {
                    return action()
                } finally {
                    unlock()
                }
            }
            Kotlin标准库中的use函数，类似 try-with-resource
            use函数被用来操作可关闭的资源，它接收lambda作为参数（use会确保资源被关闭）
    高阶函数中的控制流：
        return的使用
        lambda中的返回语句：从一个封闭的函数返回
            如果在lambda中使用return关键字，它会从调用lambda的函数中返回，并不只是从lambda中返回，这样的return语句叫作非局部返回
            只有在以lambda作为参数的函数是内联函数的时候才能从更外层的函数返回
            在一个非内联函数的lambda中使用return表达式是不允许的
        从lambda返回：使用标签返回
            可以在lambda表达式中使用局部返回
            lambda中的局部返回跟for循环中的break相似，它会终止lambda的执行，接着从调用lambda的代码处执行
            局部返回使用标签
            fun foo() {
                list.forEach label@{
                    return@label        //引用标签
                }
                ...
            }
            标签名可以是任何名称
            可以使用lambda作为参数的函数名作为标签
            fun foo() {
                list.forEach {
                    return@forEach        //引用标签
                }
                ...
            }
            一个lambda表达式的标签数量不能多于1个
            带标签的this表达式
            带接收者的lambda--包含一个隐式上下文对象的lambda可以通过一个this引用去访问
            如果给带接收者的lambda指定标签，就可以通过对应的带有标签的this表达式访问它的隐式接收者
            StringBuilder().apply sb@{
                listOf(1,2,3).apply { this@sb.append(this.toString()) }     //list和sb的引用
            }
            this指向作用域内最近的隐式接收者，所有隐式接收者都可以访问，外层的接收者通过显式的标签访问
        匿名函数：默认使用局部返回
            如果一个lambda包含多个返回语句，可以使用匿名函数
            匿名函数是一种用于编写传递给函数的代码块的方式
            fun foo() {
                list.forEach(fun (data) {   //匿名函数取代lambda表达式
                    if(data) return     //return 指向最近的函数：一个匿名函数
                })
            }
            list.filter(fun (data): Boolean { return false })
            匿名函数和普通函数有相同的规则，除了没有函数名和参数类型
            return从最近的使用fun关键字声明的函数返回，lambda没有使用fun关键字，所以从外层返回
            匿名函数是lambda表达式的另一种语法形式

#### 泛型

    Kotlin引入的新概念：实化类型参数和声明点变型
    实化类型参数允许在运行时的内联函数调用中引用作为类型实参的具体类型（普通函数和类不行，类型实参在运行时会被擦除）
    声明点变型可以说明一个带类型参数的泛型类型，是否是另一个泛型类型的子类型或者超类型，它们的基础类型相同但类型参数不同（和Java通配符一样的效果）

    泛型类型参数：
        泛型允许定义带类型形参的类型，这种类型的实例被创建出来的时候，类型形参被替换成称为类型实参的具体类型
        List<T> T为类型形参，List<String> String为类型实参，创建列表时必须指明类型实参
        Kotlin要求泛型的类型实参要么被显式地说明，要么能被编译器推导出来
        泛型函数和参数：
            fun <T> List<T>.slice(indices: IntRange): List<T>       //返回指定下标区间内的元素
            大部分情况下编译器会推导出类型，不需要传递类型实参
            val <T> List<T>.penultimate: T  //泛型的扩展属性
            不能声明泛型非扩展属性，普通属性不能拥有类型参数，不能在一个类的属性中存储多个不同类型的值
            可以给类或接口的方法、顶层函数以及扩展函数声明类型参数
        声明泛型类：
            interface List<T> {
                operator fun get(index: Int): T
            }
            一个类可以把它自己作为类型实参引用，如：实现Comparable接口的类
        类型参数约束：
            类型参数约束可以限制作为（泛型）类和（泛型）函数的类型实参的类型
            如果把一个类型指定为泛型类型形参的上界约束，在泛型类型初始化时，其对应的类型实参就必须是这个具体类型或者它的子类型
            fun <T: Number> List<T>.sum(): T    //Java: <T extends Number> T sum(List<T> list)
            Number就是 T 的上界，函数中可以调用上界的方法
            fun <T> foo(seq: T) where T: CharSequence, T: Appendable {}
            为一个类型参数指定多个约束，T必须实现两个接口，且方法中可以调用两个接口的方法
        让类型形参非空：
            没有指定上界的类型形参将会使用Any? 这个默认的上界
            class Processor<T> {
                fun process(value: T)   //这里value是可空的，要使用安全调用
            }
            要保证替换类型形参的始终是非空类型，可以指定一个约束来实现
            class Processor<T : Any> {
                fun process(value: T)   //这里value是非空的
            }
            指定非空上界，可以通过指定任意非空类型作为上界，来让类型参数非空，不光是类型Any
    运行时的泛型：擦除和实化类型参数
        JVM上的泛型一般是通过类型擦除实现的
        通过将函数声明为inline来解决其局限性
        声明一个inline函数，使其类型实参不被擦除（Kotlin中称作实化）

        运行时的泛型：类型检查和转换
            Kotlin的泛型在运行时也被擦除了，意味着泛型类实例不会携带用于创建它的类型实参的信息
            is检查中不可能使用类型实参中的类型（类型被擦除了）
            检查一个值是否是列表还是set等，可以使用特殊的星号投影语法：
            if(value is List<*>) {...}
            泛型类型拥有的每个类型形参都需要一个*（可以认为是拥有未知类型实参的泛型类型，类比于Java的List<?>）
            c as? List<Int>，会有Unchecked cast警告，如果实参类型不匹配，会抛出ClassCastException
            在编译期已经知道相应的类型信息时，is检查是允许的
        声明带实化类型参数的函数：
            泛型类、泛型函数在调用时不能决定调用它用的类型实参，内联函数可以避免这种限制
            内联函数的类型形参能够被实化，可以在运行时引用实际的类型实参
            inline fun <reified T> isA(value: Any) = value is T
            标准库函数filterIsInstance是实化类型参数的例子
            inline fun <reified T> Iterable<*>.filterIsInstance(): List<T>
            reified声明了类型参数不会在运行时被擦除，函数体中可以检查元素是不是指定为类型实参的类的实例
            内联函数生成的字节码引用了具体类，而不是类型参数，它不会被运行时发生的类型参数擦除影响
            带reified类型参数的inline函数不能在Java代码中调用
            内联函数可以有多个实化类型参数（reified），也可以同时拥有非实化类型参数
            内联函数过大时，最好把不依赖实化类型参数的代码抽取到单独的非内联函数中
        使用实化类型参数代替类引用：
            Service::class.java 与 Service.class（Java中）是完全等同的
            inline fun <reified T> loadService() { return ServiceLoader.load(T::class.jav) }
            val impl = loadService<Service>()
            实化类型参数T 可以当成类型参数的类来访问
            startActivity也可以这样使用
        实化类型参数的限制：
            使用实化类型参数的方式：
                用在类型检查和类型转换中（is、!is、as、as?）
                使用Kotlin反射API（::class）
                获取相应的java.lang.class（::class.java）
                作为调用其他函数的类型实参
            不能使用的情况：
                创建指定为类型参数的类的实例
                调用类型参数类的伴生对象的方法
                调用带实化类型参数函数的时候使用非实化类型形参作为类型实参
                把类、属性或者非内联函数的类型参数标记成reified
            实化类型参数只能用在内联函数上
            使用实化类型参数意味着函数和所有传给它的lambda都会被内联
    变型：泛型和子类型化
        变型描述了拥有相同基础类型和不同类型实参的（泛型）类型之间是如何关联的
        为什么存在变型：给函数传递实参
            期望是MutableList<Any>的时候把MutableList<String>当作实参传递是不安全的，Kotlin编译器禁止了（Java可以）
            在函数中添加或替换列表中的元素是不安全的，这样可能会产生类型不一致的可能性，否则就是安全的
        类、类型和子类型：
            类型和类是不一样的
            非泛型类，类的名称可以直接当作类型使用
            每一个泛型类都可能生成潜在的无限数量的类型
            子类型，Int是Number的子类型，Int不是String的子类型，任何类型都可以被认为是它自己的子类型
            超类型是子类型的反义词
            只有值的类型是变量类型的子类型时，才允许变量存储该值
            子类型和子类本质上意味着一样的事物，可空类型 说明子类型和子类不是同一个事物
            如果对任意两种类型A和B，List<A>既不是List<B>的子类型也不是它的超类型，它就被称为在该类型参数上是不变型的
            如果A是B的子类型，那么List<A>就是List<B>的子类型，这样的类或接口被称为协变的
        协变：保留子类型化关系
            一个协变类是一个泛型类
            如果A是B的子类型，那么List<A>就是List<B>的子类型，这就说子类型化被保留了
            Kotlin中声明类在某个类型参数上是可以协变的（使用out）：
                interface Producer<out T> { fun produce(): T }，类被声明成在T上协变
            将一个类的类型参数（T）标记为协变的（out），在该类型实参没有精确匹配到函数中定义的类型形参时，可以让该类（T）的值作为这些函数的实参传递，也可以作为函数的返回值
            不能把任何类都变成协变的（不安全）
            让类在某个类型参数变为协变，限制了该类中对该类型参数使用的可能性，要保证类型安全，它只能用在所谓的out位置，即这个类只能生产类型T的值而不能消费它
            函数把T当成返回类型，我们就说它在out位置，该函数生产类型为T的值
            函数如果把T用作函数参数的类型，它就在in位置，该函数消费类型为T的值
            类的类型参数前的out关键字要求所有使用T的方法只能把T放在out位置而不能放在in位置，out约束了使用T的可能性（保证对应子类型关系的安全性）
            类型参数T上的关键字out的含义：
                子类型化会被保留（List<A>是List<B>的子类型）
                T只能用在out位置
            只读集合（List）也是协变的（生产T）
            构造方法的参数即不在in位置，也不在out位置
            构造方法的参数T使用var时，不能协变（var有set函数，它是in位置），val可以协变（只有get）
            位置规则只覆盖类外部可见的API（public、protected和internal），私有方法的参数即不在in位置也不在out位置
            变型规则对private不限制（类内部使用），所以构造方法 private var T 是可以协变的
        逆变：反转子类型化关系
            逆变可以看成是协变的镜像（in），逆变类的子类型化关系与用作类型实参的类的子类型化关系是相反的
            interface Comparator<in T> { fun compare(e: T, r: T): Int } //out对应extends，in对应 super
            消费类型T，只在in位置使用
            一个在类型参数上逆变的类，如果B是A的子类型，那么Consumer<A>就是Consumer<B>的子类型，类型参数A和B交换了位置，子类型化被反转了
            对协变类型 子类型化保留了，对逆变类型 子类型化反转了
            约束类型参数的使用将导致特定的子类型化关系
            在类型参数T上的in关键字意味着子类型化被反转了，而且T只能用在in位置
            一个类可以在一个类型参数上协变，同时在另外一个类型参数上逆变
            interface Function<in P, out R> { operator fun invoke(p: P): R }
            Kotlin的表示法：(P) -> R，在它的参数类型上逆变而在返回类型上协变
        使用点变型：在类型出现的地方指定变型
            在类声明的时候就指定变型修饰符，这些修饰符会应用到所有类被使用的地方，这被称作声明点变型
            Java的通配符类型（? extends 和 ? super）
            Java中每次使用带类型参数的类型的时候，可以指定这个类型参数是否可以用它的子类型或超类型替换，这叫作使用点变型
            Kotlin也支持使用点变型，允许在类型参数出现的具体位置指定变型，在类型声明时不能被声明成协变或逆变的
            当函数的实现调用了类型参数只出现在out位置（或in位置）的方法时，在函数定义中给特定用途的类型参数加上变型修饰符
            fun <T> copy(source: MutableList<out T>, destination: MutableList<T>)
            fun <T> copy(source: MutableList<T>, destination: MutableList<in T>)
            类型投影，out 对应 ? extends，in 对应 ? super T
            Kotlin的使用点变型直接对应Java的限界通配符
            为整个泛型类指定变型（声明点变型），为泛型类型特定的使用指定变型（使用点变型）
        星号投影：使用*代替类型参数
            用星号投影表明不知道的关于泛型实参的任何信息
            List<*>，包含未知类型元素的列表（任意类型和某种特定类型的区别）
            Kotlin的Type<*> 对应Java的Type<?>
            类型实参的信息不重要的时候，可以使用星号投影，不需要使用任何在签名中引用类型参数的方法，或者只是读取数据而不关心它的具体类型
            把具体类型的值传给未知类型的类是不安全的
            当确切的类型实参是未知的或者不重要的时候，可以使用星号投影语法

#### 注解与反射

    声明并应用注解：
        一个注解允许你把额外的元数据关联到一个声明上，然后元数据就可以被相关的源代码工具访问（编译时、运行时等）
        应用注解：
            @Test fun test() {}
            @Deprecated 注解，Kotlin用replaceWith参数增强了它，提供一个替代者的（匹配）模式
            @Deprecated("xxx", ReplaceWith("removeAt(index)"))
            fun remove(index: Int) { ... }
            注解只能拥有的参数：基本数据类型、字符串、枚举、类引用、其他的注解类，以及前面类型的数组
            要把一个类指定为注解实参，在类名后加上 ::class， @Annotation(MClass::class)
            要把另一个注解指定为一个实参，去掉注解名称前面的@，如上面的ReplaceWith就是一个注解
            要把一个数组指定为一个实参，使用arrayOf函数，@RequestMapping(path = arrayOf("xx", "xxx"))
            要把属性当作注解实参使用，需要用const修饰符标记，来告知编译器这个属性是编译期常量
            注解实参需要在编译期就是已知的，所以不能引用任意的属性作为实参
        注解目标：
            使用点目标声明被用来说明要注解的元素，使用点目标被放在@符号和注解名称之间，并用冒号隔开
            @get:Rule，get导致注解@Rule被应用到属性的getter上
            @get:Rule
            val folder = TemporaryFolder()，注解的是getter，不是属性
            使用Java中声明的注解来注解一个属性，它会被默认地应用到相应的字段上
            Kotlin支持的使用点目标的完整列表：
                property（Java的注解不能应用这种使用点目标）
                field（为属性生成的字段）
                get（属性getter），set（属性setter）
                receiver（扩展函数或者扩展属性的接收者参数）
                param（构造方法的参数）
                setparam（属性setter的参数）
                delegate（为委托属性存储委托实例的字段）
                file（包含在文件中声明的顶层函数和属性的类）
            任何应用到file目标的注解都必须放在文件的顶层，放在package指令之前
            @JvmName是常见的应用到文件的注解之一（改变了对应类的名称，上面有示例）
            Kotlin允许对任意的表达式应用注解，而不仅仅是类和函数的声明及类型
            @Suppress注解，用它抑制被注解的表达式的上下文中的特定的编译器警告
            fun test() {
                @Suppress("UNCHECKED")
                val strs = list as List<String>
            }
            @Volatile、@Strictfp，直接充当了Java的关键字volatile和strictfp的替身（代替了Java语言中对应的关键字）
            还有一些注解被用来改变Kotlin声明对Java调用者的可见性：
                @JvmName，改变由Kotlin生成的Java方法或字段的名称
                @JvmStatic，被用在对象声明或者伴生对象的方法上，把它们暴露成Java的静态方法
                @JvmOverloads，指导Kotlin编译器为带默认参数值的函数生成多个重载函数
                @JvmField，可以应用于一个属性，把这个属性暴露成一个没有访问器的公有Java字段
        使用注解定制JSON序列化：
            注解的经典用法之一，定制化对象的序列化
            JKid库的使用，serialize()、deserialize()
            @JsonExclude注解用来标记一个属性，这个属性应该排除在序列化和反序列化之外
            @JsonName注解说明代表这个属性的（JSON）键值对之中的键应该是一个给定的字符串，而不是属性的名称
        声明注解：
            annotation class JsonExclude，声明注解类
            注解类只是用来定义关联到声明和表达式的元数据的结构，它们不能包含任何代码，编译器禁止为一个注解类指定类主体
            annotation class JsonName(val name: String)，拥有参数的注解，val 关键字是强制的
            public @interface JsonName {
                String value();     //Java中声明的注解
            }
            Java应用注解时需要提供value以外所有指定特性的显式名称，Kotlin中应用注解就是常规的构造方法调用
            Kotlin应用Java声明的注解时，必须对除了value以外的所有实参使用命名实参语法，value也会被Kotlin特殊对待
        元注解：控制如何处理一个注解
            注解类自己也可以被注解，可以应用到注解类上的注解被称作元注解（控制编译器如何处理注解）
            @Target(AnnotationTarget.PROPERTY)      //可以声明多个目标
            annotation class JsonExclude
            @Target元注解说明了注解可以被应用的元素类型，不使用，所有的声明都可以应用这个注解
            AnnotationTarget列出可以应用注解的全部可能的目标，有：类、文件、函数、属性、属性访问器、所有的表达式 等等
            AnnotationTarget.ANNOTATION_CLASS，可以用来声明自己的元注解
            AnnotationTarget.PROPERTY 在Java中不能使用，可以添加 AnnotationTarget.FIELD 支持Java
            @Retention注解，被用来说明声明的注解是否会存储到.class文件，以及在运行时是否可以通过反射来访问它
            Kotlin默认让Retention注解拥有RUNTIME保留期（Java默认在.class文件中保留注解但不会在运行时被访问到）
        使用类做注解参数：
            data class Person(val name: String, @DeserializeInterface(Company::class) val company: Company)
            annotation class DeserializeInterface(val targetClass: KClass<out Any>)，out关键字说明允许引用那些继承Any的类，而不仅仅是引用Any自己
            KClass是Java的Class类型在Kotlin中的对应类型，用来保存Kotlin类的引用
        使用泛型类做注解参数：
            @CustomSerializer注解，自定义的序列化类
            annotation class CustomSerializer(val serializerClass: KClass<out ValueSerializer<*>>)
            使用类作为注解实参时都可以这样写：KClass<out xxx>，如果xxx有自己的类型实参，就用*代替

    反射：在运行时对Kotlin对象进行自省
        反射是一种在运行时动态地访问对象属性和方法的方式，而不需要事先确定这些属性是什么
        Java反射兼容Kotlin代码
        Kotlin反射，让能访问那些在Java中不存在的概念，如属性和可空类型
        Kotlin反射API不仅限于Kotlin类，可以使用同样的API访问用任何JVM语言写成的类

        Kotlin反射API：KClass、KCallable、KFunction和KProperty
            KClass是反射的主要入口，代表一个类，可以用它列举和访问类中包含的所有声明，超类中的声明，等等
            MClass::class会给一个KClass的实例
            要在运行时取得一个对象的类，先使用javaClass属性获得Java类（等价于getClass），然后访问该类的.kotlin扩展属性，从Java切换到Kotlin的反射API
            val person = Person("Jack")
            val kClass = person.javaClass.kotlin  //返回KClass<Person>实例
            kClass.memberProperties.foreach { ... } //收集这个类、所有超类中定义的全部非扩展属性
            KClass的许多特性都声明成了扩展（memberProperties等）
            由类的所有成员组成的列表是一个KCallable实例的集合，KCallable是函数和属性的超接口，声明有call方法，允许调用对应函数或对应属性的getter
            interface KCallable<out R> {
                fun call(vararg args: Any?): R
            }
            反射调用函数时，把（被引用）函数的实参放在varargs列表中提供给它
            ::foo，这个表达式的值是来自反射API的KFunction类的一个实例，使用KCallable.call方法来调用被引用的函数
            ::foo表达式的类型是KFunction1<Int, Unit>，1代表函数有一个形参，Unit代表返回类型，1也可以是n（多个参数）
            KFunction1.invoke，可以执行foo函数，invoke接收固定数量的实参（否则编译不通过），call方法对所有类型都有效的通用手段
            call方法不提供类型安全性，invoke方法提供类型安全
            KFunctionN接口类型，称为合成的编译器生成类型，可以使用任意数量参数的函数接口
            KProperty实例上调用call方法，会调用该属性的getter，或者使用get方法
            顶层属性表示为KProperty0接口的实例，它有一个无参数的get方法
            val counter=0   val kProperty=::counter   kProperty.setter.call(2)   kProperty.get()
            成员属性由KProperty1的实例表示，它有一个单参数的get方法（参数是需要的值所属的那个对象实例）
            val memberProperty = Person::age
            memberProperty.get(person)
            memberProperty的类型是KProperty<Person, Int>，第一个类型参数表示接收者的类型，第二个参数类型代表属性的类型
            只能使用反射访问定义在最外层或者类中的属性，不能访问函数的局部变量
            所有的声明都能被注解，所有KAnnotatedElement是KClass、KFunction和KParameter的基类（三者代表声明的接口）
            KMutableProperty（KProperty的子类）表示var的可变属性，可以使用getter、setter接口，以及获取它们的注解
        用反射实现对象序列化：
            fun serialize(obj: Any): String = buildString { serializeObject(obj) }
            private fun StringBuilder.serializeObject(obj: Any) {
                val kClass = obj.javaClass.kotlin
                val properties = kClass.memberProperties
                properties.joinToStringBuilder(this, prefix="{", postfix="}"){ prop ->
                    serializeString(prop.name)
                    append(":")
                    serializePropertyValue(prop.get(obj))
                }
            }
            prop变量的类型是KProperty1<Any, *>，prop.get返回Any类型的值，是因为列举了对象类中的所有属性
            因为传递的对象和获取属性列表的对象是同一个，接收者的类型是不会错的
        用注解定制序列化：
            KAnnotatedElement接口定义了属性annotations（它是所有注解的实例组成的集合，包括运行时）
            KProperty继承了KAnnotatedElement，可以用KProperty.annotations
            KAnnotatedElement.findAnnotation，返回一个注解，其类型就是指定为类型实参的类型（如果注解存在）
            过滤@JsonExclude注解的属性：
                kClass.memberProperties.filter { it.findAnnotation<JsonExclude>() == null }
            annotation class JsonName(val name: String)
            val jsonName = prop.findAnnotation<JsonName>()
            val propName = jsonName?.name ?: prop.name  //取得JsonName的实参或者属性名称
            StringBuilder.serializeProperty(prop: KProperty1<Any, *>, obj: Any)

            annotation class CustomSerializer(val serializerClass: KClass<out ValueSerializer<*>>)
            fun KProperty<*>.getSerializer(): ValueSerializer<Any?>? {
                val customSerializerAnn = findAnnotation<CustomSerializer>() ?: return null
                val serializerClass = customSerializerAnn.serializerClass
                val valueSerializer = serializerClass.objectInstance ?: serializerClass.createInstance()
                return valueSerializer as ValueSerializer<Any?>
            }
            object拥有非空值的objectInstance属性，可以用它来访问object创建的单例实例
            objectInstance存储了DateSerializer的单例实例（object声明）
            createInstance用来创建一个新的实例（普通类，非object声明）
        JSON解析和对象反序列化：
            inline fun <reified T: Any> deserialize(json: String): T
            val book = deserialize<Book>(json)
            反序列化的三个主要阶段：词法分析器（lexer）、语法分析器（解析器）以及反序列化组件本身
            词法分析把字符串切分成由标记组成的列表
            两类标记：特殊意义的字符（,:[]{}）的字符标记；对应到字符串、数字、布尔值以及null常量的值标记
            解析器负责将无格式的标记列表转换为结构化的表示法，理解JSON的更高级别的结构，并将各个标记转化为JSON支持的语义元素（键值对、对象、数组）
            JKid将所有K-V的配对作为参数传递给要被反序列化的类的构造方法，不支持在对象实例创建后设置其属性
            种子（Seed）接口，ObjectSeed（存储了正在构造的对象的状态）
            class.getConstructorParameter(propertyName) //构造方法参数
        反序列化的最后一步：callBy()和使用反射创建对象
            KCallable.call方法，不支持默认参数值
            KCallable.callBy方法，支持默认参数值
            interface KCallable<out R> {
                fun callBy(args: Map<KParameter, Any?>): R
            }
            map将被作为参数传给这个方法，如果map中缺少形参，可行的话它的默认值将会被使用（不必按顺序）
            KParameter.type属性，可以识别类属性的类型
            反序列化一个对象时，打交道的是构造方法参数，而不是属性
            class.primaryConstructor，主构造方法
            constructor.parameters.forEach {}，遍历构造方法参数
            constructor.callBy，调用构造方法，实例化类
            param.isOptional，表示一个参数是否有默认值
            param.type.isMarkedNullable，参数类型是否是可空的（true，null会作为默认值）
            缓存的使用（ClassInfoCache）

        获取一个KClass实例，如果类是静态已知的，使用ClassName::class，否则使用obj.javaClass.kotlin从对象实例上取得类
        KFunction接口和KProperty接口都继承KCallable，通用的call方法，callBy方法用来调用带默认参数值的方法
        KFunction0、KFunction1等不同参数数量的函数可以使用invoke方法调用
        KProperty0、KProperty1是接收者数量不同的属性，支持用get方法取回值，KMutableProperty继承它们，支持set方法改变属性的值

#### DSL构建

    领域特定语言：DSL
    从API到DSL：
        Kotlin允许构建整洁API的功能的代表包括：扩展函数、中缀调用、lambda简明语法和运算符重载
        Kotlin的DSL是完全静态类型的
        领域特定语言的概念：
            通用编程语言与领域特定语言（专注在特定任务，或者说领域上）
            最常见的DSL是SQL和正则表达式
        内部DSL：
            内部DSL是通用编程语言编写的程序的一部分，外部DSL有自己的独立语法
        DSL的结构：
        使用内部DSL构建HTML：
    构建结构化的API：DSL中带接收者的lambda
        带接收者的lambda和扩展函数类型：
            fun buildString(buildAction: StringBuilder.() -> Unit): String //定义带接收者的函数类型的参数
            { StringBuilder().buildAction() } //传递StringBuilder实例作为lambda的接收者
            buildString{
                append("xxx")
            }
            省略 this，隐式引用SB
            String.(Int, Int) -> Unit，分别是接收者类型、参数类型、返回类型
            apply、with 中有带接收者的扩展函数，两者有共同之处（见源码实现）
        在HTML构建器中使用带接收者的lambda：
        Kotlin构建器：促成抽象和重用
    使用invoke约定构建更灵活的代码块嵌套：
        invoke约定允许把自定义类型的对象当作函数一样调用
        invoke约定：像函数一样可以调用的对象
            invoke约定对应 ()，get约定对应 []
            即函数调用，如：A()
            operator fun invoke(name: String)，可以传参
        invoke约定和函数式类型：
        DSL中的invoke约定：在Gradle中声明依赖
    实践中的Kotlin DSL：
        把中缀调用链接起来：测试框架中的should
        在基本数据类型上定义扩展：处理日期
        成员扩展函数：为SQL设计的内部DSL
            在类中声明扩展函数和扩展属性，这样的函数或属性既是它容器类的成员，也是某些其他类型的扩展
            这样的函数和属性叫做成员扩展
            限制它们应用的作用域
        Anko：动态创建Android UI
