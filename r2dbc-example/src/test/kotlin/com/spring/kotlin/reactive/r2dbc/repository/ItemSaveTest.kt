package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Item
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.Result
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@SpringBootTest
class ItemSaveTest {
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
    fun saveByTemplateTest() {
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
    fun saveByQueryTest() {
        val id = 999L
        dataBaseClient.sql("INSERT INTO item(id, name, price) VALUES ($id, '테스트 아티템 15', 22.99)")
            .fetch().all()
            .`as`(StepVerifier::create)
            .then {
                itemRepository.findById(id)
                    .`as`(StepVerifier::create)
                    .expectNextMatches {
                        Assertions.assertEquals(id, it.id)
                        true
                    }.verifyComplete()
            }
            .verifyComplete()
    }

    @Test
    fun batchSaveTest() {
        val item1 = Item(name = "배치 아이템1", price = 101.0)
        val item2 = Item(name = "배치 아이템2", price = 141.0)
        itemRepository.batchSave(listOf(item1, item2))
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                println("item save success: $it")
                true
            }.verifyComplete()
    }

}