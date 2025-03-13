package org.mmarket.clans.system.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.StringColumnType
import org.jetbrains.exposed.sql.statements.api.ExposedBlob

object ClansTable : Table("clans") {
    private val gson = Gson()
    private val newsListType = object : TypeToken<List<String>>() {}.type

    val id = uuid("id").uniqueIndex()
    val name = varchar("name", length = 256).uniqueIndex()
    val colorlessName = varchar("colorless_name", length = 12)
    val treasury = long("treasury").default(0)
    var news = text("news").default("[]").transform({ str: String -> 
        gson.fromJson<List<String>>(str, newsListType)
    }, { list: List<String> ->
        gson.toJson(list)
    })
    var motd = text("motd").default("")
    val creator = uuid("creator").uniqueIndex()
    var owner = uuid("owner").uniqueIndex()
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}