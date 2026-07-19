package com.github.y248.book_manager.book.application

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import org.springframework.stereotype.Component

/**
 * 書籍の登録・更新で共通して必要になる著者指定(AuthorSpec)の解決処理。
 * 既存著者への紐付け・新規著者の登録（バルクインサート）をまとめて行う。
 */
@Component
class AuthorSpecResolver(
    private val authorRepository: AuthorRepository,
) {
    /**
     * authorSpecsで指定された順序を維持したまま、既存著者への紐付け・新規著者の登録を解決する。
     */
    fun resolve(authorSpecs: List<AuthorSpec>): List<Author> {
        val existingSpecs = authorSpecs.filterIsInstance<AuthorSpec.Existing>()
        rejectIfDuplicatedAuthorId(existingSpecs)
        val existingAuthorsById: Map<AuthorId, Author> = existingSpecs.associate { spec ->
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

    private fun rejectIfDuplicatedAuthorId(existingSpecs: List<AuthorSpec.Existing>) {
        val duplicated = existingSpecs
            .groupBy { it.authorId }
            .values
            .firstOrNull { it.size > 1 }
            ?.first()
            ?: return
        throw IllegalArgumentException("authorId ${duplicated.authorId.value} is specified more than once")
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