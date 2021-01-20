package com.god.seep.media.bean

data class Person(val name: String, val age: Int? = null)

fun main(args: Array<String>) {
    val persons = listOf(Person("Alice"), Person("Bob", 20))
    val max = persons.maxByOrNull { it.age ?: 0 }
    println("oldest is: $max")
    if (max is Person)
        max.name.toUpperCase()
}