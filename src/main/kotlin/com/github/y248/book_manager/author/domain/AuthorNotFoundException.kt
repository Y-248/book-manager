package com.github.y248.book_manager.author.domain

class AuthorNotFoundException(id: AuthorId) : RuntimeException("Author not found: id=${id.value}")