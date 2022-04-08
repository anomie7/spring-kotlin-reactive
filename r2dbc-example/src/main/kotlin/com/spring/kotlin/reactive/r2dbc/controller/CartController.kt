package com.spring.kotlin.reactive.r2dbc.controller

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.repository.CartRepository
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
class CartController(val cartRepository: CartRepository) {

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
}