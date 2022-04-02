/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// tag::code[]
package com.springkotlinreactive

import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.time.Duration
import java.util.*

@Service
class KitchenService {//
    /**
     * Generates continuous stream of dishes.
     */
    fun getDishes(): Flux<Dish> {
        return Flux.generate { sink: SynchronousSink<Dish> ->
            sink.next(
                randomDish()
            )
        }.delayElements(Duration.ofMillis(250))
    }

    /**
     * Randomly pick the next dish.
     */
    private fun randomDish(): Dish {
        return menu[picker.nextInt(menu.size)]
    }

    private val menu = listOf( //
        Dish("Sesame chicken"),  //
        Dish("Lo mein noodles, plain"),  //
        Dish("Sweet & sour beef")
    )
    private val picker = Random()
}