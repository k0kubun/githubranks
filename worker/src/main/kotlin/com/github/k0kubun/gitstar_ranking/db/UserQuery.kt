package com.github.k0kubun.gitstar_ranking.db

import com.github.k0kubun.gitstar_ranking.client.UserResponse
import com.github.k0kubun.gitstar_ranking.core.StarsCursor
import com.github.k0kubun.gitstar_ranking.core.User
import com.github.k0kubun.gitstar_ranking.core.table
import java.sql.Timestamp
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.RecordMapper
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.now

class UserQuery(private val database: DSLContext) {
    private val userColumns = listOf(
        field("id"),
        field("type"),
        field("login"),
        field("stargazers_count"),
        field("updated_at", Timestamp::class.java),
    )

    private val userMapper = RecordMapper<Record, User> { record ->
        User(
            id = record.get("id", Long::class.java),
            type = record.get("type", String::class.java),
            login = record.get("login", String::class.java),
            stargazersCount = record.get("stargazers_count", Long::class.java),
            updatedAt = record.get("updated_at", Timestamp::class.java),
        )
    }

    fun find(id: Long): User? {
        return database
            .select(userColumns)
            .from("users")
            .where(field("id").eq(id))
            .fetchOne(userMapper)
    }

    fun findBy(login: String): User? {
        return database
            .select(userColumns)
            .from("users")
            .where(field("login").eq(login))
            .fetchOne(userMapper)
    }

    fun create(user: UserResponse) {
        database
            .insertInto(table("users", primaryKey = "id"))
            .columns(
                field("id"),
                field("type"),
                field("login"),
                field("avatar_url"),
                field("created_at"),
                field("updated_at"),
            )
            .values(
                user.id,
                user.type,
                user.login,
                user.avatarUrl,
                now(), // created_at
                now(), // updated_at
            )
            .onDuplicateKeyUpdate()
            .set(field("login", String::class.java), field("excluded.login", String::class.java))
            .execute()
    }

    fun update(id: Long, login: String? = null, stargazersCount: Long? = null) {
        database
            .update(table("users"))
            .set(field("updated_at"), now())
            .run {
                if (login != null) {
                    set(field("login"), login)
                } else this
            }
            .run {
                if (stargazersCount != null) {
                    set(field("stargazers_count"), stargazersCount)
                } else this
            }
            .where(field("id").eq(id))
            .execute()
    }

    fun destroy(id: Long) {
        database
            .delete(table("users"))
            .where(field("id").eq(id))
            .execute()
    }

    fun count(stargazersCount: Long? = null): Long {
        return database
            .selectCount()
            .from("users")
            .where(field("type").eq("User"))
            .run {
                if (stargazersCount != null) {
                    and("stargazers_count = ?", stargazersCount)
                } else this
            }
            .fetchOne(0, Long::class.java)!!
    }

    fun max(column: String): Long? {
        return database
            .select(field(column))
            .from("users")
            .orderBy(field(column).desc())
            .limit(1)
            .fetchOne(column, Long::class.java)
    }

    fun findStargazersCount(stargazersCountLessThan: Long): Long? {
        return database
            .select(field("stargazers_count"))
            .from("users")
            .where(field("stargazers_count").lessThan(stargazersCountLessThan))
            .orderBy(field("stargazers_count").desc())
            .limit(1)
            .fetchOne("stargazers_count", Long::class.java)
    }

    fun orderByIdAsc(stargazersCount: Long, idAfter: Long, limit: Int): List<User> {
        return database
            .select(userColumns)
            .from("users")
            .where(field("stargazers_count").eq(stargazersCount))
            .and(field("id").greaterThan(idAfter))
            .orderBy(field("id").asc())
            .limit(limit)
            .fetch(userMapper)
    }

    fun orderByStarsDesc(limit: Int, after: StarsCursor? = null): List<User> {
        return database
            .select(userColumns)
            .from("users")
            .where(field("type").eq("User"))
            .run {
                if (after != null) {
                    and("(stargazers_count, id) < (?, ?)", after.stars, after.id)
                } else this
            }
            .orderBy(field("stargazers_count").desc(), field("id").desc())
            .limit(limit)
            .fetch(userMapper)
    }
}
