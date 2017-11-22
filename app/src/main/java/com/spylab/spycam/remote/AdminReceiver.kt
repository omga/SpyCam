package com.spylab.spycam.remote

import android.app.admin.DeviceAdminReceiver
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.UserHandle
import android.util.Log

class AdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(ctxt: Context, intent: Intent) {
        Log.d("ADMIN", "onEnabled")

        val cn = ComponentName(ctxt, AdminReceiver::class.java)
        val mgr = ctxt.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        mgr.setPasswordQuality(cn,
                DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC)

        onPasswordChanged(ctxt, intent, null)
    }

    override fun onPasswordFailed(context: Context?, intent: Intent?, user: UserHandle?) {
        Log.d("ADMIN", "FAILED")
    }

    override fun onPasswordSucceeded(context: Context?, intent: Intent?, user: UserHandle?) {
        Log.d("ADMIN", "SUX")
    }

    override fun onPasswordChanged(context: Context?, intent: Intent?, user: UserHandle?) {
        val mgr = context?.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val msg = if (mgr.isActivePasswordSufficient) {
            "COMPLIANT"
        } else {
            "NOT COMPLIANT"
        }
        Log.d("ADMIN", msg)

    }

}
