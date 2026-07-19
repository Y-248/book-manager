package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.book.domain.Book

/**
 * 登録結果。レスポンス構築時に各著者の名前・生年月日も必要になるため、Book（著者IDの参照のみ持つ）と
 * 解決済みのAuthor一覧をセットで返す。
 */
data class RegisteredBook(val book: Book, val authors: List<Author>)