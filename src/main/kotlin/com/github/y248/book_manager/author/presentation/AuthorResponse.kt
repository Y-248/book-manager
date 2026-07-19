package com.github.y248.book_manager.author.presentation

import com.github.y248.book_manager.author.domain.Author
import java.time.LocalDate
import java.time.OffsetDateTime

data class AuthorResponse(
    val id: Long,
    val name: String,
    val birthDate: LocalDate,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    companion object {
        fun from(author: Author): AuthorResponse = AuthorResponse(
            id = requireNotNull(author.id) { "id must not be null after registration" }.value,
            name = author.name,
            birthDate = author.birthDate,
            createdAt = requireNotNull(author.createdAt),
            updatedAt = requireNotNull(author.updatedAt),
        )
    }
}