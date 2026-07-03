package com.enterprise.fabric

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class Application

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val version: String = "0.1.0",
    val devAuthEnabled: Boolean = false,
    val brokerEnabled: Boolean = false,
    val kafkaBootstrap: String = "localhost:19092"
)

fun main(args: Array<String>) { runApplication<Application>(*args) }
