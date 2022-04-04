package com.spring.kotlin.reactive.r2dbc.entity

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.PersistenceConstructor
import org.springframework.data.annotation.Transient

data class Cart(
    @Id
    val id: Long? = null,

    @Transient
    @Value("null")
    var cartItems: List<CartItem>? = null
){
//    @PersistenceConstructor
//    constructor(id: Long?): this(id = id, cartItems = null)
}
