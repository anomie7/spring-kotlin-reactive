package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.CartItem
import com.spring.kotlin.reactive.r2dbc.entity.Item
import com.spring.kotlin.reactive.r2dbc.service.CartService
import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import org.springframework.r2dbc.core.DatabaseClient
import reactor.test.StepVerifier

@SpringBootTest
class CartSaveTest {
    @Autowired
    private lateinit var cartService: CartService

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Autowired
    private lateinit var dataBaseClient: DatabaseClient

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
    fun updateCartItemTest() {
        val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)
        r2dbcEntityTemplate.update(CartItem::class.java)
            .matching(Query.query(Criteria.where("id").`is`(1)))
            .apply(Update.update("quantity", 5)).subscribe()
    }

    @Test
    fun addItemToCartTestWhenUpdate() {
        val cartId = 1L
        val itemId = 2L
        cartService.addItem(cartId, itemId)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(cartId, it.cartId)
                Assertions.assertEquals(itemId, it.itemId)
                Assertions.assertEquals(5, it.quantity)
                true
            }.verifyComplete()

        cartService.getById(cartId)
            .`as`(StepVerifier::create)
            .expectNextMatches { cart ->
                val cartItem = cart.cartItems?.firstOrNull { it.itemId == itemId }
                Assertions.assertNotNull(cartItem)
                Assertions.assertEquals(5, cartItem?.quantity)
                true
            }.verifyComplete()
    }

    @Test
    fun addItemToCartTestWhenCreate() {
        val cartId: Long = 1
        val itemId: Long = 3

        cartService.addItem(cartId, itemId)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(cartId, it.cartId)
                Assertions.assertEquals(itemId, it.itemId)
                Assertions.assertEquals(1, it.quantity)
                true
            }
            .verifyComplete()

        cartService.getById(cartId)
            .`as`(StepVerifier::create)
            .expectNextMatches { cart ->
                val cartItem = cart.cartItems?.firstOrNull { it.itemId == itemId }
                Assertions.assertNotNull(cartItem)
                Assertions.assertEquals(cartId, cartItem?.cartId)
                Assertions.assertEquals(itemId, cartItem?.itemId)
                Assertions.assertEquals(1, cartItem?.quantity)
                true
            }
            .verifyComplete()
    }
}