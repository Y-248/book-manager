package com.github.y248.book_manager.book.domain

import com.github.y248.book_manager.author.domain.AuthorId
import java.math.BigDecimal
import java.time.OffsetDateTime

class Book private constructor(
    val id: BookId?,
    val title: String,
    val price: BigDecimal,
    val publicationStatus: PublicationStatus,
    val authorIds: List<AuthorId>,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
) {
    /**
     * 書籍情報を更新する。指定しなかった項目は現在の値を維持する（PATCHセマンティクス）。
     * authorIdsを指定した場合は紐付けをそのリストの内容で完全に置き換える（未指定の場合は現在の紐付けを維持）。
     */
    fun update(
        title: String? = null,
        price: BigDecimal? = null,
        publicationStatus: PublicationStatus? = null,
        authorIds: List<AuthorId>? = null,
    ): Book {
        requireNotNull(id) { "cannot update a book that has not been persisted yet" }
        val newTitle = title ?: this.title
        val newPrice = price ?: this.price
        val newPublicationStatus = publicationStatus ?: this.publicationStatus
        val newAuthorIds = authorIds ?: this.authorIds

        if (this.publicationStatus == PublicationStatus.PUBLISHED && newPublicationStatus == PublicationStatus.UNPUBLISHED) {
            throw InvalidPublicationStatusTransitionException(this.publicationStatus, newPublicationStatus)
        }
        validate(newTitle, newPrice, newAuthorIds)

        return Book(id, newTitle, newPrice, newPublicationStatus, newAuthorIds, createdAt, updatedAt)
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 255

        /**
         * 新規書籍を登録する。DBのCHECK制約（price >= 0）と、
         * DB制約だけでは表現できない「書籍は最低1人の著者を持つ」というビジネスルールをここで担保する。
         */
        fun register(
            title: String,
            price: BigDecimal,
            publicationStatus: PublicationStatus,
            authorIds: List<AuthorId>,
        ): Book {
            validate(title, price, authorIds)
            return Book(
                id = null,
                title = title,
                price = price,
                publicationStatus = publicationStatus,
                authorIds = authorIds,
                createdAt = null,
                updatedAt = null,
            )
        }

        /**
         * 永続化済みの値からBookを復元する。DBから読み出した値は正当であることが前提のため、再バリデーションは行わない。
         */
        fun reconstruct(
            id: BookId,
            title: String,
            price: BigDecimal,
            publicationStatus: PublicationStatus,
            authorIds: List<AuthorId>,
            createdAt: OffsetDateTime,
            updatedAt: OffsetDateTime,
        ): Book = Book(id, title, price, publicationStatus, authorIds, createdAt, updatedAt)

        private fun validate(title: String, price: BigDecimal, authorIds: List<AuthorId>) {
            require(title.isNotBlank()) { "title must not be blank" }
            require(title.length <= MAX_TITLE_LENGTH) { "title must be at most $MAX_TITLE_LENGTH characters" }
            require(price >= BigDecimal.ZERO) { "price must be greater than or equal to 0" }
            require(authorIds.isNotEmpty()) { "a book must have at least one author" }
        }
    }
}