package com.belenot.service.vk

import com.vk.api.sdk.client.VkApiClient
import com.vk.api.sdk.client.actors.ServiceActor
import com.vk.api.sdk.httpclient.HttpTransportClient
import com.vk.api.sdk.objects.users.Fields
import java.sql.DriverManager
import java.time.Instant

data class VKUser(val id: Int, val firstName: String, val lastName: String, val isOnline: Boolean, val scrapeTime: Instant)

class UsersService(val vkConfig: VkApiConfig, val dbConfig: DbConfig) {

    val vk = VkApiClient(HttpTransportClient())
    val serviceActor = ServiceActor(vkConfig.appId.toInt(), vkConfig.token)
    val conn = DriverManager.getConnection(dbConfig.url, dbConfig.username, dbConfig.password)

    fun scrapeUsersOnline(userIds: List<String>) {
        val users = vk
            .users()
            .get(serviceActor)
            .userIds(userIds)
            .fields(Fields.ONLINE)
            .execute().map {
                VKUser(
                    it.id,
                    it.firstName,
                    it.lastName,
                    it.isOnline,
                    Instant.now()
                )
            }
        for (user in users) {
            saveToDb(user)
        }
    }

    fun saveToDb(vkUser: VKUser) {
        logger.info("Save user: $vkUser")
        conn.prepareStatement("insert into users_online (user_id, first_name, last_name, is_online, scrape_time) values (?, ?, ?, ?, ?)").also {
            it.setInt(1, vkUser.id)
            it.setString(2, vkUser.firstName)
            it.setString(3, vkUser.lastName)
            it.setBoolean(4, vkUser.isOnline)
            it.setTimestamp(5, java.sql.Timestamp(vkUser.scrapeTime.epochSecond))
        }.executeUpdate()
    }

    fun getSavedUsers(startDate: Instant, endDate: Instant) = conn
        .also { logger.info("Get saved users from ${startDate} to ${endDate}") }
        .prepareStatement("select user_id, first_name, last_name, is_online, scrape_time from users_online where scrape_time >= ? and scrape_time <= ?")
        .also {
            it.setTimestamp(1, java.sql.Timestamp(startDate.epochSecond))
            it.setTimestamp(2, java.sql.Timestamp(endDate.epochSecond))
        }
        .executeQuery()
        .use {
            generateSequence {
                if(it.next())
                    VKUser(
                        id = it.getInt(1),
                        firstName = it.getString(2),
                        lastName = it.getString(3),
                        isOnline = it.getBoolean(4),
                        scrapeTime = Instant.ofEpochSecond(it.getTimestamp(5).time)
                    )
                else null
            }.toList()
        }
        .also { logger.info("Return ${it.size} users")}

}