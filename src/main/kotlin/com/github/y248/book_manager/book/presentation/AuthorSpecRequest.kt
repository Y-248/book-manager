package com.github.y248.book_manager.book.presentation

import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate

/**
 * authorIdのみ、またはname+birthDateのみを指定する（どちらか一方）。
 * この組み合わせチェック自体はBean Validationでは表現しづらいため、
 * book.application.AuthorSpec.of()で行う。
 */
data class AuthorSpecRequest(
    val authorId: Long? = null,

    @field:Size(max = 255, message = "must be at most 255 characters")
    val name: String? = null,

    @field:PastOrPresent(message = "must be a past or present date")
    val birthDate: LocalDate? = null,
)