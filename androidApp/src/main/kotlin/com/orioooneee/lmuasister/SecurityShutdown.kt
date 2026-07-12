package com.orioooneee.lmuasister

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.widget.Toast
import com.orioooneee.lmuasister.security.SecurityGate
import kotlin.system.exitProcess

internal object SecurityShutdown {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activities = linkedSetOf<Activity>()

    @Volatile
    private var closing = false

    fun install(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    synchronized(activities) { activities += activity }
                }

                override fun onActivityDestroyed(activity: Activity) {
                    synchronized(activities) { activities -= activity }
                }

                override fun onActivityStarted(activity: Activity) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivityStopped(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            },
        )
    }

    fun close(application: Application) {
        SecurityGate.block()
        if (closing) return
        closing = true

        mainHandler.post {
            Toast.makeText(
                application,
                "Application was closed due to security reasons",
                Toast.LENGTH_LONG,
            ).show()

            val snapshot = synchronized(activities) { activities.toList() }
            snapshot.forEach { activity ->
                runCatching { activity.finishAffinity() }
            }

            mainHandler.postDelayed(
                {
                    Process.killProcess(Process.myPid())
                    exitProcess(0)
                },
                750L,
            )
        }
    }
}
