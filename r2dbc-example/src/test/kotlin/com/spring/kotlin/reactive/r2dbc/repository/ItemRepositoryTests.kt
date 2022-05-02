package com.spring.kotlin.reactive.r2dbc.repository

import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatchers.*
import com.spring.kotlin.reactive.r2dbc.entity.Item
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Example
import org.springframework.data.domain.ExampleMatcher
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.test.StepVerifier

@SpringBootTest
class ItemRepositoryTests {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var dataBaseClient: DatabaseClient

    @Test
    fun saveTest() {
        val item = Item(name = "컴퓨터", price = 1000.22)

        val result = itemRepository.save(item).block()
        Assertions.assertEquals(result?.name, "컴퓨터")
        Assertions.assertEquals(result?.price, 1000.22)
    }

    @Test
    fun testDynamicQueryByObject() {
        val name = "Alf alarm clock"
        val price = 19.99
        val example = Example.of(Item(name = name, price = price))
        itemRepository.findAll(example)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(it.name, name)
                Assertions.assertEquals(it.price, price)
                true
            }
            .verifyComplete()
    }

    @Test
    fun testDynamicQueryByMatcher() {
        val name = "IPhone"
        val price = 0.0
        val matcher = ExampleMatcher.matching()
            .withMatcher("name", contains().ignoreCase())
            .withIgnorePaths("price")
        val example = Example.of(Item(name = name, price = price), matcher)

        itemRepository.findAll(example)
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertTrue(it.name.contains(name))
                true
            }
            .verifyComplete()
    }

    @Test
    fun testSearchByName() {
        val iphone = "IPhone"
        val item = Item(name = iphone, price = 0.0)
        itemRepository.searchItem(item)
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                val name = it["name"] as String
                Assertions.assertTrue(name.contains(iphone))
                true
            }
            .verifyComplete()
    }

    @Test
    fun testSearchByPrice() {
        val price = 20.99
        val item = Item(name = "", price = price)
        itemRepository.searchItem(item)
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertEquals(it["price"] as Double, price)
                true
            }
            .verifyComplete()
    }

    @Test
    fun testSearchByNameAndPrice() {
        val iphone = "IPhone"
        val price = 20.99
        val item = Item(name = iphone, price = price)
        itemRepository.searchItem(item)
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                val name = it["name"] as String
                Assertions.assertTrue(name.contains(iphone))
                Assertions.assertEquals(it["price"] as Double, price)
                true
            }
            .verifyComplete()
    }

}