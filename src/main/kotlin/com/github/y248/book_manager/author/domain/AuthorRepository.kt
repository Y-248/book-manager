package com.github.y248.book_manager.author.domain

import java.time.LocalDate

interface AuthorRepository {
    fun existsByNameAndBirthDate(name: String, birthDate: LocalDate): Boolean

    fun save(author: Author): Author
}