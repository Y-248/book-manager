package com.github.y248.book_manager.author.domain

import java.time.LocalDate

interface AuthorRepository {
    fun existsByNameAndBirthDate(name: String, birthDate: LocalDate): Boolean

    /**
     * 更新対象の著者自身(id)は重複判定から除外して、他の著者とのname/birthDateの重複有無を調べる。
     */
    fun existsByNameAndBirthDateExcluding(id: AuthorId, name: String, birthDate: LocalDate): Boolean

    fun findById(id: AuthorId): Author?

    fun save(author: Author): Author

    /**
     * 複数の新規著者を1回のバルクインサートで登録する（書籍登録APIで新規著者が複数指定された場合に使用）。
     */
    fun saveAll(authors: List<Author>): List<Author>

    fun update(author: Author): Author
}