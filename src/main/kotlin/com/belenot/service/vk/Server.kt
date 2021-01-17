package com.belenot.service.vk

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.time.Instant

enum class Health { HEALTHY, UNHEALTHY}
data class HealthRs (val status: Health)
data class AddUserIdsRq (val userIds: List<String>)
data class RemoveUserIdsRq (val userIds: List<String>)

class Server(val serverConfig: ServerConfig, val usersService: UsersService, val usersScrapeJob: UsersScrapeJob) {
    fun start() {
        logger.info("Starting http server on port ${serverConfig.port}.")
        embeddedServer(Netty, serverConfig.port) {
            module(usersService, usersScrapeJob)
        }.start(true)
    }
}

fun Application.module(usersService: UsersService, usersScrapeJob: UsersScrapeJob) {

    install(ContentNegotiation) {
        jackson {
            // Support for Java Time Api, e.g. Instant
            this.registerModule(JavaTimeModule())
        }
    }

    install(CallLogging)

    routing {
        get("/health") {
            call.respond(HealthRs(Health.HEALTHY))
        }

        get("/users-online") {
            val startDate =  Instant.ofEpochSecond(call.parameters["start-date"]?.toLong() ?: Instant.now().epochSecond)
            val endDate = Instant.ofEpochSecond(call.parameters["end-date"]?.toLong() ?: Instant.now().epochSecond)
            call.respond(usersService.getSavedUsers(startDate, endDate))
        }

        post("/add-user-ids") {
            val addUserIdsRq = call.receive<AddUserIdsRq>()
            usersScrapeJob.addUserIds(addUserIdsRq.userIds)
            call.respond(HttpStatusCode.OK)
        }

        post("/remove-user-ids") {
            val removeUserIdsRq = call.receive<RemoveUserIdsRq>()
            usersScrapeJob.removeUserIds(removeUserIdsRq.userIds)
            call.respond(HttpStatusCode.OK)
        }
    }
}