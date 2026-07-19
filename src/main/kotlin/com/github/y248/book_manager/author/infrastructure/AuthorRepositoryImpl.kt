package com.github.y248.book_manager.author.infrastructure

import com.github.y248.book_manager.author.domain.Author
import com.github.y248.book_manager.author.domain.AuthorId
import com.github.y248.book_manager.author.domain.AuthorNotFoundException
import com.github.y248.book_manager.author.domain.AuthorRepository
import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import com.github.y248.book_manager.jooq.tables.references.AUTHORS
import org.jooq.DSLContext
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime

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

    override fun existsByNameAndBirthDateExcluding(id: AuthorId, name: String, birthDate: LocalDate): Boolean =
        dsl.fetchExists(
            dsl.selectOne()
                .from(AUTHORS)
                .where(AUTHORS.NAME.eq(name))
                .and(AUTHORS.BIRTH_DATE.eq(birthDate))
                .and(AUTHORS.ID.ne(id.value))
        )

    override fun findById(id: AuthorId): Author? {
        val record = dsl.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE, AUTHORS.CREATED_AT, AUTHORS.UPDATED_AT)
            .from(AUTHORS)
            .where(AUTHORS.ID.eq(id.value))
            .fetchOne() ?: return null

        return Author.reconstruct(
            id = AuthorId(requireNotNull(record.get(AUTHORS.ID))),
            name = requireNotNull(record.get(AUTHORS.NAME)),
            birthDate = requireNotNull(record.get(AUTHORS.BIRTH_DATE)),
            createdAt = requireNotNull(record.get(AUTHORS.CREATED_AT)),
            updatedAt = requireNotNull(record.get(AUTHORS.UPDATED_AT)),
        )
    }

    override fun findAllByIds(ids: List<AuthorId>): List<Author> {
        if (ids.isEmpty()) return emptyList()

        val records = dsl.select(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE, AUTHORS.CREATED_AT, AUTHORS.UPDATED_AT)
            .from(AUTHORS)
            .where(AUTHORS.ID.`in`(ids.map { it.value }))
            .fetch()

        return records.map { record ->
            Author.reconstruct(
                id = AuthorId(requireNotNull(record.get(AUTHORS.ID))),
                name = requireNotNull(record.get(AUTHORS.NAME)),
                birthDate = requireNotNull(record.get(AUTHORS.BIRTH_DATE)),
                createdAt = requireNotNull(record.get(AUTHORS.CREATED_AT)),
                updatedAt = requireNotNull(record.get(AUTHORS.UPDATED_AT)),
            )
        }
    }

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

    /**
     * 複数著者を1本のINSERT文（マルチVALUES + RETURNING）でまとめて登録する。
     * jOOQのbatchInsert()はJDBCバッチ実行になり生成されたid等を確実に取得できないため、
     * 生成列を返す必要があるここでは複数行RETURNINGが取れるマルチVALUES方式を使っている
     * （生成列が不要なbook_author_relationsの登録ではbatchInsert()を使用: BookRepositoryImpl参照）。
     */
    override fun saveAll(authors: List<Author>): List<Author> {
        if (authors.isEmpty()) return emptyList()

        var insert = dsl.insertInto(AUTHORS, AUTHORS.NAME, AUTHORS.BIRTH_DATE)
            .values(authors[0].name, authors[0].birthDate)
        for (author in authors.drop(1)) {
            insert = insert.values(author.name, author.birthDate)
        }

        val records = try {
            insert.returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE, AUTHORS.CREATED_AT, AUTHORS.UPDATED_AT)
                .fetch()
        } catch (e: DuplicateKeyException) {
            throw DuplicateAuthorException(authors.joinToString(prefix = "[", postfix = "]") { "${it.name}/${it.birthDate}" })
        }

        return records.map { record ->
            Author.reconstruct(
                id = AuthorId(requireNotNull(record.get(AUTHORS.ID))),
                name = requireNotNull(record.get(AUTHORS.NAME)),
                birthDate = requireNotNull(record.get(AUTHORS.BIRTH_DATE)),
                createdAt = requireNotNull(record.get(AUTHORS.CREATED_AT)),
                updatedAt = requireNotNull(record.get(AUTHORS.UPDATED_AT)),
            )
        }
    }

    override fun update(author: Author): Author {
        val id = requireNotNull(author.id) { "author id must not be null when updating" }
        val record = try {
            dsl.update(AUTHORS)
                .set(AUTHORS.NAME, author.name)
                .set(AUTHORS.BIRTH_DATE, author.birthDate)
                .set(AUTHORS.UPDATED_AT, OffsetDateTime.now())
                .where(AUTHORS.ID.eq(id.value))
                .returning(AUTHORS.ID, AUTHORS.NAME, AUTHORS.BIRTH_DATE, AUTHORS.CREATED_AT, AUTHORS.UPDATED_AT)
                .fetchOne()
                ?: throw AuthorNotFoundException(id)
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