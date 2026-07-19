package com.github.y248.book_manager.book.presentation

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.book.domain.Book
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class BookResponse(
    val id: Long,
    val title: String,
    val price: BigDecimal,
    val publicationStatus: String,
    val authors: List<AuthorSummary>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
) {
    data class AuthorSummary(
        val id: Long,
        val name: String,
        val birthDate: LocalDate,
    )

    companion object {
        fun from(book: Book, authors: List<Author>): BookResponse = BookResponse(
            id = requireNotNull(book.id).value,
            title = book.title,
            price = book.price,
            publicationStatus = book.publicationStatus.name,
            authors = authors.map {
                AuthorSummary(id = requireNotNull(it.id).value, name = it.name, birthDate = it.birthDate)
            },
            createdAt = requireNotNull(book.createdAt),
            updatedAt = requireNotNull(book.updatedAt),
        )
    }
}