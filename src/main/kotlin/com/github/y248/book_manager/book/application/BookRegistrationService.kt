package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BookRegistrationService(
    private val bookRepository: BookRepository,
    private val authorSpecResolver: AuthorSpecResolver,
) {
    @Transactional
    fun register(
        title: String,
        price: BigDecimal,
        publicationStatus: PublicationStatus,
        authorSpecs: List<AuthorSpec>,
    ): BookWithAuthors {
        val authors = authorSpecResolver.resolve(authorSpecs)
        val book = Book.register(
            title = title,
            price = price,
            publicationStatus = publicationStatus,
            authorIds = authors.map { requireNotNull(it.id) },
        )
        val savedBook = bookRepository.save(book)
        return BookWithAuthors(savedBook, authors)
    }
}