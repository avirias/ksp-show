package com.avirias

fun main() {
    println("Hello World!")
}

@Decorator
interface HelloWorld {
    fun name(): String
    fun age(): Int
}