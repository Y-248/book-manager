package com.github.y248.book_manager.shared.presentation

import com.github.y248.book_manager.author.domain.DuplicateAuthorException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationError(
        exception: MethodArgumentNotValidException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val fieldErrors = exception.bindingResult.fieldErrors.map {
            ErrorResponse.FieldError(field = it.field, message = it.defaultMessage ?: "invalid value")
        }
        val body = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Validation failed",
            path = request.requestURI,
            errors = fieldErrors,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }

    @ExceptionHandler(DuplicateAuthorException::class)
    fun handleDuplicateAuthor(
        exception: DuplicateAuthorException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = HttpStatus.CONFLICT.value(),
            error = HttpStatus.CONFLICT.reasonPhrase,
            message = exception.message ?: "Conflict",
            path = request.requestURI,
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body)
    }

    /**
     * ドメイン層のrequire()/requireNotNull()（不変条件違反）はIllegalArgumentExceptionとして飛んでくる。
     * 本来はDTOのBean Validationで先に弾かれる想定だが、防御的に書いた不変条件チェックが
     * 何らかの理由で発火した場合にSpringのデフォルト500ハンドリングに落ちないようにする。
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        exception: IllegalArgumentException,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        val body = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = exception.message ?: "Invalid request",
            path = request.requestURI,
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body)
    }
}