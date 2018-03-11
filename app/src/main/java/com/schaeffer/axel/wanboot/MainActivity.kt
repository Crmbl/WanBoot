package com.schaeffer.axel.wanboot

import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import com.github.ybq.android.spinkit.style.Circle
import org.jetbrains.anko.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.stream.Collectors
import kotlin.concurrent.timerTask
import kotlinx.android.synthetic.main.login_popup.view.*
import javax.net.ssl.*
import android.text.InputType
import android.widget.EditText



class MainActivity : AppCompatActivity() {
    private var usernameApi : String = ""
    private var urlServer : String = ""
    private var passwordApi : String = ""
    private var token : String = ""
    private var notNeeded : Boolean = false
    private var alert : AlertDialogBuilder? = null
    private var isChecked : Boolean = false
    private var prefs : SharedPreferences? = null
    private var isPopupShown : Boolean = false

    private val prefsFileName = "com.schaeffer.axel.wanboot.prefs"
    private val urlApiPing : String = "/api/wan/Ping"
    private val urlApiBoot : String = "/api/wan/BootUp"
    private val urlApiToken : String = "/api/token/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        showPopup()
    }

    private fun setToken() {
        notNeeded = false
        var info = findViewById<TextView>(R.id.label_info)
        var label = findViewById<TextView>(R.id.label_status)
        var button = findViewById<Button>(R.id.push_button)

        var result = ""
        try {
            doAsync(task = {
                //Dev stuff here, bypass ssl verification
                HttpsTrustManager.allowAllSSL()
                val urlConnection = URL("$urlServer$urlApiToken").openConnection() as HttpsURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.connectTimeout = 12000
                urlConnection.readTimeout = 12000
                urlConnection.setRequestProperty("Content-Type","application/json")
                val str = "{\"username\": \"$usernameApi\", \"password\": \"$passwordApi\"}"
                val outputInBytes = str.toByteArray(charset("UTF-8"))
                val os = urlConnection.outputStream
                os.write(outputInBytes)
                os.close()

                val responseCode = urlConnection.responseCode
                result = BufferedReader(InputStreamReader(urlConnection.inputStream)).lines().collect(Collectors.joining())
                urlConnection.disconnect()

                if (!notNeeded) {
                    if (responseCode == 200) {
                        uiThread {
                            token = result.split(':')[1].split('"')[1]
                            hideLoader()
                            pingRequest(false)
                        }
                    } else {
                        uiThread {
                            hideLoader()
                            info.text = "${formatPrompt(getString(R.string.server_not_responding))}"
                            info.text = "${formatPrompt(getString(R.string.status_error))}"
                            label.text = getString(R.string.title_error)
                            label.setTextAppearance(R.style.TextStatusThemeError)
                            button.setBackgroundResource(R.drawable.button_bg_round_diabled)
                            button.isClickable = false

                            val errorTimer = Timer()
                            errorTimer.schedule(timerTask {
                                showPopup()
                            }, 1000)
                        }
                    }
                }
            })
        }
        finally {
            val timer = Timer()
            timer.schedule(timerTask {
                if (result == "") {
                    notNeeded = true
                    runOnUiThread {
                        info.text = "${formatPrompt(getString(R.string.server_not_responding))}"
                        info.text = "${formatPrompt(getString(R.string.status_error))}"
                        label.text = getString(R.string.title_error)
                        label.setTextAppearance(R.style.TextStatusThemeError)
                        button.setBackgroundResource(R.drawable.button_bg_round_diabled)
                        button.isClickable = false
                        hideLoader()
                    }

                    val popupTimer = Timer()
                    popupTimer.schedule(timerTask {
                        showPopup()
                    }, 1000)
                }
            }, 12000)
        }
    }

    private fun sendMagicPackage() {
        notNeeded = false
        var info = findViewById<TextView>(R.id.label_info)
        var label = findViewById<TextView>(R.id.label_status)
        var button = findViewById<Button>(R.id.push_button)

        info.text = "${formatPrompt("${getString(R.string.status_magic)} $urlServer...")}"

        var result = ""
        try
        {
            doAsync {
                //Dev stuff here, bypass ssl verification
                HttpsTrustManager.allowAllSSL()
                val urlConnection = URL("$urlServer$urlApiBoot").openConnection() as HttpsURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connectTimeout = 12000
                urlConnection.readTimeout = 12000
                urlConnection.setRequestProperty("Authorization", "Bearer $token")
                val responseCode = urlConnection.responseCode
                result = BufferedReader(InputStreamReader(urlConnection.inputStream)).lines().collect(Collectors.joining())
                urlConnection.disconnect()

                uiThread {
                    if (!notNeeded) {
                        if (result == "Success" && responseCode == 200) {
                            info.text = "${formatPrompt(getString(R.string.magic_sparkle))}"
                            pingRequest(true)
                        }
                        else {
                            info.text = "${formatPrompt(getString(R.string.error_magic))}"
                            info.text = "${formatPrompt(getString(R.string.status_chaos))}"
                        }
                    }
                }
            }
        }
        finally {
            val timer = Timer()
            timer.schedule(timerTask {
                if (result == "") {
                    notNeeded = true
                    runOnUiThread {
                        info.text = "${formatPrompt(getString(R.string.server_not_responding))}"
                        info.text = "${formatPrompt(getString(R.string.status_error))}"
                        label.text = getString(R.string.title_error)
                        label.setTextAppearance(R.style.TextStatusThemeError)
                        button.setBackgroundResource(R.drawable.button_bg_round)
                        button.isClickable = true
                    }
                }
            }, 12000)
        }
    }

    private fun pingRequest(doTwice : Boolean) {
        notNeeded = false
        var label = findViewById<TextView>(R.id.label_status)
        var info = findViewById<TextView>(R.id.label_info)
        var button = findViewById<Button>(R.id.push_button)

        label.text = getString(R.string.title_determining)
        label.setTextAppearance(R.style.TextStatusThemeDetermining)
        info.text = formatPrompt("${getString(R.string.status_ping)} $urlServer...")
        button.setBackgroundResource(R.drawable.button_bg_round_diabled)
        button.isClickable = false

        var result = ""
        try {
            doAsync {
                //Dev stuff here, bypass ssl verification
                HttpsTrustManager.allowAllSSL()
                val urlConnection = URL("$urlServer$urlApiPing").openConnection() as HttpsURLConnection
                urlConnection.requestMethod = "GET"
                urlConnection.connectTimeout = 12000
                urlConnection.readTimeout = 12000
                urlConnection.setRequestProperty("Authorization", "Bearer $token")
                val responseCode = urlConnection.responseCode
                result = BufferedReader(InputStreamReader(urlConnection.inputStream)).lines().collect(Collectors.joining())
                urlConnection.disconnect()

                uiThread {
                    if (!notNeeded) {
                        if (result == "Success" && responseCode == 200) {
                            info.text = "${formatPrompt(getString(R.string.hello_world))}"
                            info.text = "${formatPrompt(getString(R.string.status_awake))}"
                            label.text = getString(R.string.title_awake)
                            label.setTextAppearance(R.style.TextStatusThemeAwake)
                            button.setBackgroundResource(R.drawable.button_bg_round_diabled)
                            button.isClickable = false
                        }
                        else {
                            if (doTwice)
                                pingRequest(false)

                            info.text = "${formatPrompt(getString(R.string.sleep_zzz))}"
                            info.text = "${formatPrompt(getString(R.string.status_sleep))}"
                            label.text = getString(R.string.title_asleep)
                            label.setTextAppearance(R.style.TextStatusThemeSleeping)
                            button.setBackgroundResource(R.drawable.button_bg_round)
                            button.isClickable = true
                        }
                    }
                }
            }
        }
        finally {
            val timer = Timer()
            timer.schedule(timerTask {
                if (result == "") {
                    notNeeded = true
                    runOnUiThread {
                        info.text = "${formatPrompt(getString(R.string.server_not_responding))}"
                        info.text = "${formatPrompt(getString(R.string.status_error))}"
                        label.text = getString(R.string.title_error)
                        label.setTextAppearance(R.style.TextStatusThemeError)
                        button.setBackgroundResource(R.drawable.button_bg_round)
                        button.isClickable = true
                    }
                }
            }, 12000)
        }
    }

    //region UI methods

    private fun onClickLogin(v : View) {
        usernameApi = v.editText.text.toString()
        passwordApi = v.editText2.text.toString()
        urlServer = v.editText3.text.toString()

        if (prefs != null) {
            val editor = prefs!!.edit()
            if (isChecked) {
                editor.putString("usernameApi", usernameApi)
                editor.putString("passwordApi", passwordApi)
                editor.putString("urlServer", urlServer)
                editor.putBoolean("isChecked", true)
            }
            else {
                editor.clear()
            }

            editor.apply()
        }

        alert?.dismiss()
        isPopupShown = false
        displayLoader()
        setToken()
    }

    private fun onCheckBoxChecked(b : Boolean) {
        isChecked = b
    }

    fun onClickBtn(v : View) {
        // send magic package to boot the computer.
        sendMagicPackage()
    }

    private fun formatPrompt(stringToAdd : String) : String{
        var info = findViewById<EditText>(R.id.label_info)
        info.text.append("$stringToAdd\n")

        info.post({
            var length = info.text.length
            info.isFocusable = true
            info.setSelection(length)
            info.isLongClickable = false
            info.setRawInputType(InputType.TYPE_CLASS_TEXT)
            info.setTextIsSelectable(true)
        })

        return info.text.toString()
    }

    private fun displayLoader() {
        var gridLoader : GridLayout = this@MainActivity.findViewById(R.id.GridLoader)
        var loader : ProgressBar = gridLoader.findViewById(R.id.spin_kit)
        loader.indeterminateDrawable = Circle()
        gridLoader.visibility = View.VISIBLE
    }

    private fun hideLoader() {
        var gridLoader : GridLayout = this@MainActivity.findViewById(R.id.GridLoader)
        gridLoader.visibility = View.INVISIBLE
    }

    private fun showPopup() {
        if (!isPopupShown) {
            runOnUiThread({
                isPopupShown = true
                prefs = this.getSharedPreferences(prefsFileName, 0)
                usernameApi = prefs!!.getString("usernameApi", "")
                passwordApi = prefs!!.getString("passwordApi", "")
                urlServer = prefs!!.getString("urlServer", "")
                isChecked = prefs!!.getBoolean("isChecked", false)

                alert = alert {
                    this.customView{
                        val input : View = layoutInflater.inflate(R.layout.login_popup, null)
                        this.addView(input, null)
                        if (usernameApi != "")
                            input.editText.setText(usernameApi)
                        if (passwordApi != "")
                            input.editText2.setText(passwordApi)
                        if (urlServer != "")
                            input.editText3.setText(urlServer)
                        if (isChecked == true)
                            input.checkBox.isChecked = true

                        input.button.setOnClickListener { onClickLogin(input) }
                        input.checkBox.setOnCheckedChangeListener { _, b ->  onCheckBoxChecked(b) }
                    }
                }.apply {
                    cancellable(false)
                    setFinishOnTouchOutside(false)
                }.show()
            })
        }
    }

    //endregion UI methods
}
