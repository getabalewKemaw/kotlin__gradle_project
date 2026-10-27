package org.dbu.library.service

import org.dbu.library.model.Book
import org.dbu.library.repository.LibraryRepository
import java.time.Clock
import java.time.LocalDate

class DefaultLibraryService(
    private val repository: LibraryRepository,
    private val clock: Clock = Clock.systemDefaultZone()
) : LibraryService {

    override fun addBook(book: Book): Boolean = repository.addBook(book)

    override fun borrowBook(patronId: String, isbn: String): BorrowResult {
        val book = repository.findBook(isbn) ?: return BorrowResult.BOOK_NOT_FOUND
        val patron = repository.findPatron(patronId) ?: return BorrowResult.PATRON_NOT_FOUND
        if (!book.isAvailable) return BorrowResult.NOT_AVAILABLE
        if (patron.borrowedBooks.size >= MAX_BOOKS_PER_PATRON) {
            return BorrowResult.LIMIT_REACHED
        }

        val dueDate = LocalDate.now(clock).plusDays(LOAN_PERIOD_DAYS)
        repository.updateBook(book.copy(isAvailable = false))
        repository.updatePatron(
            patron.copy(
                borrowedBooks = patron.borrowedBooks + isbn,
                bookDueDates = patron.bookDueDates + (isbn to dueDate)
            )
        )
        return BorrowResult.SUCCESS
    }

    override fun returnBook(patronId: String, isbn: String): Boolean {
        val book = repository.findBook(isbn) ?: return false
        val patron = repository.findPatron(patronId) ?: return false
        if (isbn !in patron.borrowedBooks) return false

        repository.updateBook(book.copy(isAvailable = true))
        repository.updatePatron(
            patron.copy(
                borrowedBooks = patron.borrowedBooks - isbn,
                bookDueDates = patron.bookDueDates - isbn
            )
        )
        return true
    }

    override fun search(query: String): List<Book> {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        return repository.getAllBooks().filter {
            it.title.contains(normalizedQuery, ignoreCase = true) ||
                it.author.contains(normalizedQuery, ignoreCase = true) ||
                it.isbn.contains(normalizedQuery, ignoreCase = true)
        }
    }

    private companion object {
        const val MAX_BOOKS_PER_PATRON = 3
        const val LOAN_PERIOD_DAYS = 14L
    }
}