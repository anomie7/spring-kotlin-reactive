package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux

@Repository
interface CartRepository : ReactiveCrudRepository<Cart, Long>, CartCustomRepository {
    @Query("select * from cart")
    fun findAllByQuery(): Flux<Cart>
}

interface CartCustomRepository {
    fun getAll(): Flux<Cart>
}

@Repository
class CartCustomRepositoryImpl(val dataBaseClient: DatabaseClient) : CartCustomRepository {
    override fun getAll(): Flux<Cart> {
        return dataBaseClient.sql("""
            SELECT * FROM cart
        """).fetch().all().map {
            val id = it["id"] as Long
            Cart(id)
        }
    }

}