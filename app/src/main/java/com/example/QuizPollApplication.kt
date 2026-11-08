package com.example

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class QuizPollApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    val firebaseApp = FirebaseApp.initializeApp(this)
    if (firebaseApp == null) {
      android.util.Log.e("QuizPollApplication", "Firebase is not configured. Replace app/google-services.json with the QuizPoll package configuration.")
      return
    }
    val appCheck = FirebaseAppCheck.getInstance(firebaseApp)
    if (BuildConfig.DEBUG) {
      appCheck.installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    } else {
      appCheck.installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
  }
}
