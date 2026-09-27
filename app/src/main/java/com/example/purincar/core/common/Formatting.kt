// How dates, times, money, gallons and mileage are written out on screen.
package com.example.purincar.core.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LongDate = DateTimeFormatter.ofPattern("MMMM d, yyyy")
private val DateTime = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")

// Writes a date like "March 4, 2026".
fun LocalDate.toLongDate(): String = format(LongDate)

// Writes a moment in local time like "Mar 4, 2026 at 3:15 PM".
fun Instant.toDateTime(): String = atZone(ZoneId.systemDefault()).format(DateTime)

// Writes an amount like "$1,234.50".
fun Double.toMoney(): String = String.format(Locale.US, "$%,.2f", this)

// Writes a gallon amount to the given number of decimals.
fun Double.toGallons(decimals: Int = 2): String = String.format(Locale.US, "%.${decimals}f", this)

// Writes a mileage with thousands separators like "12,345".
fun Int.toMiles(): String = String.format(Locale.US, "%,d", this)
