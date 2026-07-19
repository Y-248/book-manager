package com.github.y248.book_manager.book.domain

class InvalidPublicationStatusTransitionException(from: PublicationStatus, to: PublicationStatus) :
    RuntimeException("Cannot change publicationStatus from $from to $to")