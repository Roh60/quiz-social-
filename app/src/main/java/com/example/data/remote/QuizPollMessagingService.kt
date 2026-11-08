package com.example.data.remote

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class QuizPollMessagingService : FirebaseMessagingService() {

  companion object {
    private const val TAG = "QuizPollFCM"
    const val CHANNEL_ID = "quiz_poll_community_alerts"
    const val CHANNEL_NAME = "QuizPoll Community Activity"

    fun ensureNotificationChannel(context: Context) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
          val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
          ).apply {
            description = "Real-time alerts when your quizzes and polls receive comments or votes."
            enableVibration(true)
          }
          manager.createNotificationChannel(channel)
        }
      }
    }

    fun showSystemNotification(
      context: Context,
      title: String,
      message: String,
      notificationId: Int = (System.currentTimeMillis() % 10000).toInt()
    ) {
      try {
        ensureNotificationChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
          flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
          context,
          notificationId,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
          .setSmallIcon(android.R.drawable.ic_dialog_info)
          .setContentTitle(title)
          .setContentText(message)
          .setStyle(NotificationCompat.BigTextStyle().bigText(message))
          .setPriority(NotificationCompat.PRIORITY_HIGH)
          .setAutoCancel(true)
          .setContentIntent(pendingIntent)
          .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, notification)
      } catch (e: Exception) {
        Log.e(TAG, "Error displaying notification", e)
      }
    }
  }

  override fun onNewToken(token: String) {
    super.onNewToken(token)
    Log.d(TAG, "Refreshed FCM token: $token")
    val currentUser = Firebase.auth.currentUser
    if (currentUser != null) {
      try {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(currentUser.uid).update(
          mapOf(
            "fcmToken" to token,
            "fcmTokenUpdatedAt" to FieldValue.serverTimestamp()
          )
        )
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist FCM token", e)
      }
    }
  }

  override fun onMessageReceived(remoteMessage: RemoteMessage) {
    super.onMessageReceived(remoteMessage)
    Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

    val title = remoteMessage.notification?.title
      ?: remoteMessage.data["title"]
      ?: "QuizPoll Community"
    val body = remoteMessage.notification?.body
      ?: remoteMessage.data["body"]
      ?: "You have new activity on your content."

    showSystemNotification(applicationContext, title, body)
  }
}
