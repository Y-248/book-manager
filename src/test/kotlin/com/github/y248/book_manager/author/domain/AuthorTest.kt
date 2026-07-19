package com.github.y248.book_manager.author.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.OffsetDateTime

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

    @Nested
    @DisplayName("update()は指定した項目だけを変更し、指定しなかった項目は現在の値を維持する（PATCHセマンティクス）")
    inner class Update {

        private val persisted = Author.reconstruct(
            id = AuthorId(1L),
            name = "夏目漱石",
            birthDate = LocalDate.of(1867, 2, 9),
            createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
        )

        @Test
        @DisplayName("nameとbirthDateを両方指定すると、両方とも変更される")
        fun `updates both name and birthDate when both are given`() {
            val updated = persisted.update(name = "夏目金之助", birthDate = LocalDate.of(1867, 2, 10))

            assertThat(updated.name).isEqualTo("夏目金之助")
            assertThat(updated.birthDate).isEqualTo(LocalDate.of(1867, 2, 10))
        }

        @Test
        @DisplayName("nameのみ指定すると、birthDateは元の値のまま変更されない")
        fun `keeps the current birthDate when only name is given`() {
            val updated = persisted.update(name = "夏目金之助")

            assertThat(updated.name).isEqualTo("夏目金之助")
            assertThat(updated.birthDate).isEqualTo(persisted.birthDate)
        }

        @Test
        @DisplayName("birthDateのみ指定すると、nameは元の値のまま変更されない")
        fun `keeps the current name when only birthDate is given`() {
            val updated = persisted.update(birthDate = LocalDate.of(1867, 2, 10))

            assertThat(updated.name).isEqualTo(persisted.name)
            assertThat(updated.birthDate).isEqualTo(LocalDate.of(1867, 2, 10))
        }

        @Test
        @DisplayName("何も指定しない場合、元の値のまま変更されない")
        fun `keeps everything unchanged when nothing is given`() {
            val updated = persisted.update()

            assertThat(updated.name).isEqualTo(persisted.name)
            assertThat(updated.birthDate).isEqualTo(persisted.birthDate)
        }

        @Test
        @DisplayName("id・createdAtは更新後も維持される")
        fun `keeps id and createdAt unchanged`() {
            val updated = persisted.update(name = "夏目金之助")

            assertThat(updated.id).isEqualTo(persisted.id)
            assertThat(updated.createdAt).isEqualTo(persisted.createdAt)
        }

        @Test
        @DisplayName("更新後のnameが空白のみになる場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to a blank name`() {
            assertThatIllegalArgumentException().isThrownBy { persisted.update(name = "   ") }
        }

        @Test
        @DisplayName("更新後のnameが255文字を超える場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to a name longer than 255 characters`() {
            val tooLongName = "a".repeat(256)

            assertThatIllegalArgumentException().isThrownBy { persisted.update(name = tooLongName) }
        }

        @Test
        @DisplayName("更新後のbirthDateが未来日になる場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to a birthDate in the future`() {
            val tomorrow = LocalDate.now().plusDays(1)

            assertThatIllegalArgumentException().isThrownBy { persisted.update(birthDate = tomorrow) }
        }

        @Test
        @DisplayName("未永続化（id=null）の著者に対して呼び出すと、IllegalArgumentExceptionを投げる")
        fun `rejects updating an author that has not been persisted yet`() {
            val unpersisted = Author.register(name = "テスト太郎", birthDate = LocalDate.of(2000, 1, 1))

            assertThatIllegalArgumentException().isThrownBy { unpersisted.update(name = "テスト次郎") }
        }
    }
}