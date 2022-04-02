package com.springkotlinreactive

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux
import java.util.function.Function

@RestController
class ServerController(private val kitchen: KitchenService) {
    @GetMapping(value = ["/server"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun serveDishes(): Flux<Dish> {
        return kitchen.getDishes()
    }

    // end::controller[]
    // tag::deliver[]
    @GetMapping(value = ["/served-dishes"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun deliverDishes(): Flux<Dish> {
        return kitchen.getDishes().map { dish -> Dish.deliver(dish) }
    } // end::deliver[]
}