package com.github.y248.book_manager.author.presentation

import com.github.y248.book_manager.author.application.AuthorRegistrationService
import com.github.y248.book_manager.author.application.AuthorUpdateService
import com.github.y248.book_manager.author.domain.AuthorId
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
@RequestMapping("/api/v1/authors")
class AuthorController(
    private val authorRegistrationService: AuthorRegistrationService,
    private val authorUpdateService: AuthorUpdateService,
) {
    @PostMapping
    fun register(@Valid @RequestBody request: RegisterAuthorRequest): ResponseEntity<AuthorResponse> {
        val author = authorRegistrationService.register(
            name = requireNotNull(request.name),
            birthDate = requireNotNull(request.birthDate),
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(AuthorResponse.from(author))
    }

    @PatchMapping("/{authorId}")
    fun update(
        @PathVariable authorId: Long,
        @Valid @RequestBody request: UpdateAuthorRequest,
    ): ResponseEntity<AuthorResponse> {
        val author = authorUpdateService.update(
            id = AuthorId(authorId),
            name = request.name,
            birthDate = request.birthDate,
        )
        return ResponseEntity.ok(AuthorResponse.from(author))
    }
}