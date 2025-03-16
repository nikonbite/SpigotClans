package org.mmarket.clans.system.util

import org.ktorm.schema.BaseTable
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import java.nio.ByteBuffer
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types
import java.util.UUID

/**
 * Тип SQL для UUID, который сохраняет UUID как BINARY(16) в базе данных
 */
object UuidSqlType : SqlType<UUID>(Types.BINARY, "binary") {
    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: UUID) {
        val bytes = ByteBuffer.allocate(16).apply {
            putLong(parameter.mostSignificantBits)
            putLong(parameter.leastSignificantBits)
        }.array()
        ps.setBytes(index, bytes)
    }

    override fun doGetResult(rs: ResultSet, index: Int): UUID? {
        val bytes = rs.getBytes(index) ?: return null
        val bb = ByteBuffer.wrap(bytes)
        val high = bb.long
        val low = bb.long
        return UUID(high, low)
    }
}

/**
 * Расширение для BaseTable, которое добавляет метод binaryUuid для создания колонки UUID
 */
fun <T : BaseTable<*>> T.binaryUuid(name: String): Column<UUID> {
    return registerColumn(name, UuidSqlType)
} 