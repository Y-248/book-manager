package com.github.y248.book_manager.book.domain

import com.github.y248.book_manager.author.domain.AuthorId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

@DisplayName("Bookクラスは書籍のタイトルや価格、著者との紐付けといった情報を扱う")
class BookTest {

    @Nested
    @DisplayName("register()は入力値のバリデーションを行う")
    inner class Register {

        @Test
        @DisplayName("正しいタイトル・価格・出版状況・著者IDを渡すと、未永続化状態（id/createdAt/updatedAtがnull）の書籍を生成する")
        fun `creates a book with the given values`() {
            val book = Book.register(
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorIds = listOf(AuthorId(1L)),
            )

            assertThat(book.id).isNull()
            assertThat(book.title).isEqualTo("吾輩は猫である")
            assertThat(book.price).isEqualByComparingTo("1200.00")
            assertThat(book.publicationStatus).isEqualTo(PublicationStatus.UNPUBLISHED)
            assertThat(book.authorIds).containsExactly(AuthorId(1L))
            assertThat(book.createdAt).isNull()
            assertThat(book.updatedAt).isNull()
        }

        @Test
        @DisplayName("タイトルが空白のみの場合、IllegalArgumentExceptionを投げる")
        fun `rejects a blank title`() {
            assertThatIllegalArgumentException().isThrownBy {
                Book.register(
                    title = "   ",
                    price = BigDecimal.ZERO,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorIds = listOf(AuthorId(1L)),
                )
            }
        }

        @Test
        @DisplayName("タイトルが255文字を超える場合、IllegalArgumentExceptionを投げる")
        fun `rejects a title longer than 255 characters`() {
            val tooLongTitle = "a".repeat(256)

            assertThatIllegalArgumentException().isThrownBy {
                Book.register(
                    title = tooLongTitle,
                    price = BigDecimal.ZERO,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorIds = listOf(AuthorId(1L)),
                )
            }
        }

        @Test
        @DisplayName("価格が負の値の場合、IllegalArgumentExceptionを投げる")
        fun `rejects a negative price`() {
            assertThatIllegalArgumentException().isThrownBy {
                Book.register(
                    title = "テスト書籍",
                    price = BigDecimal("-1"),
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorIds = listOf(AuthorId(1L)),
                )
            }
        }

        @Test
        @DisplayName("価格が0の場合、書籍を生成できる（境界値）")
        fun `accepts a price of zero`() {
            val book = Book.register(
                title = "テスト書籍",
                price = BigDecimal.ZERO,
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorIds = listOf(AuthorId(1L)),
            )

            assertThat(book.price).isEqualByComparingTo(BigDecimal.ZERO)
        }

        @Test
        @DisplayName("著者が1人も指定されていない場合、IllegalArgumentExceptionを投げる")
        fun `rejects an empty author list`() {
            assertThatIllegalArgumentException().isThrownBy {
                Book.register(
                    title = "テスト書籍",
                    price = BigDecimal.ZERO,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorIds = emptyList(),
                )
            }
        }
    }

    @Nested
    @DisplayName("update()は指定した項目だけを変更し、指定しなかった項目は現在の値を維持する（PATCHセマンティクス）")
    inner class Update {

        private val persisted = Book.reconstruct(
            id = BookId(1L),
            title = "吾輩は猫である",
            price = BigDecimal("1200.00"),
            publicationStatus = PublicationStatus.UNPUBLISHED,
            authorIds = listOf(AuthorId(1L)),
            createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
        )

        @Test
        @DisplayName("全項目を指定すると、全項目とも変更される")
        fun `updates all fields when all are given`() {
            val updated = persisted.update(
                title = "吾輩は猫である（改訂版）",
                price = BigDecimal("1500.00"),
                publicationStatus = PublicationStatus.PUBLISHED,
                authorIds = listOf(AuthorId(2L)),
            )

            assertThat(updated.title).isEqualTo("吾輩は猫である（改訂版）")
            assertThat(updated.price).isEqualByComparingTo("1500.00")
            assertThat(updated.publicationStatus).isEqualTo(PublicationStatus.PUBLISHED)
            assertThat(updated.authorIds).containsExactly(AuthorId(2L))
        }

        @Test
        @DisplayName("何も指定しない場合、元の値のまま変更されない")
        fun `keeps everything unchanged when nothing is given`() {
            val updated = persisted.update()

            assertThat(updated.title).isEqualTo(persisted.title)
            assertThat(updated.price).isEqualByComparingTo(persisted.price)
            assertThat(updated.publicationStatus).isEqualTo(persisted.publicationStatus)
            assertThat(updated.authorIds).isEqualTo(persisted.authorIds)
        }

        @Test
        @DisplayName("authorIdsを指定すると、紐付けがそのリストの内容で完全に置き換わる")
        fun `replaces the author links entirely when authorIds is given`() {
            val updated = persisted.update(authorIds = listOf(AuthorId(2L), AuthorId(3L)))

            assertThat(updated.authorIds).containsExactly(AuthorId(2L), AuthorId(3L))
        }

        @Test
        @DisplayName("authorIdsを指定しない場合、現在の紐付けが維持される")
        fun `keeps the current author links when authorIds is not given`() {
            val updated = persisted.update(title = "改訂版")

            assertThat(updated.authorIds).isEqualTo(persisted.authorIds)
        }

        @Test
        @DisplayName("id・createdAtは更新後も維持される")
        fun `keeps id and createdAt unchanged`() {
            val updated = persisted.update(title = "改訂版")

            assertThat(updated.id).isEqualTo(persisted.id)
            assertThat(updated.createdAt).isEqualTo(persisted.createdAt)
        }

        @Test
        @DisplayName("更新後のtitleが空白のみになる場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to a blank title`() {
            assertThatIllegalArgumentException().isThrownBy { persisted.update(title = "   ") }
        }

        @Test
        @DisplayName("更新後のtitleが255文字を超える場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to a title longer than 255 characters`() {
            val tooLongTitle = "a".repeat(256)

            assertThatIllegalArgumentException().isThrownBy { persisted.update(title = tooLongTitle) }
        }

        @Test
        @DisplayName("更新後のpriceが負の値になる場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to a negative price`() {
            assertThatIllegalArgumentException().isThrownBy { persisted.update(price = BigDecimal("-1")) }
        }

        @Test
        @DisplayName("authorIdsに空リストを指定した場合、IllegalArgumentExceptionを投げる")
        fun `rejects updating to an empty author list`() {
            assertThatIllegalArgumentException().isThrownBy { persisted.update(authorIds = emptyList()) }
        }

        @Test
        @DisplayName("PUBLISHEDからUNPUBLISHEDへ変更しようとすると、InvalidPublicationStatusTransitionExceptionを投げる")
        fun `rejects changing publicationStatus from PUBLISHED to UNPUBLISHED`() {
            val published = persisted.update(publicationStatus = PublicationStatus.PUBLISHED)

            assertThatThrownBy {
                published.update(publicationStatus = PublicationStatus.UNPUBLISHED)
            }.isInstanceOf(InvalidPublicationStatusTransitionException::class.java)
        }

        @Test
        @DisplayName("UNPUBLISHEDからPUBLISHEDへの変更は許可される")
        fun `allows changing publicationStatus from UNPUBLISHED to PUBLISHED`() {
            val updated = persisted.update(publicationStatus = PublicationStatus.PUBLISHED)

            assertThat(updated.publicationStatus).isEqualTo(PublicationStatus.PUBLISHED)
        }

        @Test
        @DisplayName("PUBLISHEDのままでの更新（変更なし）は許可される")
        fun `allows keeping publicationStatus as PUBLISHED`() {
            val published = persisted.update(publicationStatus = PublicationStatus.PUBLISHED)

            val updated = published.update(publicationStatus = PublicationStatus.PUBLISHED)

            assertThat(updated.publicationStatus).isEqualTo(PublicationStatus.PUBLISHED)
        }

        @Test
        @DisplayName("未永続化（id=null）の書籍に対して呼び出すと、IllegalArgumentExceptionを投げる")
        fun `rejects updating a book that has not been persisted yet`() {
            val unpersisted = Book.register(
                title = "テスト書籍",
                price = BigDecimal.ZERO,
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorIds = listOf(AuthorId(1L)),
            )

            assertThatIllegalArgumentException().isThrownBy { unpersisted.update(title = "テスト書籍2") }
        }
    }
}