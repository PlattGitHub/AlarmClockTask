package com.example.alarmclocktask

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService

/**
 * [JobIntentService] subclass that sets [Alarm] after device reboot.
 *
 * @author Alexander Gorin
 */
class BootService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        if (Utils.isSetAlarm(this)) {
            val alarm = Alarm().apply {
                val time = Utils.getTimeAlarm(this@BootService)
                minutes = time.first
                hours = time.second
                ringtoneName = Utils.getRingtoneAlarm(this@BootService)
            }
            Utils.setupAlarm(this, alarm)
        }
    }


    fun setupWork(context: Context, workIntent: Intent) {
        enqueueWork(context, BootService::class.java, JOB_ID, workIntent)
    }

    private companion object {
        const val JOB_ID = 123
    }
}
