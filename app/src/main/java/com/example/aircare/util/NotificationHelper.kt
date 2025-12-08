package com.example.aircare.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.aircare.R

object NotificationHelper {

    private const val CHANNEL_ID = "aircare_channel_id"
    private const val CHANNEL_NAME = "AirCare Notifications"
    private const val SAVE_NOTIFICATION_ID = 1
    private const val DELETE_NOTIFICATION_ID = 2
    private const val PROFILE_UPDATE_NOTIFICATION_ID = 3
    private const val PROFILE_PICTURE_UPDATE_NOTIFICATION_ID = 4

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifikasi untuk aplikasi AirCare"
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSaveSuccessNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Data Tersimpan")
            .setContentText("Data kualitas udara berhasil disimpan ke riwayat.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(SAVE_NOTIFICATION_ID, builder.build())
    }

    fun showDeleteSuccessNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Data Terhapus")
            .setContentText("Data riwayat kualitas udara telah berhasil dihapus.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(DELETE_NOTIFICATION_ID, builder.build())
    }

    fun showProfileUpdateSuccessNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Profil Diperbarui")
            .setContentText("Informasi profil Anda telah berhasil diperbarui.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PROFILE_UPDATE_NOTIFICATION_ID, builder.build())
    }

    fun showProfilePictureUpdateSuccessNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle("Foto Profil Diperbarui")
            .setContentText("Foto profil Anda telah berhasil diperbarui.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(PROFILE_PICTURE_UPDATE_NOTIFICATION_ID, builder.build())
    }
}
