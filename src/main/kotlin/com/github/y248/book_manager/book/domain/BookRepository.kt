package com.github.y248.book_manager.book.domain

interface BookRepository {
    /**
     * booksへの登録と、book_author_relationsへの紐付け登録（バルクインサート）を同一トランザクションで行う。
     */
    fun save(book: Book): Book
}