package com.github.y248.book_manager.book.presentation

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class RegisterBookRequest(
    @field:NotBlank(message = "must not be blank")
    @field:Size(max = 255, message = "must be at most 255 characters")
    val title: String?,

    @field:NotNull(message = "must not be null")
    @field:DecimalMin(value = "0", message = "must be greater than or equal to 0")
    val price: BigDecimal?,

    @field:Pattern(regexp = "UNPUBLISHED|PUBLISHED", message = "must be UNPUBLISHED or PUBLISHED")
    val publicationStatus: String? = null,

    @field:NotEmpty(message = "must contain at least one author")
    @field:Valid
    val authors: List<AuthorSpecRequest>?,
)