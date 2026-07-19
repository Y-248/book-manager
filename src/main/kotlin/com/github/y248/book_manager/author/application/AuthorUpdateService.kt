package com.github.y248.book_manager.author.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class AuthorUpdateService(
    private val authorRepository: AuthorRepository,
) {
    @Transactional
    fun update(id: AuthorId, name: String?, birthDate: LocalDate?): Author {
        val existing = authorRepository.findById(id) ?: throw AuthorNotFoundException(id)
        val updated = existing.update(name = name, birthDate = birthDate)

        if (authorRepository.existsByNameAndBirthDateExcluding(id, updated.name, updated.birthDate)) {
            throw DuplicateAuthorException(updated.name, updated.birthDate)
        }

        return authorRepository.update(updated)
    }
}