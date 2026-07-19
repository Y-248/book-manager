package com.github.y248.book_manager.author.domain

import java.time.LocalDate

class DuplicateAuthorException(name: String, birthDate: LocalDate) :
    RuntimeException("Author already exists: name=$name, birthDate=$birthDate")