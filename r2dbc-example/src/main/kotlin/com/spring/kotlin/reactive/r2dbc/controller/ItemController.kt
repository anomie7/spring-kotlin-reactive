package com.spring.kotlin.reactive.r2dbc.controller

import com.spring.kotlin.reactive.r2dbc.entity.Item
import com.spring.kotlin.reactive.r2dbc.repository.ItemRepository
import com.spring.kotlin.reactive.r2dbc.request.ItemRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@RestController
class ItemController(val itemRepository: ItemRepository) {

    @GetMapping("/v1/items")
    fun getItems(): Flux<Item> {
        return itemRepository.findAll()
    }

    @PostMapping("/v1/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun saveItems(@RequestBody request: ItemRequest): Mono<Item> {
        return itemRepository.save(Item(name = request.name, price = request.price))
    }
}