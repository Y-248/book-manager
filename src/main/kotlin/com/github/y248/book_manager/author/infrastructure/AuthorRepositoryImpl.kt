package com.github.y248.book_manager.author.infrastructure

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import com.github.y248.book_manager.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class AuthorRepositoryImpl(
    private val dsl: DSLContext,
) : AuthorRepository {

    override fun existsByNameAndBirthDate(name: String, birthDate: LocalDate): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(AUTHORS)
                .where(AUTHORS.NAME.eq(name))
                .and(AUTHORS.BIRTH_DATE.eq(birthDate))
        )

    override fun save(author: Author): Author {
        val record = try {
            dsl.insertInto(AUTHORS)
                .set(AUTHORS.NAME, author.name)
                .set(AUTHORS.BIRTH_DATE, author.birthDate)
                .returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE, AUTHORS.CREATED_AT, AUTHORS.UPDATED_AT)
                .fetchOne()
                ?: error("Failed to insert author: no record returned")
        } catch (e: DuplicateKeyException) {
            throw DuplicateAuthorException(author.name, author.birthDate)
        }

        return Author.reconstruct(
            id = AuthorId(requireNotNull(record.get(AUTHORS.ID))),
            name = requireNotNull(record.get(AUTHORS.NAME)),
            birthDate = requireNotNull(record.get(AUTHORS.BIRTH_DATE)),
            createdAt = requireNotNull(record.get(AUTHORS.CREATED_AT)),
            updatedAt = requireNotNull(record.get(AUTHORS.UPDATED_AT)),
        )
    }
}