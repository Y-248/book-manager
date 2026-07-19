package com.github.y248.book_manager.book.domain

class BookNotFoundException(id: BookId) : RuntimeException("Book not found: id=${id.value}")