package com.spring.kotlin.reactive.r2dbc

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.core.publisher.switchIfEmpty

@SpringBootTest
class ItemRepositoryTests {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Test
    fun saveTest() {
        val item = Item(name = "컴퓨터", price = 1000.22)

        val result = itemRepository.save(item).block()
        Assertions.assertEquals(result?.name, "컴퓨터")
        Assertions.assertEquals(result?.price, 1000.22)
    }
}