package com.cy.shareText

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceFragmentCompat
import com.blankj.utilcode.util.ServiceUtils
import com.blankj.utilcode.util.ToastUtils

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, SettingsFragment())
            .commit()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            val hintStr = getString(R.string.hint_port)
            val findPreference =
                findPreference<EditTextPreference>(getString(R.string.preference_port_key))
            findPreference?.apply {
                setOnBindEditTextListener {
                    it.inputType = InputType.TYPE_CLASS_NUMBER
                    it.hint = hintStr
                }
                setOnPreferenceChangeListener { _, newValue ->
                    val newValueString = newValue.toString()

                    val standard = !TextUtils.isEmpty(newValueString)
                            && newValueString.toInt() in 1..65535
                    if (standard) {
                        if (ServiceUtils.isServiceRunning(WebServer::class.java)) {
                            activity?.sendBroadcast(
                                Intent(
                                    activity,
                                    StopServerReceiver::class.java
                                ).apply { putExtra(StopServerReceiver.NEED_RESTART, true) })
                        }
                    } else {
                        ToastUtils.showShort(hintStr)
                    }
                    standard
                }
            }
        }
    }
}