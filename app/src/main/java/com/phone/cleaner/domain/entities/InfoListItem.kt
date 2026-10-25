package com.phone.cleaner.domain.entities

/** One line of a Device Info detail screen, already turned into text. */
sealed interface InfoListItem {
    /** The heading over one camera's block. */
    data class Heading(val title: String) : InfoListItem

    /** A label/value card, e.g. "Screen Size" / "6.71"". */
    data class Row(val label: String, val value: String) : InfoListItem
}
