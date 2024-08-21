package com.eemphasys.vitalconnect.common.extensions

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.eemphasys.vitalconnect.R
import com.eemphasys.vitalconnect.common.Constants
import com.eemphasys.vitalconnect.common.enums.SendStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.text.SimpleDateFormat
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.yearsUntil
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds


fun Long.asTimeString(): String = SimpleDateFormat("H:mm", Locale.getDefault()).format(Date(this))

fun Long.asDateString(): String = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date(this))

fun Long.asMessageCount(): String = if (this > 99) "99+" else this.toString()

@RequiresApi(Build.VERSION_CODES.O)
fun Long.asMessageDateString() : String {
    if (this == 0L) {
        return ""
    }

    val instant = Instant.fromEpochMilliseconds(this)
    val updatedInstant = instant.plus(Constants.TIME_OFFSET!!.seconds)

    val now = Clock.System.now()
//    val timeZone = TimeZone.currentSystemDefault()
//    val days: Int = updatedInstant.daysUntil(now, timeZone)

//    val dateFormat = if (days == 0) "H:mm" else "dd-MM-yyyy H:mm"
    val dateFormat = "H:mm"
//    return SimpleDateFormat(dateFormat, Locale.getDefault()).format(Date(this))
    // Create a SimpleDateFormat for the desired format in UTC
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Convert kotlinx.datetime.Instant to java.time.Instant, then to Date
    return formatter.format(Date.from(updatedInstant.toJavaInstant()))
}

@RequiresApi(Build.VERSION_CODES.O)
fun Long.asMessageDateChangedString(): String {
    if (this == 0L) {
        return ""
    }

    // Create the original Instant from the given timestamp
    val originalInstant = Instant.fromEpochMilliseconds(this)

    // Apply the offset (convert seconds to milliseconds for calculation)
    val offsetMillis = (Constants.TIME_OFFSET ?: 0) * 1000L
    val updatedInstant = originalInstant.plus(offsetMillis.milliseconds)

    // Get the current time in UTC as Instant
    val nowInstant = Clock.System.now()

    // Convert Instants to Java Date for comparison
    val updatedDate = Date.from(updatedInstant.toJavaInstant())
    val nowDate = Date.from(nowInstant.toJavaInstant())

    // Define the format for the date
    val dateFormat = "E MMM dd yyyy"
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    // Helper function to get the start of the day for comparison
    fun startOfDay(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    // Compare if the updated date and current date are on the same day
    val updatedStartOfDay = startOfDay(updatedDate)
    val nowStartOfDay = startOfDay(nowDate)
    val isSameDay = updatedStartOfDay == nowStartOfDay

    // Return "Today" if the dates are the same, otherwise return the formatted date
    return if (isSameDay) "Today" else formatter.format(updatedDate)
}

@RequiresApi(Build.VERSION_CODES.O)
fun Long.asLastMessageDateString(context: Context) : String {    if (this == 0L) {
    return ""
}

    // Create the original Instant from the given timestamp
    val originalInstant = Instant.fromEpochMilliseconds(this)

    // Apply the offset (convert seconds to milliseconds for calculation)
    val offsetMillis = (Constants.TIME_OFFSET ?: 0) * 1000L
    val updatedInstant = originalInstant.plus(offsetMillis.milliseconds)

    // Get the current time in UTC as Instant
    val nowInstant = Clock.System.now()

    // Convert Instants to Java Date for comparison
    val updatedDate = Date.from(updatedInstant.toJavaInstant())
    val nowDate = Date.from(nowInstant.toJavaInstant())

    // Helper function to get the start of the day for comparison
    fun startOfDay(date: Date): Date {
        val calendar = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.time
    }

    // Compare if the updated date and current date are on the same day
    val updatedStartOfDay = startOfDay(updatedDate)
    val nowStartOfDay = startOfDay(nowDate)
    val isSameDay = updatedStartOfDay == nowStartOfDay

    // Define the format for the date
    val dateFormat = if (isSameDay) "H:mm" else "E MMM dd yyyy"
    val formatter = SimpleDateFormat(dateFormat, Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    return formatter.format(updatedDate)
}

fun SendStatus.asLastMesageStatusIcon() = when(this) {
    SendStatus.SENDING -> R.drawable.ic_waiting_message
    SendStatus.SENT -> R.drawable.ic_sent_message
    SendStatus.ERROR -> R.drawable.ic_failed_message
    else -> 0
}

fun SendStatus.asLastMessageTextColor(context: Context) = when (this) {
    SendStatus.ERROR -> ContextCompat.getColor(context, R.color.colorAccent)
    else -> ContextCompat.getColor(context, R.color.text_subtitle)
}