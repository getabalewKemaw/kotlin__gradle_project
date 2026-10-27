package org.dbu.library.repository

import org.dbu.library.model.Book
import org.dbu.library.model.Patron

class InMemoryLibraryRepository : LibraryRepository {

    private val books = mutableMapOf<String, Book>()
    private val patrons = mutableMapOf<String, Patron>()

    override fun addBook(book: Book): Boolean =
        books.putIfAbsent(book.isbn, book) == null

    override fun findBook(isbn: String): Book? = books[isbn]

    override fun updateBook(book: Book) {
        require(books.containsKey(book.isbn)) { "Book ${book.isbn} does not exist" }
        books[book.isbn] = book
    }

    override fun addPatron(patron: Patron): Boolean =
        patrons.putIfAbsent(patron.id, patron) == null

    override fun findPatron(id: String): Patron? = patrons[id]

    override fun updatePatron(patron: Patron) {
        require(patrons.containsKey(patron.id)) { "Patron ${patron.id} does not exist" }
        patrons[patron.id] = patron
    }

    override fun getAllBooks(): List<Book> = books.values.toList()

    override fun getAllPatrons(): List<Patron> = patrons.values.toList()
}