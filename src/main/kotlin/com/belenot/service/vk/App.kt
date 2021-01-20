package com.belenot.service.vk

import com.sksamuel.hoplite.ConfigLoader
import khttp.get
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception

val logger = LoggerFactory.getLogger("main")

fun main(args: Array<String>) {
    logger.info("VK API.")

    val config = if (args.size == 0)
        ConfigLoader().loadConfigOrThrow<Config>("/application.yml").also { logger.info("Load default config from classpath.") }
    else
        ConfigLoader().loadConfigOrThrow<Config>(args.map{ File(it) }).also { logger.info("Load config from ${args.map{ it }.joinToString(",")}") }
    logConfig(config)
    if (config.sidecarHealthCheck.enabled) runBlocking {
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

fun logConfig(config: Config) {
    val msg = """
        
        VK Client:
          appId=${config.vkApi.appId}
        Database:
          url=${config.db.url}
          username=${config.db.username}
        Sidecar health check:
          port=${config.sidecarHealthCheck.port}
          path=${config.sidecarHealthCheck.path}
          enabled=${config.sidecarHealthCheck.enabled}
    """.trimIndent()
    logger.info(msg)
}