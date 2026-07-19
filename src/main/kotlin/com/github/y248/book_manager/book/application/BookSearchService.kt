package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.book.domain.BookRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookSearchService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
) {
    /**
     * 指定した著者に紐づく書籍一覧を取得する。著者の存在確認は行わない
     * （該当著者が存在しない場合も、書籍が0件の場合と同様に空リストを返す）。
     */
    @Transactional(readOnly = true)
    fun findByAuthorId(authorId: AuthorId): List<BookWithAuthors> {
        val books = bookRepository.findAllByAuthorId(authorId)
        if (books.isEmpty()) return emptyList()

        val authorsById = authorRepository.findAllByIds(books.flatMap { it.authorIds }.distinct())
            .associateBy { it.id }

        return books.map { book ->
            BookWithAuthors(book, book.authorIds.map { requireNotNull(authorsById[it]) })
        }
    }
}