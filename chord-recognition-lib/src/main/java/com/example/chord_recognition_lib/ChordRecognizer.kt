package com.example.chord_recognition_lib

import android.util.Log
import androidx.annotation.NonUiContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import com.paramsen.noise.Noise
import com.example.chord_recognition_lib.AudioSource
import kotlin.math.*

class ChordRecognizer(
    private val sampleRate: Int = 11025,
    private val frameSize: Int = 8192,
    private val hopSize: Int = 1024,
    ) {
    val TAG = javaClass.simpleName

    private val noise = Noise.real(frameSize)
    private val chordStream: Flowable<String> = AudioSource(sampleRate, frameSize).stream()
        .observeOn(Schedulers.newThread())
        .map { calculateSpectrum(it) }
        .map { calculateChromagram(it) }
        .map { estimateChord(it) }

    private fun applyHammingWindow(signalFrame: FloatArray) {
        val a0 = 25 / 46
        val N = signalFrame.size
        for (n in signalFrame.indices) {
            signalFrame[n] = signalFrame[n] * (a0 - (1 - a0) * cos(2 * PI * n / N)).toFloat()
        }
    }

    private fun calculateSpectrum(signalFrame: FloatArray): FloatArray {
        applyHammingWindow(signalFrame)
        val fft = noise.fft(signalFrame, FloatArray(frameSize + 2))
        val spectrum = FloatArray(frameSize / 2)

        for (i in 0 until fft.size / 2 - 1) {
            val real = fft[i * 2]
            val imaginary = fft[i * 2 + 1]

            spectrum[i] = sqrt( real * real + imaginary * imaginary)
//            Log.d(TAG, "$i: ${spectrum[i]}")
        }
        return spectrum
    }

    private fun calculateChromagram(spectrum: FloatArray): FloatArray {
        val chromagram = FloatArray(12)

        for (n in chromagram.indices) {
            var sum = 0.0f
            for (phi in 1..2) {
                for (h in 1..2) {
                    val fn = 130.81 * 2.0.pow(n / 12.0)
                    val k = round((fn * phi * h) / (sampleRate.toFloat() / frameSize.toFloat())).toInt()
                    val k0 = k - (2 * h)
                    val k1 = k + (2 * h)
//                    Log.d(TAG, "$n, $phi, $h: $k")
//                    Log.d(TAG, "${(fn * phi * h) / (sampleRate / frameSize)}")
                    var max = 0.0f
                    for (i in k0..k1) {
                        if (spectrum[i] > max) {
                            max = spectrum[i]
                        }
                    }

                    sum += max * (1 / h)
                }
            }
            chromagram[n] = sum
//            Log.d(TAG, "$n: $sum")
        }

        val total = chromagram.sum()
        for (i in chromagram.indices) {
            chromagram[i] = chromagram[i] / total
            Log.d(TAG, "$i: ${chromagram[i]}")
        }

        return chromagram
    }

    private val chordTemplates = Array<FloatArray>(25) {
        return@Array when (it) {
            0 -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)
            1 -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            2 -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            3 -> floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
            4 -> floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
            5 -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            6 -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
            7 -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            8 -> floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            9 -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            10 -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
            11 -> floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            12 -> floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f)
            13 -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            14 -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            15 -> floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
            16 -> floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            17 -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f)
            18 -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            19 -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f)
            20 -> floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f)
            21 -> floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f)
            22 -> floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f)
            23 -> floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f)
            else -> floatArrayOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f)
        }
    }

    private fun dotProduct(vec1: FloatArray, vec2: FloatArray): Float {
        var sum = 0.0f
        for (i in vec1.indices) {
            sum += vec1[i] * vec2[i]
        }
        return sum
    }

    private fun findNearestTemplate(chromagram: FloatArray): Int {
        var argmax = 0
        var max = 0.0f

        for (i in chordTemplates.indices) {
            val dotProductValue = dotProduct(chromagram, chordTemplates[i])
            Log.d(TAG, "DT: $i: $dotProductValue")

            if (dotProductValue > max) {
                max  = dotProductValue
                argmax = i
            }
        }

        return argmax
    }

    private fun estimateChord(chromagram: FloatArray): String {
        return when (findNearestTemplate(chromagram)) {
            0 -> "C"
            1 -> "C#"
            2 -> "D"
            3 -> "D#"
            4 -> "E"
            5 -> "F"
            6 -> "F#"
            7 -> "G"
            8 -> "G#"
            9 -> "A"
            10 -> "A#"
            11 -> "B"
            12 -> "Cm"
            13 -> "C#m"
            14 -> "Dm"
            15 -> "D#m"
            16 -> "Em"
            17 -> "Fm"
            18 -> "F#m"
            19 -> "Gm"
            20 -> "G#m"
            21 -> "Am"
            22 -> "A#m"
            23 -> "Bm"
            else -> ""
        }
    }

    fun chordStream(): Flowable<String> {
        return chordStream
    }
}