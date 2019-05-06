package com.example.alarmclocktask

/**
 * Data class for alarm
 *
 * @author ALexander Gorin
 */
data class Alarm(
    var minutes: Int = -1,
    var hours: Int = -1,
    var ringtoneName: String = ""
)