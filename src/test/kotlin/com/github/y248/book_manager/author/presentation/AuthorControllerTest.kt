package com.github.y248.book_manager.author.presentation

import com.github.y248.book_manager.author.application.AuthorRegistrationService
import com.github.y248.book_manager.author.application.AuthorUpdateService
import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.OffsetDateTime

@WebMvcTest(AuthorController::class)
@DisplayName("AuthorControllerは著者に関するAPIを扱う")
class AuthorControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authorRegistrationService: AuthorRegistrationService

    @MockitoBean
    private lateinit var authorUpdateService: AuthorUpdateService

    @Nested
    @DisplayName("POST /api/v1/authorsは著者登録リクエストを受け付け、バリデーション結果や登録結果に応じたレスポンスを返す")
    inner class Register {

        @Test
        @DisplayName("正常なリクエストの場合、著者を登録して201 Createdと登録内容を返す")
        fun `register returns 201 with the registered author`() {
            val birthDate = LocalDate.of(1867, 2, 9)
            val registered = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目漱石",
                birthDate = birthDate,
                createdAt = OffsetDateTime.parse("2026-07-19T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-07-19T00:00:00+09:00"),
            )
            `when`(authorRegistrationService.register(name = "夏目漱石", birthDate = birthDate)).thenReturn(registered)

            mockMvc.perform(
                post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"夏目漱石","birthDate":"1867-02-09"}""")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("夏目漱石"))
                .andExpect(jsonPath("$.birthDate").value("1867-02-09"))
        }

        @Test
        @DisplayName("nameが空文字の場合、著者登録は行わず400 Bad Requestとnameのバリデーションエラーを返す")
        fun `register returns 400 when name is blank`() {
            mockMvc.perform(
                post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","birthDate":"1867-02-09"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("name"))

            verifyNoInteractions(authorRegistrationService)
        }

        @Test
        @DisplayName("nameが255文字を超える場合、著者登録は行わず400 Bad Requestとnameのバリデーションエラーを返す")
        fun `register returns 400 when name is longer than 255 characters`() {
            val tooLongName = "a".repeat(256)

            mockMvc.perform(
                post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"$tooLongName","birthDate":"1867-02-09"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("name"))

            verifyNoInteractions(authorRegistrationService)
        }

        @Test
        @DisplayName("birthDateが未来日の場合、著者登録は行わず400 Bad RequestとbirthDateのバリデーションエラーを返す")
        fun `register returns 400 when birthDate is in the future`() {
            val tomorrow = LocalDate.now().plusDays(1)

            mockMvc.perform(
                post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"テスト太郎","birthDate":"$tomorrow"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"))

            verifyNoInteractions(authorRegistrationService)
        }

        @Test
        @DisplayName("birthDateがnullの場合、著者登録は行わず400 Bad RequestとbirthDateのバリデーションエラーを返す")
        fun `register returns 400 when birthDate is null`() {
            mockMvc.perform(
                post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"テスト太郎","birthDate":null}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"))

            verifyNoInteractions(authorRegistrationService)
        }

        @Test
        @DisplayName("同名・同生年月日の著者が既に存在する場合、409 Conflictを返す")
        fun `register returns 409 when the author already exists`() {
            val birthDate = LocalDate.of(1867, 2, 9)
            `when`(authorRegistrationService.register(name = "夏目漱石", birthDate = birthDate))
                .thenThrow(DuplicateAuthorException("夏目漱石", birthDate))

            mockMvc.perform(
                post("/api/v1/authors")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"夏目漱石","birthDate":"1867-02-09"}""")
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.status").value(409))
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/authors/{authorId}は著者更新リクエストを受け付け、バリデーション結果や更新結果に応じたレスポンスを返す")
    inner class Update {

        @Test
        @DisplayName("正常なリクエストの場合、著者を更新して200 OKと更新後の内容を返す")
        fun `update returns 200 with the updated author`() {
            val updated = Author.reconstruct(
                id = AuthorId(1L),
                name = "夏目金之助",
                birthDate = LocalDate.of(1867, 2, 9),
                createdAt = OffsetDateTime.parse("2026-01-01T00:00:00+09:00"),
                updatedAt = OffsetDateTime.parse("2026-07-20T00:00:00+09:00"),
            )
            `when`(authorUpdateService.update(id = AuthorId(1L), name = "夏目金之助", birthDate = null))
                .thenReturn(updated)

            mockMvc.perform(
                patch("/api/v1/authors/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"夏目金之助"}""")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("夏目金之助"))
                .andExpect(jsonPath("$.birthDate").value("1867-02-09"))
        }

        @Test
        @DisplayName("nameが255文字を超える場合、著者更新は行わず400 Bad Requestとnameのバリデーションエラーを返す")
        fun `update returns 400 when name is longer than 255 characters`() {
            val tooLongName = "a".repeat(256)

            mockMvc.perform(
                patch("/api/v1/authors/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"$tooLongName"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("name"))

            verifyNoInteractions(authorUpdateService)
        }

        @Test
        @DisplayName("birthDateが未来日の場合、著者更新は行わず400 Bad RequestとbirthDateのバリデーションエラーを返す")
        fun `update returns 400 when birthDate is in the future`() {
            val tomorrow = LocalDate.now().plusDays(1)

            mockMvc.perform(
                patch("/api/v1/authors/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"birthDate":"$tomorrow"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.errors[0].field").value("birthDate"))

            verifyNoInteractions(authorUpdateService)
        }

        @Test
        @DisplayName("対象のauthorIdの著者が存在しない場合、404 Not Foundを返す")
        fun `update returns 404 when the author does not exist`() {
            `when`(authorUpdateService.update(id = AuthorId(999L), name = "夏目金之助", birthDate = null))
                .thenThrow(AuthorNotFoundException(AuthorId(999L)))

            mockMvc.perform(
                patch("/api/v1/authors/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"夏目金之助"}""")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.status").value(404))
        }

        @Test
        @DisplayName("更新後の内容が自分以外の著者とname・birthDateで重複する場合、409 Conflictを返す")
        fun `update returns 409 when the updated content duplicates another author`() {
            `when`(authorUpdateService.update(id = AuthorId(1L), name = "森鴎外", birthDate = null))
                .thenThrow(DuplicateAuthorException("森鴎外", LocalDate.of(1867, 2, 9)))

            mockMvc.perform(
                patch("/api/v1/authors/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"森鴎外"}""")
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.status").value(409))
        }
    }
}