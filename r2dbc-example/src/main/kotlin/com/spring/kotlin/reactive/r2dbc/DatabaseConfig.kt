package com.spring.kotlin.reactive.r2dbc

import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement


@Configuration
@EnableR2dbcRepositories
@EnableTransactionManagement
class DatabaseConfig