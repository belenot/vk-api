package com.belenot.service.vk

data class VkApiConfig(
    val token: String,
    val appId: String
)

data class DbConfig(
    val url: String,
    val username: String,
    val password: String
)

data class ServerConfig(
    val port: Int
)

data class UsersScrapeConfig(
    val interval: Int
)

data class Config(
    val vkApi: VkApiConfig,
    val db: DbConfig,
    val server: ServerConfig,
    val usersScrape: UsersScrapeConfig
)