package com.github.y248.book_manager.shared.presentation

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.OffsetDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val errors: List<FieldError>? = null,
) {
    data class FieldError(
        val field: String,
        val message: String,
    )
}