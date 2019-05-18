package com.example.alarmclocktask

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.example.alarmclocktask.AlarmReceiver.Companion.CANCEL_INTENT_ACTION
import com.example.alarmclocktask.AlarmReceiver.Companion.FIRE_ALARM_INTENT_ACTION
import com.example.alarmclocktask.AlarmReceiver.Companion.NOTIFICATION_INTENT_ACTION
import java.text.DateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Singleton that is used to perform main actions in application.
 *
 * @author Alexander Gorin
 */
object Utils {

    private const val ALARM_MINUTES_KEY = "com.example.alarmclocktask.ALARM_MINUTES_KEY"
    private const val ALARM_PREF_FILE_KEY = "com.example.alarmclocktask.ALARM_PREF_FILE_KEY"
    private const val ALARM_HOURS_KEY = "com.example.alarmclocktask.ALARM_HOURS_KEY"
    private const val ALARM_RINGTONE_KEY = "com.example.alarmclocktask.ALARM_MUSIC_KEY"
    private const val ALARM_IS_SET = "com.example.alarmclocktask.ALARM_IS_SET"
    private const val ALARM_IS_CANCELLED = "com.example.alarmclocktask.ALARM_IS_CANCELLED"

    private const val CHANNEL_ID = "CHANNEL_ID"
    private const val NOTIFICATION_ID = 123
    private const val ALARM_FIRE_REQUEST_CODE = 111
    private const val NOTIFICATION_REQUEST_CODE = 333
    private const val CANCEL_REQUEST_CODE = 444

    private const val DEFAULT_VOLUME = 50F

    private val mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
        )
        isLooping = true
        setVolume(DEFAULT_VOLUME, DEFAULT_VOLUME)
    }

    fun setupAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmFireIntent =
            setupAlarmPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationIntent =
            setupNotificationPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarmTime = getCalendarAlarmTime(alarm)

        writeToPrefsAlarm(context, alarm)
        enableBootReceiver(context)

        if (Calendar.getInstance().timeInMillis - alarmTime.timeInMillis > 0
            || isCancelledAlarm(context)
        ) {
            alarmTime.timeInMillis += TimeUnit.DAYS.toMillis(1)
        }

        setupAlarmManager(context, alarmTime, alarm, alarmManager, notificationIntent, alarmFireIntent)
    }

    fun setupRepeatingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val (minutes, hours) = getTimeAlarm(context)
        val ringtoneName = getRingtoneAlarm(context)
        val alarm = Alarm(minutes, hours, ringtoneName)

        val alarmFireIntent =
            setupAlarmPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)
        val notificationIntent =
            setupNotificationPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)

        val alarmTime = getCalendarAlarmTime(alarm)
        alarmTime.timeInMillis += TimeUnit.DAYS.toMillis(1)

        setupAlarmManager(context, alarmTime, alarm, alarmManager, notificationIntent, alarmFireIntent)
    }

    fun stopAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntentFire =
            setupAlarmPendingIntent(context, PendingIntent.FLAG_NO_CREATE)
        val notificationIntent =
            setupNotificationPendingIntent(context, PendingIntent.FLAG_NO_CREATE)

        alarmManager.cancel(alarmIntentFire)
        alarmManager.cancel(notificationIntent)

        cancelNotification(context)
        cleanPrefs(context)
        disableBootReceiver(context)
    }

    fun cancelFireAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntentFire =
            setupAlarmPendingIntent(context, PendingIntent.FLAG_NO_CREATE)
        val notificationIntent =
            setupNotificationPendingIntent(context, PendingIntent.FLAG_NO_CREATE)
        alarmManager.cancel(alarmIntentFire)
        alarmManager.cancel(notificationIntent)
    }

    private fun setupAlarmPendingIntent(context: Context, flag: Int) =
        Intent(context, AlarmReceiver::class.java).let { intent ->
            intent.action = FIRE_ALARM_INTENT_ACTION
            PendingIntent.getBroadcast(
                context,
                ALARM_FIRE_REQUEST_CODE,
                intent,
                flag
            )
        }

    private fun setupNotificationPendingIntent(context: Context, flag: Int) =
        Intent(context, AlarmReceiver::class.java).let { intent ->
            intent.action = NOTIFICATION_INTENT_ACTION
            PendingIntent.getBroadcast(
                context,
                NOTIFICATION_REQUEST_CODE,
                intent,
                flag
            )
        }

    private fun getCalendarAlarmTime(alarm: Alarm) = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, alarm.hours)
        set(Calendar.MINUTE, alarm.minutes)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun setupAlarmManager(
        context: Context,
        alarmTime: Calendar,
        alarm: Alarm,
        alarmManager: AlarmManager,
        notificationIntent: PendingIntent,
        alarmFireIntent: PendingIntent
    ) {
        if (alarmTime.timeInMillis - TimeUnit.MINUTES.toMillis(5) <= Calendar.getInstance().timeInMillis) {
            createNotification(context, alarm.minutes, alarm.hours)
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime.timeInMillis - TimeUnit.MINUTES.toMillis(5) + TimeUnit.DAYS.toMillis(1),
                notificationIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime.timeInMillis - TimeUnit.MINUTES.toMillis(5),
                notificationIntent
            )
        }
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTime.timeInMillis,
            alarmFireIntent
        )
    }

    private fun enableBootReceiver(context: Context) {
        val receiver = ComponentName(context, BootReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun disableBootReceiver(context: Context) {
        val receiver = ComponentName(context, BootReceiver::class.java)
        context.packageManager.setComponentEnabledSetting(
            receiver,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    private fun writeToPrefsAlarm(context: Context, alarm: Alarm) {
        val sharedPref =
            context.getSharedPreferences(ALARM_PREF_FILE_KEY, Context.MODE_PRIVATE) ?: return
        sharedPref.edit {
            putBoolean(ALARM_IS_SET, true)
            putString(ALARM_RINGTONE_KEY, alarm.ringtoneName)
            putInt(ALARM_MINUTES_KEY, alarm.minutes)
            putInt(ALARM_HOURS_KEY, alarm.hours)
        }
    }

    fun setCancelledAlarm(context: Context, isCancelled: Boolean) {
        val sharedPref =
            context.getSharedPreferences(ALARM_PREF_FILE_KEY, Context.MODE_PRIVATE) ?: return
        sharedPref.edit {
            putBoolean(ALARM_IS_CANCELLED, isCancelled)
        }
    }

    private fun cleanPrefs(context: Context) {
        val sharedPref =
            context.getSharedPreferences(ALARM_PREF_FILE_KEY, Context.MODE_PRIVATE) ?: return
        sharedPref.edit {
            clear()
        }
    }

    fun isSetAlarm(context: Context) = context.getSharedPreferences(
        ALARM_PREF_FILE_KEY,
        Context.MODE_PRIVATE
    ).getBoolean(ALARM_IS_SET, false)

    fun getTimeAlarm(context: Context) = Pair(
        context.getSharedPreferences(
            ALARM_PREF_FILE_KEY,
            Context.MODE_PRIVATE
        ).getInt(ALARM_MINUTES_KEY, -1),
        context.getSharedPreferences(
            ALARM_PREF_FILE_KEY,
            Context.MODE_PRIVATE
        ).getInt(ALARM_HOURS_KEY, -1)
    )

    private fun isCancelledAlarm(context: Context) = context.getSharedPreferences(
        ALARM_PREF_FILE_KEY,
        Context.MODE_PRIVATE
    ).getBoolean(ALARM_IS_CANCELLED, false)

    fun getRingtoneAlarm(context: Context) = context.getSharedPreferences(
        ALARM_PREF_FILE_KEY,
        Context.MODE_PRIVATE
    ).getString(ALARM_RINGTONE_KEY, "") ?: ""

    fun createNotification(context: Context, minutes: Int, hours: Int) {
        val cancelIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = CANCEL_INTENT_ACTION
        }
        val cancelPendingIntent: PendingIntent =
            PendingIntent.getBroadcast(
                context,
                CANCEL_REQUEST_CODE,
                cancelIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        val channelID = createNotificationChannel(context)
        val builder = NotificationCompat.Builder(context, channelID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(
                String.format(
                    context.getString(R.string.notification_alarm),
                    convertAlarmToDate(Alarm(minutes, hours))
                )
            )
            .setContentText(context.getString(R.string.warning_notification))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .addAction(
                R.drawable.ic_cancel, context.getString(R.string.cancel_alarm),
                cancelPendingIntent
            )
        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun createNotificationChannel(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_name)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            return CHANNEL_ID
        }
        return ""
    }


    fun cancelNotification(context: Context) {
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun convertAlarmToDate(alarm: Alarm) =
        DateFormat.getTimeInstance(DateFormat.SHORT).format(Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.hours)
            set(Calendar.MINUTE, alarm.minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time) ?: ""


    fun setupRingtonePickerIntent(context: Context, alarm: Alarm) =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TITLE,
                context.resources.getString(R.string.ringtone_dialog_title)
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(alarm.ringtoneName))
        }

    fun playRingtone(context: Context, uriRingtone: String) {
        mediaPlayer.apply {
            setDataSource(context, Uri.parse(uriRingtone))
            prepareAsync()
            setOnPreparedListener {
                it.start()
            }
        }
    }

    fun stopPlayingRingtone() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.reset()
        }
    }
}