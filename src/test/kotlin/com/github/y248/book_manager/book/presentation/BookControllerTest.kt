package com.github.y248.book_manager.book.presentation

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import com.github.y248.book_manager.book.application.AuthorSpec
import com.github.y248.book_manager.book.application.BookRegistrationService
import com.github.y248.book_manager.book.application.RegisteredBook
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

@WebMvcTest(BookController::class)
@DisplayName("BookControllerは書籍に関するAPIを扱う")
class BookControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var bookRegistrationService: BookRegistrationService

    @Nested
    @DisplayName("POST /api/v1/booksは書籍登録リクエストを受け付け、バリデーション結果や登録結果に応じたレスポンスを返す")
    inner class Register {

        @Test
        @DisplayName("正常なリクエストの場合、書籍を登録して201 Createdと登録内容（著者情報を含む）を返す")
        fun `register returns 201 with the registered book`() {
            val author = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            )
            val book = Book.reconstruct(
                id = BookId(10L),
                title = "吾輩は猫である",
                price = BigDecimal("1200.00"),
                publicationStatus = PublicationStatus.UNPUBLISHED,
                authorIds = listOf(AuthorId(1L)),
                createdAt = OffsetDateTime.parse("2026-07-20T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-07-20T00:00:00+09:00"),
            )
            `when`(
                bookRegistrationService.register(
                    title = "吾輩は猫である",
                    price = BigDecimal("1200"),
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = listOf(AuthorSpec.Existing(AuthorId(1L))),
                )
            ).thenReturn(RegisteredBook(book, listOf(author)))

            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"title":"吾輩は猫である","price":1200,"authors":[{"authorId":1}]}"""
                    )
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.title").value("吾輩は猫である"))
                .andExpect(jsonPath("$.publicationStatus").value("UNPUBLISHED"))
                .andExpect(jsonPath("$.authors[0].id").value(1))
                .andExpect(jsonPath("$.authors[0].name").value("夏目漱石"))
        }

        @Test
        @DisplayName("titleが空文字の場合、書籍登録は行わず400 Bad Requestとtitleのバリデーションエラーを返す")
        fun `register returns 400 when title is blank`() {
            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"","price":1000,"authors":[{"authorId":1}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("title"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("titleが255文字を超える場合、書籍登録は行わず400 Bad Requestとtitleのバリデーションエラーを返す")
        fun `register returns 400 when title is longer than 255 characters`() {
            val tooLongTitle = "a".repeat(256)

            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"$tooLongTitle","price":1000,"authors":[{"authorId":1}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("title"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("priceが負の値の場合、書籍登録は行わず400 Bad Requestとpriceのバリデーションエラーを返す")
        fun `register returns 400 when price is negative`() {
            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"テスト書籍","price":-1,"authors":[{"authorId":1}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("price"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("priceがnullの場合、書籍登録は行わず400 Bad Requestとpriceのバリデーションエラーを返す")
        fun `register returns 400 when price is null`() {
            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"テスト書籍","price":null,"authors":[{"authorId":1}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("price"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("publicationStatusがUNPUBLISHED・PUBLISHED以外の場合、書籍登録は行わず400 Bad RequestとpublicationStatusのバリデーションエラーを返す")
        fun `register returns 400 when publicationStatus is invalid`() {
            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"テスト書籍","price":1000,"publicationStatus":"FOO","authors":[{"authorId":1}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("publicationStatus"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("authorsが空配列の場合、書籍登録は行わず400 Bad Requestとauthorsのバリデーションエラーを返す")
        fun `register returns 400 when authors is empty`() {
            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"テスト書籍","price":1000,"authors":[]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authors"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("新規著者のnameが255文字を超える場合、書籍登録は行わず400 Bad Requestとauthors[0].nameのバリデーションエラーを返す")
        fun `register returns 400 when a new author's name is longer than 255 characters`() {
            val tooLongName = "a".repeat(256)

            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"title":"テスト書籍","price":1000,"authors":[{"name":"$tooLongName","birthDate":"2000-01-01"}]}"""
                    )
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authors[0].name"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("新規著者のbirthDateが未来日の場合、書籍登録は行わず400 Bad Requestとauthors[0].birthDateのバリデーションエラーを返す")
        fun `register returns 400 when a new author's birthDate is in the future`() {
            val tomorrow = LocalDate.now().plusDays(1)

            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"title":"テスト書籍","price":1000,"authors":[{"name":"テスト太郎","birthDate":"$tomorrow"}]}"""
                    )
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authors[0].birthDate"))

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("authorIdとname・birthDateを同時に指定した場合、書籍登録は行わず400 Bad Requestを返す")
        fun `register returns 400 when an author specifies both authorId and name-birthDate`() {
            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"title":"テスト書籍","price":1000,"authors":[{"authorId":1,"name":"誰か","birthDate":"2000-01-01"}]}"""
                    )
            )
                .andExpect(status().isBadRequest)

            verifyNoInteractions(bookRegistrationService)
        }

        @Test
        @DisplayName("存在しないauthorIdが指定された場合、404 Not Foundを返す")
        fun `register returns 404 when authorId does not exist`() {
            `when`(
                bookRegistrationService.register(
                    title = "テスト書籍",
                    price = BigDecimal("1000"),
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = listOf(AuthorSpec.Existing(AuthorId(999L))),
                )
            ).thenThrow(AuthorNotFoundException(AuthorId(999L)))

            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"テスト書籍","price":1000,"authors":[{"authorId":999}]}""")
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("新規著者として指定されたname・birthDateが既存著者と重複する場合、409 Conflictを返す")
        fun `register returns 409 when a new author duplicates an existing one`() {
            `when`(
                bookRegistrationService.register(
                    title = "テスト書籍",
                    price = BigDecimal("1000"),
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = listOf(AuthorSpec.New(name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))),
                )
            ).thenThrow(DuplicateAuthorException("夏目漱石", LocalDate.of(1867, 2, 9)))

            mockMvc.perform(
                post("/api/v1/books")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"title":"テスト書籍","price":1000,"authors":[{"name":"夏目漱石","birthDate":"1867-02-09"}]}"""
                    )
            )
                .andExpect(status().isConflict)
        }
    }
}