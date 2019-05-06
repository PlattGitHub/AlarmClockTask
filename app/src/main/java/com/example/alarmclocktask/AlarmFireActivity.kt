package com.example.alarmclocktask

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple activity that used to show [AlertDialog] when alarm fires.
 *
 * @author ALexander Gorin
 */
class AlarmFireActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.alarm))
            .setMessage(getString(R.string.alarm_is_ringing))
            .setIcon(R.drawable.ic_alarm)
            .setCancelable(false)
            .setPositiveButton(getString(R.string.dismiss)) { _, _ ->
                Utils.stopPlayingRingtone()
                finishAndRemoveTask()
            }
        val alert = builder.create()
        alert.show()

    }
}
