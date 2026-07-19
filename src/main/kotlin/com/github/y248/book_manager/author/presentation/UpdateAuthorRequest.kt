package com.github.y248.book_manager.author.presentation

import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * PATCHセマンティクスのため全項目が任意。指定しなかった項目は現在の値を維持する。
 * nameの空文字チェックは、未指定(null)を許容する必要があるため@NotBlankではなくドメイン層(Author.update())で行う。
 */
data class UpdateAuthorRequest(
    @field:Size(max = 255, message = "must be at most 255 characters")
    val name: String? = null,

    @field:PastOrPresent(message = "must be a past or present date")
    val birthDate: LocalDate? = null,
)