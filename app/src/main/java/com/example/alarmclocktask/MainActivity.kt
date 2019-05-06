package com.example.alarmclocktask

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Simple single-fragment [AppCompatActivity] subclass.
 *
 * @author Alexander Gorin
 */
class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AlarmFragment.newInstance()).commit()
        }
    }
}

