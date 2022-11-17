package com.davidogrady.irishexchange.util

import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MessageDateFormatter {

    // date formatters
    private val fullDateTimeSdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
    private val fullDateTimeSdf2 = SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault())
    private val dateSdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    private val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun getDisplayMessageDate(messageDate: String) : String {
        val messageDateText: String

        if (messageDate == "")
            return  ""

        // parse date string formatter into objects
        val dateTimeObj = try {
            fullDateTimeSdf.parse(messageDate)
        } catch (e: Exception) {
            fullDateTimeSdf2.parse(messageDate)
        }

        // init calendars for todays date and message date
        val todayDateCal = Calendar.getInstance()
        val messageTimeCal = Calendar.getInstance()
        messageTimeCal.time = dateTimeObj!!

        // now get the day, month, year of each date and store 2 arrays
        val currentDateArray = getDayMonthYearAndStoreInArray(todayDateCal)
        val messageDateArray = getDayMonthYearAndStoreInArray(messageTimeCal)

        val messageTimeArray = getHourMinuteAndStoreInArray(messageTimeCal)

        // if the message date is not today store the date otherwise the time
        if (currentDateArray.contentEquals(messageDateArray)) {
            messageDateText = timeSdf.format(messageTimeCal.time)
        } else
            messageDateText = dateSdf.format(messageTimeCal.time)

        return messageDateText
    }

    private fun getDayMonthYearAndStoreInArray(calendar: Calendar) : Array<String> {
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK).toString()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val year= calendar.get(Calendar.YEAR).toString()
        return arrayOf(dayOfWeek, dayOfMonth, year)
    }

    private fun getHourMinuteAndStoreInArray(calendar: Calendar) : Array<String> {
        val hour = calendar.get(Calendar.HOUR).toString()
        val minute = calendar.get(Calendar.MINUTE).toString()
        return arrayOf(hour, minute)
    }

     fun getMessageTimeText(messageDate: String) : String {

        // parse date string formatter into objects
        val dateTimeObj = fullDateTimeSdf.parse(messageDate) ?: return ""

        // init calendars for message date
        val messageTimeCal = Calendar.getInstance()
        messageTimeCal.time = dateTimeObj

        return timeSdf.format(messageTimeCal.time)
    }

    fun getMessageDateText(messageDate: String) : String {

        // parse date string formatter into objects
        val dateTimeObj = fullDateTimeSdf.parse(messageDate) ?: return ""

        // init calendars for message date
        val messageTimeCal = Calendar.getInstance()
        messageTimeCal.time = dateTimeObj

        return dateSdf.format(messageTimeCal.time)

    }
}