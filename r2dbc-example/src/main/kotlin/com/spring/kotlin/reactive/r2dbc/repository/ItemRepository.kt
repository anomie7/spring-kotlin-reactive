package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Item
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface ItemRepository : ReactiveCrudRepository<Item, Long>, ReactiveQueryByExampleExecutor<Item>, ItemCustomRepository

interface ItemCustomRepository {
    fun searchItem(item: Item): Flux<MutableMap<String, Any>>
}

@Repository
class ItemCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : ItemCustomRepository {
    override fun searchItem(item: Item): Flux<MutableMap<String, Any>> {
        var selectQuery = "SELECT * FROM item "
        if (item.name.isNotEmpty() && item.price != 0.0) {
            selectQuery += "WHERE "
            selectQuery += "item.name like '%${item.name}%'"
            selectQuery += " AND "
            selectQuery += "item.price = ${item.price}"
        } else if (item.name.isNotEmpty() || item.price != 0.0) {
            selectQuery += "WHERE "
            if (item.name.isNotEmpty()) {
                selectQuery += "item.name like '%${item.name}%'"
            }

            if (item.price != 0.0) {
                selectQuery += "item.price = ${item.price}"
            }
        }
        return dataBaseClient.sql(selectQuery).fetch().all()
    }
}