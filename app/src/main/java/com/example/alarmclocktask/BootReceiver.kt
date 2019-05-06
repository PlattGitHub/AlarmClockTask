package com.example.alarmclocktask

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * [BroadcastReceiver] subclass that is used to set [Alarm] after device reboot.
 *
 * @author Alexander Gorin
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            BootService().setupWork(context, Intent(context, BootService::class.java))
        }
    }
}