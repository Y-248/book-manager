package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookNotFoundException
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.InvalidPublicationStatusTransitionException
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("BookUpdateServiceクラスは書籍更新のユースケースを担う")
class BookUpdateServiceTest {

    @Mock
    private lateinit var bookRepository: BookRepository

    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Mock
    private lateinit var authorSpecResolver: AuthorSpecResolver

    @Nested
    @DisplayName("update()は対象の書籍を取得し、著者指定(authorSpecs)を解決した上で書籍を更新する")
    inner class Update {

        private val existingBook = Book.reconstruct(
            id = BookId(1L),
            title = "吾輩は猫である",
            price = BigDecimal("1200.00"),
            publicationStatus = PublicationStatus.UNPUBLISHED,
            authorIds = listOf(AuthorId(1L)),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )

        @Test
        @DisplayName("authorsを指定した場合、AuthorSpecResolverで解決した著者で紐付けを完全に置き換える")
        fun `replaces author links when authorSpecs is given`() {
            val newAuthor = Author.reconstruct(
                id = AuthorId(2L),
                name = "新しい著者",
                birthDate = LocalDate.of(1990, 1, 1),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.findById(BookId(1L))).thenReturn(existingBook)
            val authorSpecs = listOf(AuthorSpec.Existing(AuthorId(2L)))
            `when`(authorSpecResolver.resolve(authorSpecs)).thenReturn(listOf(newAuthor))
            val updatedBook = Book.reconstruct(
                id = BookId(1L),
                title = existingBook.title,
                price = existingBook.price,
                publicationStatus = existingBook.publicationStatus,
                authorIds = listOf(AuthorId(2L)),
                createdAt = existingBook.createdAt!!,
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.update(any())).thenReturn(updatedBook)

            val service = BookUpdateService(bookRepository, authorRepository, authorSpecResolver)
            val result = service.update(
                id = BookId(1L),
                title = null,
                price = null,
                publicationStatus = null,
                authorSpecs = authorSpecs,
            )

            assertThat(result.authors).containsExactly(newAuthor)
            verify(authorRepository, never()).findAllByIds(any())
        }

        @Test
        @DisplayName("authorsを指定しない場合、著者の紐付けは変更せず、現在の著者情報をそのまま返す")
        fun `keeps the current author links when authorSpecs is not given`() {
            val currentAuthor = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.findById(BookId(1L))).thenReturn(existingBook)
            `when`(authorRepository.findAllByIds(listOf(AuthorId(1L)))).thenReturn(listOf(currentAuthor))
            `when`(bookRepository.update(any())).thenReturn(existingBook)

            val service = BookUpdateService(bookRepository, authorRepository, authorSpecResolver)
            val result = service.update(
                id = BookId(1L),
                title = "改訂版",
                price = null,
                publicationStatus = null,
                authorSpecs = null,
            )

            assertThat(result.authors).containsExactly(currentAuthor)
            verify(authorSpecResolver, never()).resolve(any())
        }

        @Test
        @DisplayName("対象のbookIdの書籍が存在しない場合、BookNotFoundExceptionを投げ、更新は行わない")
        fun `throws BookNotFoundException when the book does not exist`() {
            `when`(bookRepository.findById(BookId(999L))).thenReturn(null)

            val service = BookUpdateService(bookRepository, authorRepository, authorSpecResolver)

            assertThatThrownBy {
                service.update(id = BookId(999L), title = "改訂版", price = null, publicationStatus = null, authorSpecs = null)
            }.isInstanceOf(BookNotFoundException::class.java)

            verify(bookRepository, never()).update(any())
        }

        @Test
        @DisplayName("publicationStatusをPUBLISHEDからUNPUBLISHEDに変更しようとした場合、InvalidPublicationStatusTransitionExceptionを投げる")
        fun `throws InvalidPublicationStatusTransitionException when changing PUBLISHED to UNPUBLISHED`() {
            val publishedBook = Book.reconstruct(
                id = BookId(1L),
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.PUBLISHED,
                authorIds = listOf(AuthorId(1L)),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.findById(BookId(1L))).thenReturn(publishedBook)

            val service = BookUpdateService(bookRepository, authorRepository, authorSpecResolver)

            assertThatThrownBy {
                service.update(
                    id = BookId(1L),
                    title = null,
                    price = null,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = null,
                )
            }.isInstanceOf(InvalidPublicationStatusTransitionException::class.java)

            verify(bookRepository, never()).update(any())
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