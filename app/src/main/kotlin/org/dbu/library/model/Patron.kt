package org.dbu.library.model

import java.time.LocalDate

data class Patron(
    val id: String,
    val name: String,
    val borrowedBooks: List<String> = emptyList(),
    val bookDueDates: Map<String, LocalDate> = emptyMap()
)