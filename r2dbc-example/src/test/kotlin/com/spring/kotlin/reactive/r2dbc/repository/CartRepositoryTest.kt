package com.spring.kotlin.reactive.r2dbc.repository

import org.junit.jupiter.api.Test

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.r2dbc.core.DatabaseClient

@SpringBootTest
class CartRepositoryTest {
    @Autowired
    private lateinit var cartRepository: CartRepository

    @Autowired
    private lateinit var  dataBaseClient: DatabaseClient

    @Test
    fun getAllTest() {
        val carts = cartRepository.getAll()

        carts.map { it }.subscribe()
    }

    @Test
    fun findAllByQueryTest() {
        val carts = cartRepository.findAllByQuery()

        carts.map { it }.subscribe()
    }

    @Test
    fun findAllTest() {
        val carts = cartRepository.findAll()

        carts.collectMap {
            it.id
        }.map {
            val keys = it.keys
            keys
        }

    }
}