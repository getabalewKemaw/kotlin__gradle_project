package org.dbu.library

import org.dbu.library.model.Book
import org.dbu.library.model.Patron
import org.dbu.library.repository.InMemoryLibraryRepository
import org.dbu.library.service.BorrowResult
import org.dbu.library.service.DefaultLibraryService
import org.dbu.library.ui.handleMenuAction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class LibraryFeaturesTest {
    private val repository = InMemoryLibraryRepository()
    private val service = DefaultLibraryService(
        repository,
        Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    )

    @Test
    fun `borrowing assigns due date and return clears it`() {
        repository.addBook(Book("ISBN-1", "Kotlin", "Author", 2024))
        repository.addPatron(Patron("P1", "Alex"))

        assertEquals(BorrowResult.SUCCESS, service.borrowBook("P1", "ISBN-1"))
        assertEquals(
            LocalDate.parse("2026-01-15"),
            repository.findPatron("P1")?.bookDueDates?.get("ISBN-1")
        )
        assertFalse(repository.findBook("ISBN-1")!!.isAvailable)

        assertTrue(service.returnBook("P1", "ISBN-1"))
        assertTrue(repository.findBook("ISBN-1")!!.isAvailable)
        assertTrue(repository.findPatron("P1")!!.bookDueDates.isEmpty())
    }

    @Test
    fun `patron listing shows borrowed title and overdue due date`() {
        val dueDate = LocalDate.now().minusDays(1)
        repository.addBook(Book("ISBN-1", "Kotlin", "Author", 2024, isAvailable = false))
        repository.addPatron(
            Patron("P1", "Alex", listOf("ISBN-1"), mapOf("ISBN-1" to dueDate))
        )

        val originalOutput = System.out
        val output = ByteArrayOutputStream()
        try {
            System.setOut(PrintStream(output))
            handleMenuAction("7", service, repository)
        } finally {
            System.setOut(originalOutput)
        }

        val rendered = output.toString()
        assertTrue(rendered.contains("Alex"))
        assertTrue(rendered.contains("Kotlin"))
        assertTrue(rendered.contains("OVERDUE"))
        assertTrue(rendered.contains(dueDate.toString()))
    }

    @Test
    fun `book entry rejects blank fields and invalid years before accepting valid values`() {
        val originalInput = System.`in`
        val originalOutput = System.out
        val input = ByteArrayInputStream(
            "\nISBN-1\n\nKotlin\nAuthor\nnot-a-year\n2024\n".toByteArray()
        )
        val output = ByteArrayOutputStream()
        try {
            System.setIn(input)
            System.setOut(PrintStream(output))
            handleMenuAction("1", service, repository)
        } finally {
            System.setIn(originalInput)
            System.setOut(originalOutput)
        }

        assertNotNull(repository.findBook("ISBN-1"))
        assertTrue(output.toString().contains("This field cannot be blank"))
        assertTrue(output.toString().contains("Enter a whole year"))
    }
}
