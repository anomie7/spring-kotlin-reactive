package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import com.spring.kotlin.reactive.r2dbc.entity.CartItem
import com.spring.kotlin.reactive.r2dbc.entity.Item
import io.r2dbc.spi.ConnectionFactory
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Update
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.RuntimeException
import java.util.stream.Collectors

@Repository
interface CartRepository : ReactiveCrudRepository<Cart, Long>, CartCustomRepository {
    @Query("select * from cart")
    fun findAllByQuery(): Flux<Cart>
}

interface CartCustomRepository {
    fun getAll(): Flux<Cart>
    fun getById(cartId: Long): Flux<Cart>
    fun addItemToCart(cartId: Long, item: Item): Flux<CartItem>
}

@Repository
class CartCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient,
    connectionFactory: ConnectionFactory
) : CartCustomRepository {
    private val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)

    private val cartMapper: (t: MutableList<MutableMap<String, Any>>) -> Cart
        get() {
            val cartMapper: (t: MutableList<MutableMap<String, Any>>) -> Cart = { list ->
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
            return cartMapper
        }

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
            }.map(cartMapper)
//            .delayElements(Duration.ofSeconds(5))
    }

    /**
     * @author minu
     * 일대 다 연관 관계 구현
     * Cart 조회시 모든 연관관계를 즉시 불러옴
     */
    override fun getById(cartId: Long): Flux<Cart> {
        return dataBaseClient.sql(
            """
                SELECT cart_item.*, item.name as item_name, item.price as item_price FROM cart_item
                INNER JOIN item ON cart_item.item_id = item.id
                WHERE cart_item.cart_id = :cart_id
            """.trimMargin()
        )
            .bind("cart_id", cartId)
            .fetch().all()
            .bufferUntilChanged {
                it["cart_id"]
            }.map(cartMapper)
    }

    override fun addItemToCart(cartId: Long, item: Item): Flux<CartItem> {
        return getById(cartId)
            .switchIfEmpty(Mono.error(RuntimeException("[cart not founded $cartId]")))
            .flatMap { cart ->
                val cartItem = cart.cartItems?.firstOrNull { it.itemId == item.id }
                    ?: CartItem(
                        cartId = cartId,
                        itemId = item.id,
                        quantity = 0,
                        item = item
                    )
                cartItem.increment()
                Mono.just(cartItem)
            }.flatMap { cartItem ->
                val id = cartItem.id
                if (id != null) {
                    r2dbcEntityTemplate.update(CartItem::class.java)
                        .matching(
                            org.springframework.data.relational.core.query.Query.query(
                                Criteria.where("id").`is`(id)
                            )
                        )
                        .apply(Update.update("quantity", cartItem.quantity))
                        .flatMap {
                            Mono.just(cartItem)
                        }
                } else {
                    r2dbcEntityTemplate.insert(CartItem::class.java)
                        .using(cartItem)
                }
            }
    }
}