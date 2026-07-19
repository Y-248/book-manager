package com.github.y248.book_manager.author.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AuthorRegistrationService(
    private val authorRepository: AuthorRepository,
) {
    @Transactional
    fun register(name: String, birthDate: LocalDate): Author {
        if (authorRepository.existsByNameAndBirthDate(name, birthDate)) {
            throw DuplicateAuthorException(name, birthDate)
        }
        val author = Author.register(name, birthDate)
        return authorRepository.save(author)
    }
}