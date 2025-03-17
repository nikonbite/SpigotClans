package org.mmarket.clans.system.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.ktorm.schema.Column
import org.ktorm.schema.SqlType
import org.ktorm.schema.BaseTable
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

class MapSqlType<T : Any>(
    val typeRef: TypeReference<T>
) : SqlType<T>(Types.VARCHAR, "json") {
    private val objectMapper = ObjectMapper()

    override fun doSetParameter(ps: PreparedStatement, index: Int, parameter: T) {
        ps.setString(index, objectMapper.writeValueAsString(parameter))
    }

    override fun doGetResult(rs: ResultSet, index: Int): T? {
        val json = rs.getString(index)
        return if (json.isNullOrBlank()) null else objectMapper.readValue(json, typeRef)
    }
}

// Расширение для создания колонки с типом Map<String, Int>
fun <T : BaseTable<*>> T.mapColumn(name: String): Column<Map<String, Int>> {
    return registerColumn(name, MapSqlType(object : TypeReference<Map<String, Int>>() {}))
}