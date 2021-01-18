package com.belenot.service.vk

import com.sksamuel.hoplite.ConfigLoader
import khttp.get
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.lang.Exception

val logger = LoggerFactory.getLogger("main")

fun main() {

    logger.info("VK API.")

    val config = ConfigLoader().loadConfigOrThrow<Config>("/application.yml")
    runBlocking {
        checkKubernetesSidecar(config.sidecarHealthCheck.port, config.sidecarHealthCheck.path)
    }
    val usersService = UsersService(config.vkApi, config.db)
    val usersScrapeJob = UsersScrapeJob(config.db, config.usersScrape, usersService)
    val server = Server(config.server, usersService, usersScrapeJob)

    GlobalScope.launch {
        usersScrapeJob.run()
    }
    server.start()

    logger.info("Exit application...")
}

suspend fun checkKubernetesSidecar(port: Int, path: String) {
    var sc = 0
    while (sc < 200 || sc > 200) {
        try {
            sc = get("http://localhost:${port}${path}").statusCode
        } catch (exc: Exception) {
            logger.info("Got $sc from sidecar health check. Try again...")
            delay(3000)
        }
    }
}