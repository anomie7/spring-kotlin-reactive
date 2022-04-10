package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.entity.Item
import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.insert
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest
class CartWriteTest {
    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Test
    fun saveTest() {
        val item = Item(name = "테스트 아이템", price = 110.0)
        itemRepository.save(item)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(item.name, it.name)
                Assertions.assertEquals(item.price, it.price)
                true
            }
            .verifyComplete()
    }

    @Test
    fun saveByQueryTest() {
        val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)
        val item = Item(name = "테스트 아이템", price = 110.0)
        r2dbcEntityTemplate.insert(Item::class.java)
            .using(item)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(item.name, it.name)
                Assertions.assertEquals(item.price, it.price)
                true
            }
            .verifyComplete()
    }

    @Test
    fun addItemToCartTest() {
        addItemToCart(1, 2)
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                true
            }
            .verifyComplete()
    }


    fun addItemToCart(cartId: Long, itemId: Long): Flux<() -> Cart> {
        return cartRepository.getById(cartId)
            .defaultIfEmpty(Cart(cartId))
            .flatMap {
                Mono.just { Cart(it.id) }
            }.log()

    }
}