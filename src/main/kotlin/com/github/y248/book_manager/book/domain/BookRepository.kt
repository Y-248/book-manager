package com.github.y248.book_manager.book.domain

interface BookRepository {
    fun findById(id: BookId): Book?

    /**
     * booksへの登録と、book_author_relationsへの紐付け登録（バルクインサート）を同一トランザクションで行う。
     */
    fun save(book: Book): Book

    /**
     * booksの更新と、book_author_relationsの紐付けの完全な置き換え（削除→バルクインサート）を同一トランザクションで行う。
     */
    fun update(book: Book): Book
}