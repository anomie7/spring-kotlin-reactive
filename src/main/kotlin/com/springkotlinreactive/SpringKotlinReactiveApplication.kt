package com.springkotlinreactive

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringKotlinReactiveApplication

fun main(args: Array<String>) {
	runApplication<SpringKotlinReactiveApplication>(*args)
}
