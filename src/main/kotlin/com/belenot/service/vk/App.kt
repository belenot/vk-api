package com.belenot.service.vk

import com.sksamuel.hoplite.ConfigLoader
import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.ServiceActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.users.Fields
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

val logger = LoggerFactory.getLogger("main")

fun main() {

    logger.info("VK API.")

    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yml")
    val usersService = UsersService(config.vkApi, config.db)
    val usersScrapeJob = UsersScrapeJob(config.db, config.usersScrape, usersService)
    val server = Server(config.server, usersService, usersScrapeJob)

    GlobalScope.launch {
        usersScrapeJob.run()
    }
    server.start()

    logger.info("Exit application...")
}
