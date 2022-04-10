package com.spring.kotlin.reactive.r2dbc.entity

import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.relational.core.mapping.Column

data class CartItem(
    @Id
    val id: Long? = null,
    var quantity: Int = 1,
    @Column("cart_id")
    var cartId: Long? = null,
    @Column("item_id")
    var itemId: Long? = null,

    @Transient
    @Value("null")
    var item: Item? = null
) {
    fun increment() {
        this.quantity += 1
    }
}