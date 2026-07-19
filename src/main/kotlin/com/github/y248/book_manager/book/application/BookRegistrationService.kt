package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import com.github.y248.book_manager.book.domain.Book
import com.github.y248.book_manager.book.domain.BookRepository
import com.github.y248.book_manager.book.domain.PublicationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BookRegistrationService(
    private val bookRepository: BookRepository,
    private val authorRepository: AuthorRepository,
) {
    @Transactional
    fun register(
        title: String,
        price: BigDecimal,
        publicationStatus: PublicationStatus,
        authorSpecs: List<AuthorSpec>,
    ): RegisteredBook {
        val authors = resolveAuthors(authorSpecs)
        val book = Book.register(
            title = title,
            price = price,
            publicationStatus = publicationStatus,
            authorIds = authors.map { requireNotNull(it.id) },
        )
        val savedBook = bookRepository.save(book)
        return RegisteredBook(savedBook, authors)
    }

    /**
     * authorsで指定された順序を維持したまま、既存著者への紐付け・新規著者の登録を解決する。
     */
    private fun resolveAuthors(authorSpecs: List<AuthorSpec>): List<Author> {
        val existingAuthorsById: Map<AuthorId, Author> = authorSpecs
            .filterIsInstance<AuthorSpec.Existing>()
            .associate { spec ->
                val author = authorRepository.findById(spec.authorId) ?: throw AuthorNotFoundException(spec.authorId)
                spec.authorId to author
            }

        val newSpecs = authorSpecs.filterIsInstance<AuthorSpec.New>()
        rejectIfDuplicatedWithinRequest(newSpecs)
        newSpecs.forEach { spec ->
            if (authorRepository.existsByNameAndBirthDate(spec.name, spec.birthDate)) {
                throw DuplicateAuthorException(spec.name, spec.birthDate)
            }
        }
        val savedNewAuthors = if (newSpecs.isEmpty()) {
            emptyList()
        } else {
            authorRepository.saveAll(newSpecs.map { Author.register(it.name, it.birthDate) })
        }

        var newAuthorIndex = 0
        return authorSpecs.map { spec ->
            when (spec) {
                is AuthorSpec.Existing -> requireNotNull(existingAuthorsById[spec.authorId])
                is AuthorSpec.New -> savedNewAuthors[newAuthorIndex++]
            }
        }
    }

    private fun rejectIfDuplicatedWithinRequest(newSpecs: List<AuthorSpec.New>) {
        val duplicated = newSpecs
            .groupBy { it.name to it.birthDate }
            .values
            .firstOrNull { it.size > 1 }
            ?.first()
            ?: return
        throw DuplicateAuthorException(duplicated.name, duplicated.birthDate)
    }
}