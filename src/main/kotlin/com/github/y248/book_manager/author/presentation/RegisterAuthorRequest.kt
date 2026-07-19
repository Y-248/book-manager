package com.github.y248.book_manager.author.presentation

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class RegisterAuthorRequest(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 255, message = "must be at most 255 characters")
    val name: String?,

    @field:NotNull(message = "must not be null")
    @field:PastOrPresent(message = "must be a past or present date")
    val birthDate: LocalDate?,
)