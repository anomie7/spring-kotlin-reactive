package com.spring.kotlin.reactive.r2dbc

import org.springframework.data.annotation.Id

data class Item(@Id val id: Long? = null, var name: String, var price: Double)