package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.AuthorId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AuthorSpecは書籍登録時の著者指定（既存著者への紐付け／新規著者登録）を表す")
class AuthorSpecTest {

    @Nested
    @DisplayName("of()はauthorIdとname/birthDateのどちらか一方のみが指定されていることを検証する")
    inner class Of {

        @Test
        @DisplayName("authorIdのみ指定した場合、Existingを返す")
        fun `returns Existing when only authorId is given`() {
            val spec = AuthorSpec.of(authorId = 1L, name = null, birthDate = null)

            assertThat(spec).isEqualTo(AuthorSpec.Existing(AuthorId(1L)))
        }

        @Test
        @DisplayName("nameとbirthDateのみ指定した場合、Newを返す")
        fun `returns New when only name and birthDate are given`() {
            val spec = AuthorSpec.of(authorId = null, name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))

            assertThat(spec).isEqualTo(AuthorSpec.New(name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9)))
        }

        @Test
        @DisplayName("authorIdとname・birthDateを同時に指定した場合、IllegalArgumentExceptionを投げる")
        fun `rejects specifying both authorId and name-birthDate`() {
            assertThatIllegalArgumentException().isThrownBy {
                AuthorSpec.of(authorId = 1L, name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))
            }
        }

        @Test
        @DisplayName("何も指定しない場合、IllegalArgumentExceptionを投げる")
        fun `rejects specifying nothing`() {
            assertThatIllegalArgumentException().isThrownBy {
                AuthorSpec.of(authorId = null, name = null, birthDate = null)
            }
        }

        @Test
        @DisplayName("nameのみ指定してbirthDateを指定しない場合、IllegalArgumentExceptionを投げる")
        fun `rejects specifying name without birthDate`() {
            assertThatIllegalArgumentException().isThrownBy {
                AuthorSpec.of(authorId = null, name = "夏目漱石", birthDate = null)
            }
        }

        @Test
        @DisplayName("birthDateのみ指定してnameを指定しない場合、IllegalArgumentExceptionを投げる")
        fun `rejects specifying birthDate without name`() {
            assertThatIllegalArgumentException().isThrownBy {
                AuthorSpec.of(authorId = null, name = null, birthDate = LocalDate.of(1867, 2, 9))
            }
        }
    }
}