package com.github.y248.book_manager.book.presentation

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import com.github.y248.book_manager.book.application.AuthorSpec
import com.github.y248.book_manager.book.application.BookRegistrationService
import com.github.y248.book_manager.book.application.BookSearchService
import com.github.y248.book_manager.book.application.BookUpdateService
import com.github.y248.book_manager.book.application.BookWithAuthors
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookNotFoundException
import com.github.y248.book_manager.book.domain.InvalidPublicationStatusTransitionException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
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

    @MockitoBean
    private lateinit var bookUpdateService: BookUpdateService

    @MockitoBean
    private lateinit var bookSearchService: BookSearchService

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
            ).thenReturn(BookWithAuthors(book, listOf(author)))

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

    @Nested
    @DisplayName("PATCH /api/v1/books/{bookId}は書籍更新リクエストを受け付け、バリデーション結果や更新結果に応じたレスポンスを返す")
    inner class Update {

        @Test
        @DisplayName("正常なリクエストの場合、書籍を更新して200 OKと更新後の内容（著者情報を含む）を返す")
        fun `update returns 200 with the updated book`() {
            val newAuthor = Author.reconstruct(
                id = AuthorId(2L),
                name = "森鴎外",
                birthDate = LocalDate.of(1862, 2, 17),
                createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            )
            val updatedBook = Book.reconstruct(
                id = BookId(10L),
                title = "吾輩は猫である（改訂版）",
                price = BigDecimal("1500.00"),
                publicationStatus = PublicationStatus.PUBLISHED,
                authorIds = listOf(AuthorId(2L)),
                createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-07-20T00:00:00+09:00"),
            )
            `when`(
                bookUpdateService.update(
                    id = BookId(10L),
                    title = "吾輩は猫である（改訂版）",
                    price = BigDecimal("1500"),
                    publicationStatus = PublicationStatus.PUBLISHED,
                    authorSpecs = listOf(AuthorSpec.Existing(AuthorId(2L))),
                )
            ).thenReturn(BookWithAuthors(updatedBook, listOf(newAuthor)))

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"title":"吾輩は猫である（改訂版）","price":1500,"publicationStatus":"PUBLISHED","authors":[{"authorId":2}]}"""
                    )
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.title").value("吾輩は猫である（改訂版）"))
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.authors[0].id").value(2))
                .andExpect(jsonPath("$.authors[0].name").value("森鴎外"))
        }

        @Test
        @DisplayName("titleが255文字を超える場合、書籍更新は行わず400 Bad Requestとtitleのバリデーションエラーを返す")
        fun `update returns 400 when title is longer than 255 characters`() {
            val tooLongTitle = "a".repeat(256)

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"$tooLongTitle"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("title"))

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("priceが負の値の場合、書籍更新は行わず400 Bad Requestとpriceのバリデーションエラーを返す")
        fun `update returns 400 when price is negative`() {
            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"price":-1}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("price"))

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("publicationStatusがUNPUBLISHED・PUBLISHED以外の場合、書籍更新は行わず400 Bad RequestとpublicationStatusのバリデーションエラーを返す")
        fun `update returns 400 when publicationStatus is invalid`() {
            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"publicationStatus":"FOO"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("publicationStatus"))

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("authorsに空配列を指定した場合、書籍更新は行わず400 Bad Requestとauthorsのバリデーションエラーを返す")
        fun `update returns 400 when authors is an empty array`() {
            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"authors":[]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authors"))

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("新規著者のnameが255文字を超える場合、書籍更新は行わず400 Bad Requestとauthors[0].nameのバリデーションエラーを返す")
        fun `update returns 400 when a new author's name is longer than 255 characters`() {
            val tooLongName = "a".repeat(256)

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"authors":[{"name":"$tooLongName","birthDate":"2000-01-01"}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authors[0].name"))

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("新規著者のbirthDateが未来日の場合、書籍更新は行わず400 Bad Requestとauthors[0].birthDateのバリデーションエラーを返す")
        fun `update returns 400 when a new author's birthDate is in the future`() {
            val tomorrow = LocalDate.now().plusDays(1)

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"authors":[{"name":"テスト太郎","birthDate":"$tomorrow"}]}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authors[0].birthDate"))

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("authorIdとname・birthDateを同時に指定した場合、書籍更新は行わず400 Bad Requestを返す")
        fun `update returns 400 when an author specifies both authorId and name-birthDate`() {
            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"authors":[{"authorId":1,"name":"誰か","birthDate":"2000-01-01"}]}""")
            )
                .andExpect(status().isBadRequest)

            verifyNoInteractions(bookUpdateService)
        }

        @Test
        @DisplayName("対象のbookIdの書籍が存在しない場合、404 Not Foundを返す")
        fun `update returns 404 when the book does not exist`() {
            `when`(
                bookUpdateService.update(
                    id = BookId(999L),
                    title = "改訂版",
                    price = null,
                    publicationStatus = null,
                    authorSpecs = null,
                )
            ).thenThrow(BookNotFoundException(BookId(999L)))

            mockMvc.perform(
                patch("/api/v1/books/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"title":"改訂版"}""")
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("存在しないauthorIdが指定された場合、404 Not Foundを返す")
        fun `update returns 404 when authorId does not exist`() {
            `when`(
                bookUpdateService.update(
                    id = BookId(10L),
                    title = null,
                    price = null,
                    publicationStatus = null,
                    authorSpecs = listOf(AuthorSpec.Existing(AuthorId(999L))),
                )
            ).thenThrow(AuthorNotFoundException(AuthorId(999L)))

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"authors":[{"authorId":999}]}""")
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("新規著者として指定されたname・birthDateが既存著者と重複する場合、409 Conflictを返す")
        fun `update returns 409 when a new author duplicates an existing one`() {
            `when`(
                bookUpdateService.update(
                    id = BookId(10L),
                    title = null,
                    price = null,
                    publicationStatus = null,
                    authorSpecs = listOf(AuthorSpec.New(name = "夏目漱石", birthDate = LocalDate.of(1867, 2, 9))),
                )
            ).thenThrow(DuplicateAuthorException("夏目漱石", LocalDate.of(1867, 2, 9)))

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"authors":[{"name":"夏目漱石","birthDate":"1867-02-09"}]}""")
            )
                .andExpect(status().isConflict)
        }

        @Test
        @DisplayName("publicationStatusをPUBLISHEDからUNPUBLISHEDに変更しようとした場合、409 Conflictを返す")
        fun `update returns 409 when changing publicationStatus from PUBLISHED to UNPUBLISHED`() {
            `when`(
                bookUpdateService.update(
                    id = BookId(10L),
                    title = null,
                    price = null,
                    publicationStatus = PublicationStatus.UNPUBLISHED,
                    authorSpecs = null,
                )
            ).thenThrow(InvalidPublicationStatusTransitionException(PublicationStatus.PUBLISHED, PublicationStatus.UNPUBLISHED))

            mockMvc.perform(
                patch("/api/v1/books/10")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"publicationStatus":"UNPUBLISHED"}""")
            )
                .andExpect(status().isConflict)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/books?authorId={authorId}は指定した著者に紐づく書籍一覧を返す")
    inner class Search {

        @Test
        @DisplayName("紐づく書籍が存在する場合、200 OKと書籍一覧（各書籍の著者情報を含む）を返す")
        fun `search returns 200 with books linked to the author`() {
            val author = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            )
            val book = Book.reconstruct(
                id = BookId(10L),
                title = "こころ",
                price = BigDecimal("550.00"),
                publicationStatus = PublicationStatus.PUBLISHED,
                authorIds = listOf(AuthorId(1L)),
                createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
            )
            `when`(bookSearchService.findByAuthorId(AuthorId(1L))).thenReturn(listOf(BookWithAuthors(book, listOf(author))))

            mockMvc.perform(get("/api/v1/books").param("authorId", "1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.books[0].id").value(10))
                .andExpect(jsonPath("$.books[0].title").value("こころ"))
                .andExpect(jsonPath("$.books[0].authors[0].id").value(1))
                .andExpect(jsonPath("$.books[0].authors[0].name").value("夏目漱石"))
        }

        @Test
        @DisplayName("紐づく書籍が存在しない場合、200 OKと空のbooksを返す（著者が存在しない場合も同様）")
        fun `search returns 200 with an empty list when there are no linked books`() {
            `when`(bookSearchService.findByAuthorId(AuthorId(999L))).thenReturn(emptyList())

            mockMvc.perform(get("/api/v1/books").param("authorId", "999"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.books").isEmpty)
        }

        @Test
        @DisplayName("authorIdを指定しない場合、400 Bad Requestを返す")
        fun `search returns 400 when authorId is not given`() {
            mockMvc.perform(get("/api/v1/books"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authorId"))

            verifyNoInteractions(bookSearchService)
        }

        @Test
        @DisplayName("authorIdが数値以外の場合、400 Bad Requestを返す")
        fun `search returns 400 when authorId is not a number`() {
            mockMvc.perform(get("/api/v1/books").param("authorId", "abc"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("authorId"))

            verifyNoInteractions(bookSearchService)
        }
    }
}