package com.github.y248.book_manager.author.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
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
@DisplayName("AuthorRegistrationServiceクラスは著者登録のユースケースを担う")
class AuthorRegistrationServiceTest {

    @Mock
    private lateinit var authorRepository: AuthorRepository

    @Nested
    @DisplayName("register()は重複する著者がいないかチェックした上で著者を登録する")
    inner class Register {

        @Test
        @DisplayName("同名・同生年月日の著者が存在しない場合、著者を新規登録して返す")
        fun `saves a new author when no duplicate exists`() {
            val name = "夏目漱石"
            val birthDate = LocalDate.of(1867, 2, 9)
            val saved = Author.reconstruct(
                id = AuthorId(1L),
                name = name,
                birthDate = birthDate,
                createdAt = OffsetDateTime.now(),
                updatedAt = OffsetDateTime.now(),
            )
            `when`(authorRepository.existsByNameAndBirthDate(name, birthDate)).thenReturn(false)
            `when`(authorRepository.save(any())).thenReturn(saved)

            val service = AuthorRegistrationService(authorRepository)
            val result = service.register(name = name, birthDate = birthDate)

            assertThat(result.id).isEqualTo(AuthorId(1L))
            assertThat(result.name).isEqualTo(name)
            verify(authorRepository).save(any())
        }

        @Test
        @DisplayName("同名・同生年月日の著者が既に存在する場合、DuplicateAuthorExceptionを投げ、保存は行わない")
        fun `throws DuplicateAuthorException when name and birthDate already exist`() {
            val name = "夏目漱石"
            val birthDate = LocalDate.of(1867, 2, 9)
            `when`(authorRepository.existsByNameAndBirthDate(name, birthDate)).thenReturn(true)

            val service = AuthorRegistrationService(authorRepository)

            assertThatThrownBy { service.register(name = name, birthDate = birthDate) }
                .isInstanceOf(DuplicateAuthorException::class.java)

            verify(authorRepository, never()).save(any())
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