package com.github.y248.book_manager.book.presentation

import com.github.y248.book_manager.book.application.AuthorSpec
import com.github.y248.book_manager.book.application.BookRegistrationService
import com.github.y248.book_manager.book.application.BookUpdateService
import com.github.y248.book_manager.book.domain.BookId
import com.github.y248.book_manager.book.domain.PublicationStatus
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/books")
class BookController(
    private val bookRegistrationService: BookRegistrationService,
    private val bookUpdateService: BookUpdateService,
) {
    @PostMapping
    fun register(@Valid @RequestBody request: RegisterBookRequest): ResponseEntity<BookResponse> {
        val authorSpecs = requireNotNull(request.authors).map { AuthorSpec.of(it.authorId, it.name, it.birthDate) }
        val publicationStatus = request.publicationStatus
            ?.let { PublicationStatus.valueOf(it) }
            ?: PublicationStatus.UNPUBLISHED

        val result = bookRegistrationService.register(
            title = requireNotNull(request.title),
            price = requireNotNull(request.price),
            publicationStatus = publicationStatus,
            authorSpecs = authorSpecs,
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(BookResponse.from(result.book, result.authors))
    }

    @PatchMapping("/{bookId}")
    fun update(
        @PathVariable bookId: Long,
        @Valid @RequestBody request: UpdateBookRequest,
    ): ResponseEntity<BookResponse> {
        val authorSpecs = request.authors?.map { AuthorSpec.of(it.authorId, it.name, it.birthDate) }
        val publicationStatus = request.publicationStatus?.let { PublicationStatus.valueOf(it) }

        val result = bookUpdateService.update(
            id = BookId(bookId),
            title = request.title,
            price = request.price,
            publicationStatus = publicationStatus,
            authorSpecs = authorSpecs,
        )

        return ResponseEntity.ok(BookResponse.from(result.book, result.authors))
    }
}