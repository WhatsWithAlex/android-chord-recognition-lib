package com.example.chord_recognition_app

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ToggleButton
import io.reactivex.disposables.CompositeDisposable
import com.example.chord_recognition_lib.ChordRecognizer

class MainActivity : AppCompatActivity() {
    val TAG = javaClass.simpleName

    val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestAudio()

        val toggleButton = findViewById<ToggleButton>(R.id.toggleButton)

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                start()
            } else {
                stop()
            }
        }
    }

    private fun start() {
        val chordLabelText = findViewById<TextView>(R.id.chordLabel)
        val chordStream = ChordRecognizer().chordStream()

        disposable.add(chordStream.subscribe({ chordLabel ->
            chordLabelText.text = chordLabel
        }, {e -> e.message?.let { Log.e(TAG, it) }}))
    }

    private fun stop() {
        val chordLabelText = findViewById<TextView>(R.id.chordLabel)

        disposable.clear()
        chordLabelText.text = ""
    }

    private fun requestAudio(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(RECORD_AUDIO) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), 1337)
            return false
        }

        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

//        if (grantResults[0] == PERMISSION_GRANTED)
////            start()
    }
}