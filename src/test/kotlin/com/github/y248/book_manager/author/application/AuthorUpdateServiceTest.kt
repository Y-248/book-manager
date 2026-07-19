package com.github.y248.book_manager.author.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
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
import java.time.LocalDate
import java.time.OffsetDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AuthorUpdateServiceクラスは著者更新のユースケースを担う")
class AuthorUpdateServiceTest {

    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Nested
    @DisplayName("update()は対象の著者を取得し、重複チェックを行った上で著者を更新する")
    inner class Update {

        private val existing = Author.reconstruct(
            id = AuthorId(1L),
            name = "夏目漱石",
            birthDate = LocalDate.of(1867, 2, 9),
            createdAt = OffsetDateTime.now(),
            updatedAt = OffsetDateTime.now(),
        )

        @Test
        @DisplayName("対象の著者が存在し、他の著者とname・birthDateが重複しない場合、著者を更新して返す")
        fun `updates the author when it exists and does not duplicate another author`() {
            val updated = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目金之助",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = existing.createdAt!!,
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.findById(AuthorId(1L))).thenReturn(existing)
            `when`(authorRepository.existsByNameAndBirthDateExcluding(AuthorId(1L), "夏目金之助", LocalDate.of(1867, 2, 9)))
                .thenReturn(false)
            `when`(authorRepository.update(any())).thenReturn(updated)

            val service = AuthorUpdateService(authorRepository)
            val result = service.update(id = AuthorId(1L), name = "夏目金之助", birthDate = null)

            assertThat(result.name).isEqualTo("夏目金之助")
            verify(authorRepository).update(any())
        }

        @Test
        @DisplayName("対象のauthorIdの著者が存在しない場合、AuthorNotFoundExceptionを投げ、更新は行わない")
        fun `throws AuthorNotFoundException when the author does not exist`() {
            `when`(authorRepository.findById(AuthorId(999L))).thenReturn(null)

            val service = AuthorUpdateService(authorRepository)

            assertThatThrownBy { service.update(id = AuthorId(999L), name = "夏目金之助", birthDate = null) }
                .isInstanceOf(AuthorNotFoundException::class.java)

            verify(authorRepository, never()).update(any())
        }

        @Test
        @DisplayName("更新後の内容が自分以外の著者とname・birthDateで重複する場合、DuplicateAuthorExceptionを投げ、更新は行わない")
        fun `throws DuplicateAuthorException when the updated content duplicates another author`() {
            `when`(authorRepository.findById(AuthorId(1L))).thenReturn(existing)
            `when`(authorRepository.existsByNameAndBirthDateExcluding(AuthorId(1L), "森鴎外", LocalDate.of(1867, 2, 9)))
                .thenReturn(true)

            val service = AuthorUpdateService(authorRepository)

            assertThatThrownBy { service.update(id = AuthorId(1L), name = "森鴎外", birthDate = null) }
                .isInstanceOf(DuplicateAuthorException::class.java)

            verify(authorRepository, never()).update(any())
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