package com.spring.kotlin.reactive.r2dbc.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient
import reactor.test.StepVerifier

@SpringBootTest
class CartRepositoryTest {
    @Autowired
    private lateinit var cartRepository: CartRepository

    @Test
    fun getAllTest() {
        cartRepository.getAll()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }.verifyComplete()
    }

    @Test
    fun findAllByQueryTest() {
        cartRepository.findAllByQuery()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }.verifyComplete()
    }

    @Test
    fun findAllTest() {
        cartRepository.findAll()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }.verifyComplete()
    }

    @Test
    fun getByIdTest() {
        cartRepository.getById(1)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(1, it.id)
                Assertions.assertNotNull(it.cartItems)
                true
            }.verifyComplete()
    }

    @Test
    fun getByIdTestWhenNothing() {
        cartRepository.getById(5)
            .`as`(StepVerifier::create)
            .expectComplete().verify()
    }
}