package com.example.chord_recognition_lib

import android.util.Log
import androidx.annotation.NonUiContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import com.paramsen.noise.Noise
import com.example.chord_recognition_lib.AudioSource

class ChordRecognizer(
    private val sampleFrequency: Int,
    private val windowLength: Int,
    private val hopLength: Int,
) {
    val TAG = javaClass.simpleName

    private val noise = Noise.real(windowLength)
    private val chordStream: Flowable<String> = AudioSource().stream()
        .observeOn(Schedulers.newThread())
        .map { calculateDFT(it) }
        .map { calculateChromagram(it) }
        .map { estimateChord(it) }

    private fun calculateDFT(signalFrame: FloatArray): FloatArray {
        return noise.fft(signalFrame, FloatArray(4096 + 2))
    }

    private fun calculateChromagram(fft: FloatArray): Float {
        var sum = 0f
        for (i in fft.indices) {
            sum += fft[i] * fft[i]
        }
        return (sum / (fft.size * fft.size))
    }

    private fun estimateChord(k: Float): String {
        return k.toInt().toString()
    }

    fun chordStream(): Flowable<String> {
        return chordStream
    }
}