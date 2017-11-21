package com.spylab.spycam.remote

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.SystemClock
import android.util.Log


class UnkillableService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("UnkillableService", "onStartCommand")
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("UnkillableService", "onTaskRemoved")
        val restartService = Intent(applicationContext,
                UnkillableService::class.java)
        val restartServicePI = PendingIntent.getService(
                applicationContext, 34531, restartService,
                PendingIntent.FLAG_ONE_SHOT)

        //Restart the service once it has been killed android
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 3000, restartServicePI)

    }
}
