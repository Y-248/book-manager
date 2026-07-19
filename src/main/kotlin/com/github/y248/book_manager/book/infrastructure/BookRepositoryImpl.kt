package com.github.y248.book_manager.book.infrastructure

import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import com.github.y248.book_manager.jooq.tables.references.BOOKS
import com.github.y248.book_manager.jooq.tables.references.BOOK_AUTHOR_RELATIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class BookRepositoryImpl(
    private val dsl: DSLContext,
) : BookRepository {

    override fun save(book: Book): Book {
        val bookRecord = dsl.insertInto(BOOKS)
            .set(BOOKS.TITLE, book.title)
            .set(BOOKS.PRICE, book.price)
            .set(BOOKS.PUBLICATION_STATUS, book.publicationStatus.name)
            .returning(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS, BOOKS.CREATED_AT, BOOKS.UPDATED_AT)
            .fetchOne()
            ?: error("Failed to insert book: no record returned")

        val bookId = BookId(requireNotNull(bookRecord.get(BOOKS.ID)))

        insertAuthorRelations(bookId, book.authorIds)

        return Book.reconstruct(
            id = bookId,
            title = requireNotNull(bookRecord.get(BOOKS.TITLE)),
            price = requireNotNull(bookRecord.get(BOOKS.PRICE)),
            publicationStatus = PublicationStatus.valueOf(requireNotNull(bookRecord.get(BOOKS.PUBLICATION_STATUS))),
            authorIds = book.authorIds,
            createdAt = requireNotNull(bookRecord.get(BOOKS.CREATED_AT)),
            updatedAt = requireNotNull(bookRecord.get(BOOKS.UPDATED_AT)),
        )
    }

    /**
     * 書籍と著者の紐付けはbook_author_relationsに複数行まとめて登録する。
     * このテーブルには自動生成列が無く戻り値を取得する必要が無いため、JDBCバッチ実行によるbatchInsert()を使う。
     */
    private fun insertAuthorRelations(bookId: BookId, authorIds: List<AuthorId>) {
        val records = authorIds.map { authorId ->
            dsl.newRecord(BOOK_AUTHOR_RELATIONS).apply {
                this.bookId = bookId.value
                this.authorId = authorId.value
            }
        }
        dsl.batchInsert(records).execute()
    }
}