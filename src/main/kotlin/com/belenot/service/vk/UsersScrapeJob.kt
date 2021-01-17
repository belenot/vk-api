package com.belenot.service.vk

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import org.slf4j.LoggerFactory
import java.sql.DriverManager

class UsersScrapeJob(val dbConfig: DbConfig, val scrapeConfig: UsersScrapeConfig, val usersService: UsersService) {

    private val logger = LoggerFactory.getLogger("UsersScrapeJob")
    val conn = DriverManager.getConnection(dbConfig.url, dbConfig.username, dbConfig.password)
        .also { it.autoCommit = false}

    val mutex = Mutex(locked = true)
    val mutateIds = Mutex(locked = false)

    suspend fun run() {
        logger.info("Run.")
        while(mutex.isLocked) {
            mutateIds.lock()
            usersService.scrapeUsersOnline(userIds.toList())
            mutateIds.unlock()
            delay(scrapeConfig.interval.toLong() * 1000)
        }
        logger.info("Stop.")
    }

    val userIds: List<String>

    get() = conn.prepareStatement("select user_id from scrape_users;")
        .executeQuery()
        .use {
            generateSequence {
                if (it.next())
                    it.getInt(1).toString()
                else
                    null
            }.toList()
        }

    fun addUserIds(userIds: List<String>) {
        while(mutateIds.isLocked) { }
        val addIds = userIds.filter{ it !in this.userIds }
        logger.info("Add $addIds.")

        for (id in addIds) {
            val st = conn.prepareStatement("insert into scrape_users ( user_id ) values ( ? )")
            st.setInt(1, id.toInt())
            st.executeUpdate()
        }
        conn.commit()
    }

    fun removeUserIds(userIds: List<String>) {
        while(mutateIds.isLocked) { }
        val removeIds = userIds.filter{ it in this.userIds }
        logger.info("Remove $removeIds.")

        for (id in removeIds) {
            val st = conn.prepareStatement("delete from scrape_users  where user_id = ?")
            st.setInt(1, id.toInt())
            st.executeUpdate()
        }
    }




    fun stop() {
        mutex.unlock()
    }
}