package com.hazlaqoom.assistant

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var btnMic: ImageButton
    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var commandProcessor: CommandProcessor
    private lateinit var reminderManager: ReminderManager
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false

    private lateinit var switchWater: SwitchCompat
    private lateinit var switchHealth: SwitchCompat
    private lateinit var layoutWaterInterval: LinearLayout
    private lateinit var layoutHealthTime: LinearLayout
    private lateinit var seekBarWater: SeekBar
    private lateinit var tvWaterInterval: TextView
    private lateinit var tvHealthTime: TextView

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 100
        private const val REQUEST_CODE_SPEECH = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.CHANGE_WIFI_STATE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initVoiceAssistant()
        initReminders()
        checkPermissions()
    }

    private fun initViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvResult = findViewById(R.id.tvResult)
        btnMic = findViewById(R.id.btnMic)
        switchWater = findViewById(R.id.switchWater)
        switchHealth = findViewById(R.id.switchHealth)
        layoutWaterInterval = findViewById(R.id.layoutWaterInterval)
        layoutHealthTime = findViewById(R.id.layoutHealthTime)
        seekBarWater = findViewById(R.id.seekBarWater)
        tvWaterInterval = findViewById(R.id.tvWaterInterval)
        tvHealthTime = findViewById(R.id.tvHealthTime)
    }

    private fun initVoiceAssistant() {
        textToSpeech = TextToSpeech(this, this)
        commandProcessor = CommandProcessor(this)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        reminderManager = ReminderManager(this)

        btnMic.setOnClickListener {
            if (isListening) stopListening() else startListening()
        }
    }

    private fun initReminders() {
        switchWater.isChecked = reminderManager.isWaterReminderEnabled()
        layoutWaterInterval.visibility = if (switchWater.isChecked) LinearLayout.VISIBLE else LinearLayout.GONE

        switchWater.setOnCheckedChangeListener { _, isChecked ->
            layoutWaterInterval.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            if (isChecked) {
                val hours = seekBarWater.progress + 1
                reminderManager.scheduleWaterReminders(hours)
                Toast.makeText(this, "Water reminder every $hours hours", Toast.LENGTH_SHORT).show()
                testReminder("Water reminder active: Don't forget to drink water!")
            } else {
                reminderManager.cancelWaterReminders()
                Toast.makeText(this, "Water reminder disabled", Toast.LENGTH_SHORT).show()
            }
        }

        val savedInterval = reminderManager.getWaterInterval()
        seekBarWater.progress = savedInterval - 1
        tvWaterInterval.text = "$savedInterval hours"

        seekBarWater.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val hours = progress + 1
                tvWaterInterval.text = "$hours hours"
                if (switchWater.isChecked) {
                    reminderManager.scheduleWaterReminders(hours)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        switchHealth.isChecked = reminderManager.isHealthReminderEnabled()
        layoutHealthTime.visibility = if (switchHealth.isChecked) LinearLayout.VISIBLE else LinearLayout.GONE

        val (savedHour, savedMinute) = reminderManager.getHealthTime()
        tvHealthTime.text = String.format("%02d:%02d", savedHour, savedMinute)

        switchHealth.setOnCheckedChangeListener { _, isChecked ->
            layoutHealthTime.visibility = if (isChecked) LinearLayout.VISIBLE else LinearLayout.GONE
            if (isChecked) {
                val hour = savedHour
                val minute = savedMinute
                reminderManager.scheduleHealthCheck(hour, minute)
                Toast.makeText(this, "Health check at ${String.format("%02d:%02d", hour, minute)}", Toast.LENGTH_SHORT).show()
                testReminder("Health check active: How are you feeling today?")
            } else {
                reminderManager.cancelHealthCheck()
                Toast.makeText(this, "Health check disabled", Toast.LENGTH_SHORT).show()
            }
        }

        tvHealthTime.setOnClickListener {
            val currentHour = savedHour
            val currentMinute = savedMinute
            TimePickerDialog(this, { _, hourOfDay, minute ->
                tvHealthTime.text = String.format("%02d:%02d", hourOfDay, minute)
                if (switchHealth.isChecked) {
                    reminderManager.scheduleHealthCheck(hourOfDay, minute)
                    Toast.makeText(this, "Updated to ${String.format("%02d:%02d", hourOfDay, minute)}", Toast.LENGTH_SHORT).show()
                }
            }, currentHour, currentMinute, true).show()
        }
    }

    private fun testReminder(message: String) {
        textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun checkPermissions() {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toMutableList()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun startListening() {
        isListening = true
        btnMic.setImageResource(android.R.drawable.ic_media_pause)
        tvStatus.text = getString(R.string.listening)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show()
            stopListening()
        }
    }

    private fun stopListening() {
        isListening = false
        btnMic.setImageResource(android.R.drawable.ic_btn_speak_now)
        tvStatus.text = getString(R.string.tap_to_speak)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK && data != null) {
            val results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = results?.get(0) ?: ""
            tvResult.text = spokenText
            tvStatus.text = getString(R.string.processing)
            processCommand(spokenText)
        }
        stopListening()
    }

    private fun processCommand(command: String) {
        val response = commandProcessor.execute(command)
        tvStatus.text = response
        speak(response)
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("ar", "SA"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.language = Locale.US
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        speechRecognizer.destroy()
    }
}
