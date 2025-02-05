package io.eigr.synapsys.extensions.store.backends.mysql

import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.store.AbstractStore
import java.sql.Connection

class MySQLStore<S : Any>(
    serializer: MessageSerializer,
    private val connection: Connection
) : AbstractStore<S>(serializer) {

    init {
        connection.prepareStatement(
            """
            CREATE TABLE IF NOT EXISTS actor_states (
                id VARCHAR(255) PRIMARY KEY,
                data BLOB NOT NULL
            )
            """
        ).use { it.executeUpdate() }
    }

    override suspend fun persistData(id: String, bytes: ByteArray) {
        connection.prepareStatement(
            "REPLACE INTO actor_states (id, data) VALUES (?, ?)"
        ).use { stmt ->
            stmt.setString(1, id)
            stmt.setBytes(2, bytes)
            stmt.executeUpdate()
        }
    }

    override suspend fun retrieveData(id: String): ByteArray? {
        return connection.prepareStatement(
            "SELECT data FROM actor_states WHERE id = ?"
        ).use { stmt ->
            stmt.setString(1, id)
            stmt.executeQuery().use { rs ->
                if (rs.next()) rs.getBytes("data") else null
            }
        }
    }
}