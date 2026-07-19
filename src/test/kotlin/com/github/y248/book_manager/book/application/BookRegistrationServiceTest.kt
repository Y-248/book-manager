package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("BookRegistrationServiceクラスは書籍登録のユースケースを担う")
class BookRegistrationServiceTest {

    @Mock
    private lateinit var bookRepository: BookRepository

    @Mock
    private lateinit var authorSpecResolver: AuthorSpecResolver

    @Nested
    @DisplayName("register()はAuthorSpecResolverで著者指定(authorSpecs)を解決した上で書籍を登録する")
    inner class Register {

        @Test
        @DisplayName("解決済みの著者IDを紐付けて書籍を登録し、解決済みの著者一覧とともに返す")
        fun `registers a book linked to the resolved authors`() {
            val authorSpecs = listOf(AuthorSpec.Existing(AuthorId(1L)))
            val resolvedAuthor = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorSpecResolver.resolve(authorSpecs)).thenReturn(listOf(resolvedAuthor))
            val savedBook = Book.reconstruct(
                id = BookId(10L),
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorIds = listOf(AuthorId(1L)),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.save(any())).thenReturn(savedBook)

            val service = BookRegistrationService(bookRepository, authorSpecResolver)
            val result = service.register(
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorSpecs = authorSpecs,
            )

            assertThat(result.book).isSameAs(savedBook)
            assertThat(result.authors).containsExactly(resolvedAuthor)
        }
    }

    /**
     * MockitoのArgumentMatchers.any()はKotlinのnull非許容型に対してnullを返してしまいコンパイル・実行時に問題になるため、
     * 非nullとして扱えるラッパーを用意する（mockito-kotlin未導入のためこの形にしている）。
     */
    private fun <T> any(): T {
        ArgumentMatchers.any<T>()
        @Suppress("UNCHECKED_CAST")
        return null as T
    }
}