package com.neko233.ide.gitdailyworker.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 *
 *
 * @author SolarisNeko
 * Date on 2023-11-21
 * */
object DateTimeUtils {

    @JvmStatic
    fun isSameDay(
        date1: Date,
        date2: Long,
    ): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val commitDate = Date(date2)
        return sdf.format(date1) == sdf.format(commitDate)
    }

    @JvmStatic
    fun isSameWeek(
        date1: Date,
        date2: Long,
    ): Boolean {
        val calendar = Calendar.getInstance()
        calendar.time = date1
        val weekStart = calendar.firstDayOfWeek

        calendar.timeInMillis = date2
        val commitWeek = calendar.get(Calendar.WEEK_OF_YEAR)

        return calendar.firstDayOfWeek == weekStart && calendar.get(Calendar.WEEK_OF_YEAR) == commitWeek
    }

    @JvmStatic
    fun isSameMonth(
        date1: Date,
        date2: Long,
    ): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val commitDate = Date(date2)
        return sdf.format(date1) == sdf.format(commitDate)
    }

}