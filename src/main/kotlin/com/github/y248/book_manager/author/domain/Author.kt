package com.github.y248.book_manager.author.domain

import java.time.LocalDate
import java.time.OffsetDateTime

class Author private constructor(
    val id: AuthorId?,
    val name: String,
    val birthDate: LocalDate,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
) {
    companion object {
        private const val MAX_NAME_LENGTH = 255

        /**
         * 新規著者を登録する。DBのCHECK制約（birth_date <= CURRENT_DATE）と同等の不変条件をここでも担保する。
         */
        fun register(name: String, birthDate: LocalDate): Author {
            require(name.isNotBlank()) { "name must not be blank" }
            require(name.length <= MAX_NAME_LENGTH) { "name must be at most $MAX_NAME_LENGTH characters" }
            require(!birthDate.isAfter(LocalDate.now())) { "birthDate must not be after the current date" }
            return Author(id = null, name = name, birthDate = birthDate, createdAt = null, updatedAt = null)
        }

        /**
         * 永続化済みの値からAuthorを復元する。DBから読み出した値は正当であることが前提のため、再バリデーションは行わない。
         */
        fun reconstruct(
            id: AuthorId,
            name: String,
            birthDate: LocalDate,
            createdAt: OffsetDateTime,
            updatedAt: OffsetDateTime,
        ): Author = Author(id, name, birthDate, createdAt, updatedAt)
    }
}