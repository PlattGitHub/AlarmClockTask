package com.example.alarmclocktask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * [BroadcastReceiver] subclass that performs actions of AlarmManager.
 *
 * @author Alexander Gorin
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent?.let {
            when (it.action) {
                NOTIFICATION_INTENT_ACTION -> {
                    val (minutes, hours) = Utils.getTimeAlarm(context)
                    Utils.createNotification(
                        context,
                        minutes,
                        hours
                    )
                }
                FIRE_ALARM_INTENT_ACTION -> {
                    Utils.cancelNotification(context)
                    Utils.setupRepeatingAlarm(context)
                    Utils.setCancelledAlarm(context, false)

                    val ringtone = Utils.getRingtoneAlarm(context)
                    if (ringtone != "") {
                        Utils.playRingtone(context, ringtone)
                    }
                    context.startActivity(Intent(context, AlarmFireActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })

                }
                CANCEL_INTENT_ACTION -> {
                    Utils.cancelFireAlarm(context)
                    Utils.cancelNotification(context)
                    Utils.setCancelledAlarm(context, true)
                    Utils.setupRepeatingAlarm(context)
                }
            }
        }
    }

    companion object {
        const val CANCEL_INTENT_ACTION = "com.example.alarmclocktask.CANCEL_INTENT_ACTION"
        const val NOTIFICATION_INTENT_ACTION = "com.example.alarmclocktask.NOTIFICATION_INTENT_ACTION"
        const val FIRE_ALARM_INTENT_ACTION = "com.example.alarmclocktask.FIRE_ALARM_INTENT_ACTION"
    }
}