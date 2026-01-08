package com.example.safezoneai.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidad para grabar audio en situaciones de emergencia.
 * Usa MediaRecorder para grabación real de audio.
 */
class AudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false

    /**
     * Verifica si el permiso de grabación de audio está concedido.
     */
    fun hasAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Inicia la grabación de audio.
     * @return El archivo donde se está guardando el audio, o null si falla.
     */
    fun startRecording(): File? {
        if (!hasAudioPermission()) {
            return null
        }

        if (isRecording) {
            stopRecording()
        }

        return try {
            // Crear archivo de audio con timestamp
            val audioDir = File(context.filesDir, "emergency_audios")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioFile = File(audioDir, "emergency_$timestamp.m4a")
            currentAudioFile = audioFile

            // Configurar MediaRecorder
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)

                prepare()
                start()
            }

            isRecording = true
            audioFile

        } catch (e: IOException) {
            e.printStackTrace()
            release()
            null
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            release()
            null
        }
    }

    /**
     * Detiene la grabación de audio.
     * @return El archivo de audio grabado, o null si no había grabación activa.
     */
    fun stopRecording(): File? {
        if (!isRecording) return null

        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false

            currentAudioFile

        } catch (e: Exception) {
            e.printStackTrace()
            release()
            null
        }
    }

    /**
     * Verifica si hay una grabación en curso.
     */
    fun isRecording(): Boolean = isRecording

    /**
     * Obtiene el archivo de audio actual.
     */
    fun getCurrentAudioFile(): File? = currentAudioFile

    /**
     * Libera los recursos del MediaRecorder.
     */
    private fun release() {
        try {
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null
        isRecording = false
    }

    /**
     * Obtiene todos los audios de emergencia grabados.
     */
    fun getAllEmergencyAudios(): List<File> {
        val audioDir = File(context.filesDir, "emergency_audios")
        return if (audioDir.exists() && audioDir.isDirectory) {
            audioDir.listFiles()?.filter { it.extension == "m4a" }?.sortedByDescending { it.lastModified() }
                ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Elimina un archivo de audio.
     */
    fun deleteAudio(file: File): Boolean {
        return try {
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}