package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Item
import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono


@Repository
interface ItemRepository : ReactiveCrudRepository<Item, Long>, ReactiveQueryByExampleExecutor<Item>,
    ItemCustomRepository {

    @Modifying
    @Query("UPDATE item SET name = :name, price = :price where id = :id")
    fun updateItem(name: String, price: Double, id: Long): Mono<Int?>
}

interface ItemCustomRepository {
    fun searchItem(item: Item): Flux<MutableMap<String, Any>>
    fun batchSave(items: List<Item>): Flux<Item>
}

@Repository
class ItemCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : ItemCustomRepository {
    override fun searchItem(item: Item): Flux<MutableMap<String, Any>> {
        var selectQuery = "SELECT * FROM item "
        val whereClause = mutableListOf<String>()
        if (item.name.isNotEmpty() || item.price != 0.0) {
            if (item.name.isNotEmpty()) {
                whereClause.add("UPPER(item.name) like UPPER('%${item.name}%')")
            }

            if (item.price != 0.0) {
                whereClause.add("item.price = ${item.price}")
            }
            selectQuery += whereClause.joinToString(" AND ", "WHERE ")
        }
        return dataBaseClient.sql(selectQuery).fetch().all()
    }

    override fun batchSave(items: List<Item>): Flux<Item> {
        return dataBaseClient.inConnectionMany { connection ->
            val statement =
                connection.createStatement("INSERT INTO item(name, price) VALUES ($1, $2)")
                    .returnGeneratedValues("id", "name", "price")
            for (item in items) {
                statement.bind(0, item.name).bind(1, item.price).add()
            }
            Flux.from(statement.execute()).flatMap { result ->
                result.map { row, r ->
                    Item(row["id"] as Long, row["name"] as String, row["price"] as Double)
                }
            }
        }
    }
}