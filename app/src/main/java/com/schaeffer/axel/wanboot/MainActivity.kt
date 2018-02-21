package com.schaeffer.axel.wanboot

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
    }

    fun onClickBtn(v: View) {
        var label = findViewById<TextView>(R.id.label_status)
        var info = findViewById<TextView>(R.id.label_info)
        if (label.text == "Wake me up!")
        {
            info.visibility = View.VISIBLE
            info.text = "*contacting webservices" + "\n" + "*OKK" + "\n" + "*sending magic packet" + "\n"

            label.text = "I'm awake!"
            label.setTextAppearance(R.style.TextStatusThemeAwake)
        }
        else
        {
            info.visibility = View.INVISIBLE
            label.text = "Wake me up!"
            label.setTextAppearance(R.style.TextStatusThemeSleeping)
        }
    }
}
