package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookRepository
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
@DisplayName("BookRegistrationServiceクラスは書籍登録のユースケースを担う")
class BookRegistrationServiceTest {

    @Mock
    private lateinit var bookRepository: BookRepository

    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Nested
    @DisplayName("register()はauthorSpecs（著者指定）を解決した上で書籍を登録する")
    inner class Register {

        @Test
        @DisplayName("既存著者のみを指定した場合、新規著者登録は行わず、書籍と紐付けて登録する")
        fun `registers a book with only existing authors`() {
            val existingAuthor = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.findById(AuthorId(1L))).thenReturn(existingAuthor)
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

            val service = BookRegistrationService(bookRepository, authorRepository)
            val result = service.register(
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorSpecs = listOf(AuthorSpec.Existing(AuthorId(1L))),
            )

            assertThat(result.book.title).isEqualTo("吾輩は猫である")
            assertThat(result.authors).containsExactly(existingAuthor)
            verify(authorRepository, never()).saveAll(any())
        }

        @Test
        @DisplayName("新規著者を含む場合、著者をバルクインサートしてから書籍と紐付けて登録する")
        fun `registers a book and bulk-inserts new authors`() {
            `when`(authorRepository.existsByNameAndBirthDate("新人作家A", LocalDate.of(1990, 1, 1))).thenReturn(false)
            `when`(authorRepository.existsByNameAndBirthDate("新人作家B", LocalDate.of(1992, 5, 5))).thenReturn(false)
            val savedNewAuthors = listOf(
                Author.reconstruct(
                    id = AuthorId(2L),
                    name = "新人作家A",
                    birthDate = LocalDate.of(1990, 1, 1),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
                ),
                Author.reconstruct(
                    id = AuthorId(3L),
                    name = "新人作家B",
                    birthDate = LocalDate.of(1992, 5, 5),
                    createdAt = OffsetDateTime.now(),
                    updatedAt = OffsetDateTime.now(),
                ),
            )
            `when`(authorRepository.saveAll(any())).thenReturn(savedNewAuthors)
            val savedBook = Book.reconstruct(
                id = BookId(11L),
                title = "テスト書籍",
                price = BigDecimal("1000.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorIds = listOf(AuthorId(2L), AuthorId(3L)),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(bookRepository.save(any())).thenReturn(savedBook)

            val service = BookRegistrationService(bookRepository, authorRepository)
            val result = service.register(
                title = "テスト書籍",
                price = BigDecimal("1000.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorSpecs = listOf(
                    AuthorSpec.New(name = "新人作家A", birthDate = LocalDate.of(1990, 1, 1)),
                    AuthorSpec.New(name = "新人作家B", birthDate = LocalDate.of(1992, 5, 5)),
                ),
            )

            assertThat(result.authors).containsExactly(savedNewAuthors[0], savedNewAuthors[1])
            verify(authorRepository).saveAll(any())
        }

        @Test
        @DisplayName("存在しないauthorIdが指定された場合、AuthorNotFoundExceptionを投げ、書籍登録は行わない")
        fun `throws AuthorNotFoundException when authorId does not exist`() {
            `when`(authorRepository.findById(AuthorId(999L))).thenReturn(null)

            val service = BookRegistrationService(bookRepository, authorRepository)

            assertThatThrownBy {
                service.register(
                    title = "テスト書籍",
                    price = BigDecimal.ZERO,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = listOf(AuthorSpec.Existing(AuthorId(999L))),
                )
            }.isInstanceOf(AuthorNotFoundException::class.java)

            verify(bookRepository, never()).save(any())
        }

        @Test
        @DisplayName("新規著者としてDB上に既に存在するname・birthDateが指定された場合、DuplicateAuthorExceptionを投げ、書籍登録は行わない")
        fun `throws DuplicateAuthorException when a new author duplicates an existing one`() {
            `when`(authorRepository.existsByNameAndBirthDate("夏目漱石", LocalDate.of(1867, 2, 9))).thenReturn(true)

            val service = BookRegistrationService(bookRepository, authorRepository)

            assertThatThrownBy {
                service.register(
                    title = "テスト書籍",
                    price = BigDecimal.ZERO,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = listOf(AuthorSpec.New(name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))),
                )
            }.isInstanceOf(DuplicateAuthorException::class.java)

            verify(authorRepository, never()).saveAll(any())
            verify(bookRepository, never()).save(any())
        }

        @Test
        @DisplayName("リクエスト内で複数の新規著者のname・birthDateが重複する場合、DuplicateAuthorExceptionを投げ、書籍登録は行わない")
        fun `throws DuplicateAuthorException when two new authors in the same request duplicate each other`() {
            val service = BookRegistrationService(bookRepository, authorRepository)

            assertThatThrownBy {
                service.register(
                    title = "テスト書籍",
                    price = BigDecimal.ZERO,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = listOf(
                        AuthorSpec.New(name = "重複太郎", birthDate = LocalDate.of(2000, 1, 1)),
                        AuthorSpec.New(name = "重複太郎", birthDate = LocalDate.of(2000, 1, 1)),
                    ),
                )
            }.isInstanceOf(DuplicateAuthorException::class.java)

            verify(authorRepository, never()).saveAll(any())
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