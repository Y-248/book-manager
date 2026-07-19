package com.github.y248.book_manager.book.domain

import com.github.y248.book_manager.author.domain.AuthorId
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalArgumentException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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
}