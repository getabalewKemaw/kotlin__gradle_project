package org.dbu.library.ui

import org.dbu.library.model.Book
import org.dbu.library.model.Patron
import org.dbu.library.repository.LibraryRepository
import org.dbu.library.service.BorrowResult
import org.dbu.library.service.LibraryService
import org.dbu.library.util.display
import java.time.LocalDate

fun handleMenuAction(
    choice: String,
    service: LibraryService,
    repository: LibraryRepository
): Boolean {

    return when (choice) {

        "1" -> {
            addBook(service)
            true
        }

        "2" -> {
            registerPatron(repository)
            true
        }

        "3" -> {
            borrowBook(service)
            true
        }

        "4" -> {
            returnBook(service)
            true
        }

        "5" -> {
            search(service)
            true
        }

        "6" -> {
            listAllBooks(repository)
            true
        }

        "7" -> {
            listPatrons(repository)
            true
        }

        "0" -> false

        else -> {
            println("Invalid option")
            true
        }
    }
}

private fun addBook(service: LibraryService) {
    val isbn = readNonBlank("ISBN: ") ?: return
    val title = readNonBlank("Title: ") ?: return
    val author = readNonBlank("Author: ") ?: return
    val year = readYear() ?: return

    if (service.addBook(Book(isbn, title, author, year))) {
        println("Book added.")
    } else {
        println("A book with ISBN $isbn already exists.")
    }
}

private fun registerPatron(repository: LibraryRepository) {
    val id = readNonBlank("Patron ID: ") ?: return
    val name = readNonBlank("Name: ") ?: return

    if (repository.addPatron(Patron(id, name))) {
        println("Patron registered.")
    } else {
        println("A patron with ID $id already exists.")
    }
}

private fun borrowBook(service: LibraryService) {
    val patronId = readNonBlank("Patron ID: ") ?: return
    val isbn = readNonBlank("ISBN: ") ?: return

    when (service.borrowBook(patronId, isbn)) {
        BorrowResult.SUCCESS -> println("Book borrowed. It is due in 14 days.")
        BorrowResult.BOOK_NOT_FOUND -> println("No book found with ISBN $isbn.")
        BorrowResult.PATRON_NOT_FOUND -> println("No patron found with ID $patronId.")
        BorrowResult.NOT_AVAILABLE -> println("That book is already borrowed.")
        BorrowResult.LIMIT_REACHED -> println("A patron may borrow up to 3 books at a time.")
    }
}

private fun returnBook(service: LibraryService) {
    val patronId = readNonBlank("Patron ID: ") ?: return
    val isbn = readNonBlank("ISBN: ") ?: return

    if (service.returnBook(patronId, isbn)) {
        println("Book returned.")
    } else {
        println("The book could not be returned. Check the patron ID and ISBN.")
    }
}

private fun search(service: LibraryService) {
    val query = readNonBlank("Search title, author, or ISBN: ") ?: return
    val results = service.search(query)
    if (results.isEmpty()) {
        println("No matching books found.")
    } else {
        results.forEach { println("${it.display()} [ISBN: ${it.isbn}]") }
    }
}

private fun listAllBooks(repository: LibraryRepository) {
    val books = repository.getAllBooks()
    if (books.isEmpty()) {
        println("No books in the library.")
        return
    }

    books.forEach { book ->
        val status = if (book.isAvailable) "Available" else "Borrowed"
        println("${book.display()} [ISBN: ${book.isbn}] - $status")
    }
}

private fun listPatrons(repository: LibraryRepository) {
    val patrons = repository.getAllPatrons()
    if (patrons.isEmpty()) {
        println("No patrons registered.")
        return
    }

    val booksByIsbn = repository.getAllBooks().associateBy(Book::isbn)
    val today = LocalDate.now()
    patrons.forEach { patron ->
        println("${patron.name} [ID: ${patron.id}]")
        if (patron.borrowedBooks.isEmpty()) {
            println("  No books borrowed.")
        } else {
            patron.borrowedBooks.forEach { isbn ->
                val title = booksByIsbn[isbn]?.title ?: "Unknown book"
                val dueDate = patron.bookDueDates[isbn]
                val dueDescription = when {
                    dueDate == null -> "due date unavailable"
                    dueDate.isBefore(today) -> "OVERDUE (due $dueDate)"
                    else -> "due $dueDate"
                }
                println("  $title [ISBN: $isbn] - $dueDescription")
            }
        }
    }
}

private fun readNonBlank(prompt: String): String? {
    while (true) {
        print(prompt)
        val value = readlnOrNull() ?: return null
        if (value.isNotBlank()) return value.trim()
        println("This field cannot be blank. Please try again.")
    }
}

private fun readYear(): Int? {
    val currentYear = LocalDate.now().year
    while (true) {
        print("Publication year (1-$currentYear): ")
        val input = readlnOrNull() ?: return null
        val year = input.trim().toIntOrNull()
        if (year != null && year in 1..currentYear) return year
        println("Enter a whole year between 1 and $currentYear.")
    }
}