package com.spring.kotlin.reactive.r2dbc.repository

import com.spring.kotlin.reactive.r2dbc.entity.Cart
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface CartRepository : ReactiveCrudRepository<Cart, Long>, CartCustomRepository {

}

interface CartCustomRepository

@Repository
class CartCustomRepositoryImpl : CartCustomRepository