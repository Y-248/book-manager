package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
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
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthorSpecResolverクラスは書籍登録・更新時の著者指定(AuthorSpec)の解決を担う")
class AuthorSpecResolverTest {

    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Nested
    @DisplayName("resolve()は既存著者への紐付け・新規著者の登録（バルクインサート）を指定順で解決する")
    inner class Resolve {

        @Test
        @DisplayName("既存著者のみを指定した場合、新規著者登録は行わず、該当著者をそのまま返す")
        fun `resolves only existing authors without registering any new author`() {
            val author = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.findById(AuthorId(1L))).thenReturn(author)

            val resolver = AuthorSpecResolver(authorRepository)
            val result = resolver.resolve(listOf(AuthorSpec.Existing(AuthorId(1L))))

            assertThat(result).containsExactly(author)
            verify(authorRepository, never()).saveAll(any())
        }

        @Test
        @DisplayName("新規著者を含む場合、著者をバルクインサートし、指定した順序で結果を返す")
        fun `bulk-inserts new authors and preserves the given order`() {
            val existingAuthor = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.findById(AuthorId(1L))).thenReturn(existingAuthor)
            `when`(authorRepository.existsByNameAndBirthDate("新人作家A", LocalDate.of(1990, 1, 1))).thenReturn(false)
            val newAuthor = Author.reconstruct(
                id = AuthorId(2L),
                name = "新人作家A",
                birthDate = LocalDate.of(1990, 1, 1),
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.saveAll(any())).thenReturn(listOf(newAuthor))

            val resolver = AuthorSpecResolver(authorRepository)
            val result = resolver.resolve(
                listOf(
                    AuthorSpec.Existing(AuthorId(1L)),
                    AuthorSpec.New(name = "新人作家A", birthDate = LocalDate.of(1990, 1, 1)),
                )
            )

            assertThat(result).containsExactly(existingAuthor, newAuthor)
        }

        @Test
        @DisplayName("存在しないauthorIdが指定された場合、AuthorNotFoundExceptionを投げる")
        fun `throws AuthorNotFoundException when authorId does not exist`() {
            `when`(authorRepository.findById(AuthorId(999L))).thenReturn(null)

            val resolver = AuthorSpecResolver(authorRepository)

            assertThatThrownBy { resolver.resolve(listOf(AuthorSpec.Existing(AuthorId(999L)))) }
                .isInstanceOf(AuthorNotFoundException::class.java)
        }

        @Test
        @DisplayName("同じauthorIdが複数回指定された場合、IllegalArgumentExceptionを投げる")
        fun `rejects the same authorId specified more than once`() {
            val resolver = AuthorSpecResolver(authorRepository)

            assertThatIllegalArgumentException().isThrownBy {
                resolver.resolve(listOf(AuthorSpec.Existing(AuthorId(1L)), AuthorSpec.Existing(AuthorId(1L))))
            }
        }

        @Test
        @DisplayName("新規著者としてDB上に既に存在するname・birthDateが指定された場合、DuplicateAuthorExceptionを投げ、登録は行わない")
        fun `throws DuplicateAuthorException when a new author duplicates an existing one`() {
            `when`(authorRepository.existsByNameAndBirthDate("夏目漱石", LocalDate.of(1867, 2, 9))).thenReturn(true)

            val resolver = AuthorSpecResolver(authorRepository)

            assertThatThrownBy {
                resolver.resolve(listOf(AuthorSpec.New(name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))))
            }.isInstanceOf(DuplicateAuthorException::class.java)

            verify(authorRepository, never()).saveAll(any())
        }

        @Test
        @DisplayName("リクエスト内で複数の新規著者のname・birthDateが重複する場合、DuplicateAuthorExceptionを投げ、登録は行わない")
        fun `throws DuplicateAuthorException when two new authors in the same request duplicate each other`() {
            val resolver = AuthorSpecResolver(authorRepository)

            assertThatThrownBy {
                resolver.resolve(
                    listOf(
                        AuthorSpec.New(name = "重複太郎", birthDate = LocalDate.of(2000, 1, 1)),
                        AuthorSpec.New(name = "重複太郎", birthDate = LocalDate.of(2000, 1, 1)),
                    )
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