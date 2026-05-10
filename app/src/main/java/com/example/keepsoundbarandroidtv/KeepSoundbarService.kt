package com.example.keepsoundbarandroidtv

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlin.math.sin

class KeepSoundbarService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var audioTrack: AudioTrack? = null

    companion object {
        private const val CHANNEL_ID = "KeepSoundbarAwakeChannel"
        private const val NOTIFICATION_ID = 1
        private const val FREQUENCY = 19000.0 // 19 kHz
        private const val SAMPLE_RATE = 44100
        private const val INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            initAudioTrack()
            while (isActive) {
                playInaudibleTone()
                delay(INTERVAL_MS)
            }
        }
    }

    private fun initAudioTrack() {
        val durationMs = 1000 // 1 second
        val numSamples = durationMs * SAMPLE_RATE / 1000
        val sample = ShortArray(numSamples)
        val angleResolution = 2.0 * Math.PI * FREQUENCY / SAMPLE_RATE

        val amplitude = Short.MAX_VALUE * 0.15 // %15 volume, soundbar için yeterli
        val fadeLength = SAMPLE_RATE / 50 // 20ms fade in/out

        for (i in 0 until numSamples) {
            val sinValue = sin(angleResolution * i)
            val fade = when {
                i < fadeLength -> i.toDouble() / fadeLength
                i > numSamples - fadeLength -> (numSamples - i).toDouble() / fadeLength
                else -> 1.0
            }
            sample[i] = (sinValue * amplitude * fade).toInt().toShort()
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(audioFormat)
            .setBufferSizeInBytes(sample.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(sample, 0, sample.size)
    }

    private suspend fun playInaudibleTone() {
        try {
            audioTrack?.play()
            delay(1100) // 1 second play + buffer
            audioTrack?.stop()
            audioTrack?.reloadStaticData() // Re-prime for next play
        } catch (e: Exception) {
            // Re-initialize if something goes wrong
            initAudioTrack()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Keep Soundbar Awake Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Running inaudible pings to prevent soundbar from sleeping"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Keep Soundbar Awake")
            .setContentText("Service is active")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        audioTrack?.release()
    }
}
