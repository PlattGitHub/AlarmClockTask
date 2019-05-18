package com.example.alarmclocktask

import android.app.Activity
import android.app.TimePickerDialog
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.util.*


/**
 * A simple [Fragment] subclass that is used to create [Alarm].
 * Has 2 Buttons: for picking time with [TimePickerDialog] and for picking ringtone.
 * Has [Switch] to turn [Alarm] on.
 *
 */
class AlarmFragment : Fragment() {

    private lateinit var switchButton: Switch
    private lateinit var pickTimeButton: Button
    private lateinit var pickRingtoneButton: Button
    private lateinit var timeTextView: TextView
    private lateinit var ringtoneTextView: TextView
    private val alarm = Alarm()
    private var isAlarmSet = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context?.let {
            isAlarmSet = Utils.isSetAlarm(it)
            if (isAlarmSet) {
                val time = Utils.getTimeAlarm(it)
                val ringtone = Utils.getRingtoneAlarm(it)
                alarm.minutes = time.first
                alarm.hours = time.second
                alarm.ringtoneName = ringtone
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_alarm, container, false).apply {
            switchButton = findViewById(R.id.switchButton)
            pickTimeButton = findViewById(R.id.pickTimeButton)
            pickRingtoneButton = findViewById(R.id.pickRingtoneButton)
            timeTextView = findViewById(R.id.timeTextView)
            ringtoneTextView = findViewById(R.id.ringtoneNameTextView)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isAlarmSet) {
            switchButton.isChecked = true
            timeTextView.text = Utils.convertAlarmToDate(alarm)
            if (alarm.ringtoneName != "") {
                ringtoneTextView.text =
                    RingtoneManager.getRingtone(context, Uri.parse(alarm.ringtoneName)).getTitle(context)
            }
        }

        switchButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (timeTextView.text != "") {
                    context?.let { Utils.setupAlarm(it, alarm) }
                    timeTextView.text = Utils.convertAlarmToDate(alarm)
                } else {
                    switchButton.isChecked = false
                    Toast.makeText(context, getString(R.string.pick_time), Toast.LENGTH_SHORT).show()
                }
            } else {
                context?.let { Utils.stopAlarm(it) }
            }
        }

        pickTimeButton.setOnClickListener {
            switchButton.isChecked = false
            showTimePicker()
        }

        pickRingtoneButton.setOnClickListener {
            context?.let {
                switchButton.isChecked = false
                val intent = Utils.setupRingtonePickerIntent(it, alarm)
                startActivityForResult(intent, RINGTONE_PICKER_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RINGTONE_PICKER_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val uri = data?.extras?.get(RingtoneManager.EXTRA_RINGTONE_PICKED_URI) as Uri?
                uri?.let {
                    alarm.ringtoneName = uri.toString()
                    ringtoneTextView.text = RingtoneManager.getRingtone(context, uri).getTitle(context)
                }
            }
        }
    }

    private val timePickerResult = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
        alarm.hours = hourOfDay
        alarm.minutes = minute
        timeTextView.text = Utils.convertAlarmToDate(alarm)
    }

    private fun showTimePicker() {
        val myCalender = Calendar.getInstance()
        val hourInit = myCalender.get(Calendar.HOUR_OF_DAY)
        val minuteInit = myCalender.get(Calendar.MINUTE)
        val timePickerDialog = TimePickerDialog(context, timePickerResult, hourInit, minuteInit, true)
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            timePickerDialog.show()
        }, 50L)
    }

    companion object {
        fun newInstance() = AlarmFragment()
        private const val RINGTONE_PICKER_REQUEST_CODE = 555
    }
}
