package com.example.alarmclocktask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * [BroadcastReceiver] subclass that performs actions of AlarmManager.
 *
 * @author Alexander Gorin
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        intent?.let {
            when (it.action) {
                Utils.NOTIFICATION_INTENT_ACTION -> {
                    val hours = intent.getIntExtra(Utils.ALARM_HOURS_EXTRA, -1)
                    val minutes = intent.getIntExtra(Utils.ALARM_MINUTES_EXTRA, -1)
                    val ringtoneName = intent.getStringExtra(Utils.ALARM_RINGTONE_EXTRA) ?: ""
                    Utils.createNotification(context, minutes, hours, ringtoneName)
                }
                Utils.FIRE_ALARM_INTENT_ACTION -> {
                    val ringtone = intent.getStringExtra(Utils.ALARM_RINGTONE_EXTRA) ?: ""
                    if (ringtone != "") {
                        Utils.playRingtone(context, ringtone)
                    }
                    Utils.cancelNotification(context)
                    val intentFire = Intent(context, AlarmFireActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intentFire)
                }
                Utils.CANCEL_INTENT_ACTION -> {
                    val hours = intent.getIntExtra(Utils.ALARM_HOURS_EXTRA, -1)
                    val minutes = intent.getIntExtra(Utils.ALARM_MINUTES_EXTRA, -1)
                    val ringtoneName = intent.getStringExtra(Utils.ALARM_RINGTONE_EXTRA) ?: ""
                    Utils.cancelFireAlarm(context, minutes, hours, ringtoneName)
                    Utils.cancelNotification(context)
                }
                else -> {
                    return
                }
            }
        }
    }
}