package io.vdartsabvile.avspeak

import java.sql.Connection
import java.sql.DriverManager

class SQLiteConnector {
    private val url = "jdbc:sqlite:database_users.db" // Укажите имя базы данных

    fun connect(): Connection {
        return DriverManager.getConnection(url)
    }
}
