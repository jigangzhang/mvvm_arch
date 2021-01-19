package com.god.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * gradle构建执行的三个生命周期：初始化--配置--执行
 * 初始化：解析settings.gradle，构造一个Settings实例（对应Settings接口），为每个包含的每个项目（include:':app' 等）实例化一个Project， 详解： https://juejin.cn/post/6917486983946338318
 * 配置阶段：解析每个项目中的build.gradle以及依赖的gradle插件，完成 Project配置和 Task配置，以及创建Task依赖的有向无环图
 * 执行阶段：根据Task依赖关系（生成的有向无环图）依次执行Task动作
 */
class SeepPlugin implements Plugin<Project> {

    @Override
    void apply(Project target) {
        println '自定义插件'
    }
}