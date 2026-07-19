package com.github.y248.book_manager.author.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("Authorクラスは著者の名前や生年月日といった情報を扱う")
class AuthorTest {

    @Nested
    @DisplayName("register()は入力値のバリデーションを行う")
    inner class Register {

        @Test
        @DisplayName("正しい名前と生年月日を渡すと、未永続化状態（id/createdAt/updatedAtがnull）の著者を生成する")
        fun `creates an author with the given name and birthDate`() {
            val author = Author.register(name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))

            assertThat(author.id).isNull()
            assertThat(author.name).isEqualTo("夏目漱石")
            assertThat(author.birthDate).isEqualTo(LocalDate.of(1867, 2, 9))
            assertThat(author.createdAt).isNull()
            assertThat(author.updatedAt).isNull()
        }

        @Test
        @DisplayName("名前が空白のみの場合、IllegalArgumentExceptionを投げる")
        fun `rejects a blank name`() {
            assertThatIllegalArgumentException()
                .isThrownBy { Author.register(name = "   ", birthDate = LocalDate.of(2000, 1, 1)) }
        }

        @Test
        @DisplayName("名前が255文字を超える場合、IllegalArgumentExceptionを投げる")
        fun `rejects a name longer than 255 characters`() {
            val tooLongName = "a".repeat(256)

            assertThatIllegalArgumentException()
                .isThrownBy { Author.register(name = tooLongName, birthDate = LocalDate.of(2000, 1, 1)) }
        }

        @Test
        @DisplayName("名前がちょうど255文字の場合、著者を生成できる（境界値）")
        fun `accepts a name with exactly 255 characters`() {
            val maxLengthName = "a".repeat(255)

            val author = Author.register(name = maxLengthName, birthDate = LocalDate.of(2000, 1, 1))

            assertThat(author.name).hasSize(255)
        }

        @Test
        @DisplayName("生年月日が未来日の場合、IllegalArgumentExceptionを投げる")
        fun `rejects a birthDate in the future`() {
            val tomorrow = LocalDate.now().plusDays(1)

            assertThatIllegalArgumentException()
                .isThrownBy { Author.register(name = "テスト太郎", birthDate = tomorrow) }
        }

        @Test
        @DisplayName("生年月日が今日の場合、著者を生成できる（境界値）")
        fun `accepts a birthDate of today`() {
            val today = LocalDate.now()

            val author = Author.register(name = "テスト太郎", birthDate = today)

            assertThat(author.birthDate).isEqualTo(today)
        }
    }
}