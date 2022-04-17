package com.spring.kotlin.reactive.r2dbc.service

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.entity.CartItem
import com.spring.kotlin.reactive.r2dbc.repository.CartRepository
import com.spring.kotlin.reactive.r2dbc.repository.ItemRepository
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException

@Service
class CartService(
    val cartRepository: CartRepository,
    val itemRepository: ItemRepository
) {
    fun getAll(): Flux<Cart> {
        return cartRepository.getAll()
    }

    fun getById(cartId: Long): Flux<Cart> {
        return cartRepository.getById(cartId)
    }

    fun addToITem(cartId: Long, itemId: Long): Flux<CartItem> {
        return itemRepository.findById(itemId)
            .switchIfEmpty(Mono.error(RuntimeException("item not founded $itemId")))
            .flatMapMany { item ->
                cartRepository.addItemToCart(cartId, item)
            }
    }
}