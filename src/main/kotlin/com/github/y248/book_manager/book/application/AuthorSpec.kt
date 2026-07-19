package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.AuthorId
import java.time.LocalDate

/**
 * 書籍登録・更新時の著者指定を表す。authorIdのみ指定 = 既存著者への紐付け、
 * name/birthDateを指定 = 新規著者登録＋紐付け、のいずれか一方のみを許容する。
 */
sealed interface AuthorSpec {
    data class Existing(val authorId: AuthorId) : AuthorSpec
    data class New(val name: String, val birthDate: LocalDate) : AuthorSpec

    companion object {
        fun of(authorId: Long?, name: String?, birthDate: LocalDate?): AuthorSpec {
            val hasAuthorId = authorId != null
            val hasNameAndBirthDate = name != null && birthDate != null
            require(hasAuthorId != hasNameAndBirthDate) {
                "each author must specify either authorId, or both name and birthDate, but not both or neither"
            }
            return if (authorId != null) {
                Existing(AuthorId(authorId))
            } else {
                New(name = requireNotNull(name), birthDate = requireNotNull(birthDate))
            }
        }
    }
}