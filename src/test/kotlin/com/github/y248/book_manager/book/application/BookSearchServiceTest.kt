package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("BookSearchServiceクラスは著者に紐づく書籍の取得ユースケースを担う")
class BookSearchServiceTest {

    @Mock
    private lateinit var bookRepository: BookRepository

    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Nested
    @DisplayName("findByAuthorId()は指定した著者に紐づく書籍を、各書籍の著者情報も含めて返す")
    inner class FindByAuthorId {

        @Test
        @DisplayName("紐づく書籍が存在する場合、各書籍に紐づく著者（共著者を含む）の情報も解決して返す")
        fun `returns books with all of their linked authors resolved`() {
            val queriedAuthorId = AuthorId(1L)
            val coAuthorId = AuthorId(2L)
            val book = Book.reconstruct(
                id = BookId(10L),
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.PUBLISHED,
                authorIds = listOf(queriedAuthorId, coAuthorId),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.findAllByAuthorId(queriedAuthorId)).thenReturn(listOf(book))
            val queriedAuthor = Author.reconstruct(
                id = queriedAuthorId,
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            val coAuthor = Author.reconstruct(
                id = coAuthorId,
                name = "共著者",
                birthDate = LocalDate.of(1900, 1, 1),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.findAllByIds(listOf(queriedAuthorId, coAuthorId)))
                .thenReturn(listOf(queriedAuthor, coAuthor))

            val service = BookSearchService(bookRepository, authorRepository)
            val result = service.findByAuthorId(queriedAuthorId)

            assertThat(result).hasSize(1)
            assertThat(result[0].book).isSameAs(book)
            assertThat(result[0].authors).containsExactly(queriedAuthor, coAuthor)
        }

        @Test
        @DisplayName("紐づく書籍が存在しない場合、空リストを返し、著者情報の取得は行わない")
        fun `returns an empty list without fetching authors when there are no books`() {
            val authorId = AuthorId(999L)
            `when`(bookRepository.findAllByAuthorId(authorId)).thenReturn(emptyList())

            val service = BookSearchService(bookRepository, authorRepository)
            val result = service.findByAuthorId(authorId)

            assertThat(result).isEmpty()
            verify(authorRepository, never()).findAllByIds(anyList())
        }
    }
}