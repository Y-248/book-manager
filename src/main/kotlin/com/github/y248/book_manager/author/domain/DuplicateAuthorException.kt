package com.github.y248.book_manager.author.domain

import java.time.LocalDate

class DuplicateAuthorException : RuntimeException {
    constructor(name: String, birthDate: LocalDate) : super("Author already exists: name=$name, birthDate=$birthDate")

    /**
     * バルクインサート失敗時など、原因となった特定のname/birthDateを一意に特定できない場合に使う。
     */
    constructor(message: String) : super(message)
}