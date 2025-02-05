package io.eigr.synapsys.extensions.store.backends.postgres

import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.store.AbstractStore
import java.sql.Connection

class PostgresStore<S : Any>(
    serializer: MessageSerializer,
    private val connection: Connection
) : AbstractStore<S>(serializer) {

    init {
        connection.prepareStatement(
            """
            CREATE TABLE IF NOT EXISTS actor_states (
                id VARCHAR(255) PRIMARY KEY,
                data BYTEA NOT NULL
            )
            """
        ).use { it.executeUpdate() }
    }

    override suspend fun persistData(id: String, bytes: ByteArray) {
        connection.prepareStatement(
            """
            INSERT INTO actor_states (id, data)
            VALUES (?, ?)
            ON CONFLICT (id) DO UPDATE SET data = EXCLUDED.data
            """
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