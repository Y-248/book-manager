package com.github.y248.book_manager.book.presentation

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.math.BigDecimal

/**
 * PATCHセマンティクスのため全項目が任意。指定しなかった項目は現在の値を維持する。
 * titleの空文字チェックは、未指定(null)を許容する必要があるため@NotBlankではなくドメイン層(Book.update())で行う。
 * authorsは指定時のみ紐付けを完全に置き換える（未指定なら現在の紐付けを維持）。
 */
data class UpdateBookRequest(
    @field:Size(max = 255, message = "must be at most 255 characters")
    val title: String? = null,

    @field:DecimalMin(value = "0", message = "must be greater than or equal to 0")
    val price: BigDecimal? = null,

    @field:Pattern(regexp = "UNPUBLISHED|PUBLISHED", message = "must be UNPUBLISHED or PUBLISHED")
    val publicationStatus: String? = null,

    @field:Size(min = 1, message = "must contain at least one author")
    @field:Valid
    val authors: List<AuthorSpecRequest>? = null,
)