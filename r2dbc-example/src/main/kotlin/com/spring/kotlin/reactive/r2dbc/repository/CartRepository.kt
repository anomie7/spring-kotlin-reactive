package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.entity.CartItem
import com.spring.kotlin.reactive.r2dbc.entity.Item
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import java.time.Duration
import java.util.stream.Collectors

@Repository
interface CartRepository : ReactiveCrudRepository<Cart, Long>, CartCustomRepository {
    @Query("select * from cart")
    fun findAllByQuery(): Flux<Cart>
}

interface CartCustomRepository {
    fun getAll(): Flux<Cart>
    fun getById(cartId: Long): Flux<Cart>
}

@Repository
class CartCustomRepositoryImpl(val dataBaseClient: DatabaseClient) : CartCustomRepository {
    override fun getAll(): Flux<Cart> {
        return dataBaseClient.sql(
            """
            SELECT cart_item.*, item.name as item_name, item.price as item_price FROM cart
            INNER JOIN cart_item ON cart.id = cart_item.cart_id
            INNER JOIN item ON cart_item.item_id = item.id
        """
        ).fetch().all()
            .bufferUntilChanged {
                it["cart_id"]
            }.map { list ->
                val cartId = list[0]["cart_id"] as Long
                val cartItems = list.stream().map {
                    val id = it["id"] as Long
                    val quantity = it["quantity"] as Int
                    val cartId = it["cart_id"] as Long
                    val itemId = it["item_id"] as Long
                    val name = it["item_name"] as String
                    val price = it["item_price"] as Double
                    CartItem(
                        id = id,
                        quantity = quantity,
                        cartId = cartId,
                        itemId = itemId,
                        Item(
                            id = itemId,
                            name = name,
                            price = price
                        )
                    )
                }.collect(Collectors.toList())
                Cart(id = cartId, cartItems = cartItems)
            }
//            .delayElements(Duration.ofSeconds(5))
    }

    /**
     * @author minu
     * 일대 다 연관 관계 구현
     * Cart 조회시 모든 연관관계를 즉시 불러옴
     */
    override fun getById(cartId: Long): Flux<Cart> {
        return dataBaseClient.sql("SELECT cart_item.*, item.name as item_name, item.price as item_price FROM cart_item INNER JOIN item ON cart_item.item_id = item.id WHERE cart_item.cart_id = :cart_id")
            .bind("cart_id", cartId)
            .fetch().all()
            .bufferUntilChanged {
                it["cart_id"]
            }.map { list ->
                val cartId = list[0]["cart_id"] as Long
                val cartItems = list.stream().map {
                    val id = it["id"] as Long
                    val quantity = it["quantity"] as Int
                    val cartId = it["cart_id"] as Long
                    val itemId = it["item_id"] as Long
                    val name = it["item_name"] as String
                    val price = it["item_price"] as Double
                    CartItem(
                        id = id,
                        quantity = quantity,
                        cartId = cartId,
                        itemId = itemId,
                        Item(
                            id = itemId,
                            name = name,
                            price = price
                        )
                    )
                }.collect(Collectors.toList())
                Cart(id = cartId, cartItems = cartItems)
            }
    }
}