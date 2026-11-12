package com.example.data.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Shows the App Open ad when the user comes back to the app from the background.
 *
 * It counts started activities of this app. The ad is shown only when the count goes from 0 to 1 AFTER
 * the app was really in the background. It is never shown on the first launch, on screen rotation,
 * or after another ad (an ad screen is also an activity of this app, so the count never reaches 0).
 * AppOpenAdManager itself adds a 5 minute cooldown.
 */
object AppOpenForegroundTrigger : Application.ActivityLifecycleCallbacks {

  private var registered = false
  private var startedActivities = 0
  private var wasInBackground = false

  fun register(application: Application) {
    if (registered) return
    registered = true
    application.registerActivityLifecycleCallbacks(this)
  }

  override fun onActivityStarted(activity: Activity) {
    startedActivities++
    if (startedActivities == 1 && wasInBackground) {
      wasInBackground = false
      AppOpenAdManager.showAdIfAvailable(activity)
    }
  }

  override fun onActivityStopped(activity: Activity) {
    if (startedActivities > 0) startedActivities--
    if (startedActivities == 0 && !activity.isChangingConfigurations) {
      wasInBackground = true
    }
  }

  override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
  override fun onActivityResumed(activity: Activity) {}
  override fun onActivityPaused(activity: Activity) {}
  override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
  override fun onActivityDestroyed(activity: Activity) {}
}
