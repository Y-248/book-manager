package com.github.y248.book_manager.book.infrastructure

import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.BookNotFoundException
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import com.github.y248.book_manager.jooq.tables.references.BOOKS
import com.github.y248.book_manager.jooq.tables.references.BOOK_AUTHOR_RELATIONS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class BookRepositoryImpl(
    private val dsl: DSLContext,
) : BookRepository {

    override fun findById(id: BookId): Book? {
        val bookRecord = dsl.select(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS, BOOKS.CREATED_AT, BOOKS.UPDATED_AT)
            .from(BOOKS)
            .where(BOOKS.ID.eq(id.value))
            .fetchOne() ?: return null

        val authorIds = dsl.select(BOOK_AUTHOR_RELATIONS.AUTHOR_ID)
            .from(BOOK_AUTHOR_RELATIONS)
            .where(BOOK_AUTHOR_RELATIONS.BOOK_ID.eq(id.value))
            .fetch(BOOK_AUTHOR_RELATIONS.AUTHOR_ID)
            .filterNotNull()
            .map { AuthorId(it) }

        return Book.reconstruct(
            id = BookId(requireNotNull(bookRecord.get(BOOKS.ID))),
            title = requireNotNull(bookRecord.get(BOOKS.TITLE)),
            price = requireNotNull(bookRecord.get(BOOKS.PRICE)),
            publicationStatus = PublicationStatus.valueOf(requireNotNull(bookRecord.get(BOOKS.PUBLICATION_STATUS))),
            authorIds = authorIds,
            createdAt = requireNotNull(bookRecord.get(BOOKS.CREATED_AT)),
            updatedAt = requireNotNull(bookRecord.get(BOOKS.UPDATED_AT)),
        )
    }

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
     * 著者の紐付けは常に「全削除→バルクインサート」で洗い替える。
     * Book.update()側で「authors未指定なら現在の紐付けを維持する」よう解決済みのため、
     * ここでは常にbook.authorIdsの内容で完全に置き換えるだけでよい。
     */
    override fun update(book: Book): Book {
        val id = requireNotNull(book.id) { "book id must not be null when updating" }
        val bookRecord = dsl.update(BOOKS)
            .set(BOOKS.TITLE, book.title)
            .set(BOOKS.PRICE, book.price)
            .set(BOOKS.PUBLICATION_STATUS, book.publicationStatus.name)
            .set(BOOKS.UPDATED_AT, OffsetDateTime.now())
            .where(BOOKS.ID.eq(id.value))
            .returning(BOOKS.ID, BOOKS.TITLE, BOOKS.PRICE, BOOKS.PUBLICATION_STATUS, BOOKS.CREATED_AT, BOOKS.UPDATED_AT)
            .fetchOne()
            ?: throw BookNotFoundException(id)

        dsl.deleteFrom(BOOK_AUTHOR_RELATIONS).where(BOOK_AUTHOR_RELATIONS.BOOK_ID.eq(id.value)).execute()
        insertAuthorRelations(id, book.authorIds)

        return Book.reconstruct(
            id = id,
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