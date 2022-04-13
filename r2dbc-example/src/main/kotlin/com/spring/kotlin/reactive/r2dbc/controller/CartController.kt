package com.spring.kotlin.reactive.r2dbc.controller

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.entity.CartItem
import com.spring.kotlin.reactive.r2dbc.repository.CartRepository
import com.spring.kotlin.reactive.r2dbc.repository.ItemRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException

@RestController
class CartController(val cartRepository: CartRepository, val itemRepository: ItemRepository) {

    @GetMapping("v1/carts")
    fun getCarts(): Flux<Cart> {
        return cartRepository.getAll()
    }

    @GetMapping(value = ["v1/carts/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getCartsByStream(): Flux<Cart> {
        return cartRepository.getAll()
    }

    @GetMapping("v1/carts/{id}")
    fun getCartsById(@PathVariable("id") id: Long): Flux<Cart> {
        return cartRepository.getById(id)
    }

    @PutMapping("v1/carts/{id}/cartItems/{itemId}")
    fun addItem(@PathVariable("id") cartId: Long, @PathVariable("itemId") itemId: Long): Flux<CartItem> {
        return itemRepository.findById(itemId)
            .switchIfEmpty(Mono.error(RuntimeException("item not founded $itemId")))
            .flatMapMany {
                cartRepository.addItemToCart(cartId, itemId)
            }
    }
}