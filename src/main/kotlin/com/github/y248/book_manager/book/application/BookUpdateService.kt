package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookNotFoundException
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BookUpdateService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
    private val authorSpecResolver: AuthorSpecResolver,
) {
    @Transactional
    fun update(
        id: BookId,
        title: String?,
        price: BigDecimal?,
        publicationStatus: PublicationStatus?,
        authorSpecs: List<AuthorSpec>?,
    ): BookWithAuthors {
        val existing = bookRepository.findById(id) ?: throw BookNotFoundException(id)

        // authorSpecsが指定された場合のみ著者を解決する（指定時は紐付けを完全に置き換え、未指定時は現在の紐付けを維持）。
        val authors = authorSpecs?.let { authorSpecResolver.resolve(it) }

        val updated = existing.update(
            title = title,
            price = price,
            publicationStatus = publicationStatus,
            authorIds = authors?.map { requireNotNull(it.id) },
        )
        val savedBook = bookRepository.update(updated)

        val resultAuthors = authors ?: authorRepository.findAllByIds(existing.authorIds)
        return BookWithAuthors(savedBook, resultAuthors)
    }
}