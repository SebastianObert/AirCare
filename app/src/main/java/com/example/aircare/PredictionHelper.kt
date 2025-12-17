package com.example.aircare

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class PredictionHelper(context: Context) {

    private var interpreter: Interpreter? = null
    private val modelPath = "health_prediction_model.tflite"

    // Definisikan bentuk input/output
    private val inputSize = 3 // PM2.5, Temp, Humidity
    private val outputSize = 3 // Aman, Waspada, Bahaya

    init {
        val options = Interpreter.Options()
        interpreter = try {
            loadModelFile(context, modelPath)?.let { Interpreter(it, options) }
        } catch (e: Exception) {
            Log.e("PredictionHelper", "Error loading model: ${e.message}")
            null
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer? {
        return try {
            val fileDescriptor = context.assets.openFd(modelPath)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        } catch (e: Exception) {
            Log.e("PredictionHelper", "Error reading model file: ${e.message}")
            null
        }
    }

    fun predict(pm25: Float, temp: Float, humidity: Float): String? {
        if (interpreter == null) {
            Log.e("PredictionHelper", "Interpreter is not initialized.")
            return null
        }

        // 1. Siapkan Input Buffer
        val inputBuffer = ByteBuffer.allocateDirect(inputSize * 4).apply {
            order(ByteOrder.nativeOrder())
            putFloat(pm25)
            putFloat(temp)
            putFloat(humidity)
            rewind() // Penting: kembalikan posisi ke awal sebelum dibaca model
        }

        // 2. Siapkan Output Array
        // Model output shape: [1, 3]
        val outputArray = Array(1) { FloatArray(outputSize) }

        // 3. Jalankan Model
        try {
            interpreter?.run(inputBuffer, outputArray)
        } catch (e: Exception) {
            Log.e("PredictionHelper", "Error running inference: ${e.message}")
            return null
        }

        // 4. Proses Hasil
        val probabilities = outputArray[0]

        // PERBAIKAN DI SINI:
        // Mencari index dengan nilai probabilitas tertinggi secara langsung
        val predictedIndex = probabilities.indices.maxByOrNull { probabilities[it] } ?: -1

        // Ambil nilai probabilitasnya berdasarkan index tadi
        val maxProbability = if (predictedIndex != -1) probabilities[predictedIndex] else 0.0f

        Log.d("PredictionHelper", "In: [$pm25, $temp, $humidity] -> Out: ${probabilities.contentToString()} (Index: $predictedIndex, Prob: $maxProbability)")

        // 5. Logika Notifikasi (Threshold 0.70 atau 70%)
        if (predictedIndex > 0 && maxProbability > 0.70) {
            return when (predictedIndex) {
                1 -> "Waspada: Udara berpotensi memicu gejala ringan."
                2 -> "BAHAYA: Udara berisiko tinggi. Kurangi aktivitas luar."
                else -> null
            }
        }

        return null // Aman atau probabilitas rendah
    }

    fun close() {
        interpreter?.close()
    }
}