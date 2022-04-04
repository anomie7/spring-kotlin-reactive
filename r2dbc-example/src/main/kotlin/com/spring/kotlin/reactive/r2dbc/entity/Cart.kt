package com.spring.kotlin.reactive.r2dbc.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient

data class Cart(
    @Id
    val id: Long? = null,
    @Transient
    var cartItems: List<CartItem> = listOf()
)
