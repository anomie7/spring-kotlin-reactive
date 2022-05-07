package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Item
import io.r2dbc.spi.ConnectionFactory
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.query.Update
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest
class ItemUpdateTest {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Test
    fun updateByRepositoryTest() {
        val updatedName = "updated item name"
        val updatedPrice = 0.0
        itemRepository.findAllById(listOf(1, 2))
            .flatMap {
                it.name = updatedName
                it.price = updatedPrice
                Mono.just(it)
            }
            .flatMap {
                itemRepository.save(it)
            }
            .log()
            .`as`(StepVerifier::create)
            .thenConsumeWhile { item ->
                Assertions.assertEquals(item.name, updatedName)
                Assertions.assertEquals(item.price, updatedPrice)
                true
            }.verifyComplete()
    }

    @Test
    fun updateByFluentTest() {
        val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)
        val updatedName = "updated item name"
        val updatedPrice = 0.0
        itemRepository.findAllById(listOf(1, 2))
            .flatMap {
                it.name = updatedName
                it.price = updatedPrice
                Mono.just(it)
            }
            .flatMap {
                r2dbcEntityTemplate.update(Item::class.java)
                    .matching(Query.query(Criteria.where("id").`is`(it.id!!)))
                    .apply(Update.update("name", it.name).set("price", it.price))
            }
            .log()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }
            .then {
                itemRepository.findAllById(listOf(1, 2))
                    .`as`(StepVerifier::create)
                    .thenConsumeWhile { item ->
                        Assertions.assertEquals(item.name, updatedName)
                        Assertions.assertEquals(item.price, updatedPrice)
                        true
                    }.verifyComplete()
                true
            }.verifyComplete()
    }


    @Test
    fun updateByModifyingQueryTest() {
        val updatedName = "updated item name"
        val updatedPrice = 0.0
        val itemId: Long = 1
        itemRepository.updateItem(updatedName, updatedPrice, itemId)
            .log()
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertNotNull(it)
                true
            }
            .then {
                itemRepository.findById(itemId)
                    .`as`(StepVerifier::create)
                    .thenConsumeWhile { item ->
                        Assertions.assertEquals(item.name, updatedName)
                        Assertions.assertEquals(item.price, updatedPrice)
                        true
                    }.verifyComplete()
                true
            }.verifyComplete()
    }
}