package com.spring.kotlin.reactive.r2dbc.controller

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.entity.CartItem
import com.spring.kotlin.reactive.r2dbc.repository.CartRepository
import com.spring.kotlin.reactive.r2dbc.repository.ItemRepository
import com.spring.kotlin.reactive.r2dbc.service.CartService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException

@RestController
class CartController(val cartService: CartService) {

    @GetMapping("v1/carts")
    fun getCarts(): Flux<Cart> {
        return cartService.getAll()
    }

    @GetMapping(value = ["v1/carts/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getCartsByStream(): Flux<Cart> {
        return cartService.getAll()
    }

    @GetMapping("v1/carts/{id}")
    fun getCartsById(@PathVariable("id") id: Long): Flux<Cart> {
        return cartService.getById(id)
    }

    @PostMapping("v1/carts/{id}/add/{itemId}")
    fun addItem(@PathVariable("id") cartId: Long, @PathVariable("itemId") itemId: Long): Flux<CartItem> {
        return cartService.addToITem(cartId, itemId)
    }
}