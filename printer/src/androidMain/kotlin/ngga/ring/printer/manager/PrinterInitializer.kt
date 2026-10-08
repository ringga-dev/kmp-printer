package ngga.ring.printer.manager

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import java.lang.ref.WeakReference

@SuppressLint("StaticFieldLeak")
object PrinterInitializer {
    private var context: Context? = null
    private var activityRef: WeakReference<ComponentActivity>? = null

    fun initialize(context: Context) {
        val app = context.applicationContext as? Application ?: return
        initialize(app)
    }

    fun initialize(application: Application) {
        this.context = application.applicationContext

        // Otomatis track Activity yang sedang aktif
        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                if (activity is ComponentActivity) {
                    activityRef = WeakReference(activity) // ✅ auto-set saat Activity resume
                }
            }

            override fun onActivityPaused(activity: Activity) {
                if (activityRef?.get() == activity) {
                    activityRef = null // ✅ auto-clear saat Activity tidak aktif
                }
            }

            // Sisanya tidak perlu diisi
            override fun onActivityCreated(activity: Activity, bundle: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
    }

    fun getContext(): Context {
        return context ?: throw IllegalStateException("PrinterInitializer not initialized. Call initialize() first.")
    }

    fun getContextOrNull(): Context? {
        return context
    }

    fun getActivity(): ComponentActivity? {
        return activityRef?.get()
    }
}