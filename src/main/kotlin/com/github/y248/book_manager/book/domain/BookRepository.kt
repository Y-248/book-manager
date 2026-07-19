package com.github.y248.book_manager.book.domain

import com.github.y248.book_manager.author.domain.AuthorId

interface BookRepository {
    fun findById(id: BookId): Book?

    /**
     * 指定した著者に紐づく書籍を取得する。該当が無い場合は空リストを返す（著者の存在確認は行わない）。
     */
    fun findAllByAuthorId(authorId: AuthorId): List<Book>

    /**
     * booksへの登録と、book_author_relationsへの紐付け登録（バルクインサート）を同一トランザクションで行う。
     */
    fun save(book: Book): Book

    /**
     * booksの更新と、book_author_relationsの紐付けの完全な置き換え（削除→バルクインサート）を同一トランザクションで行う。
     */
    fun update(book: Book): Book
}