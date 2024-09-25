package com.example.cbtsmpdoabangsa.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log

class CBTService: Service() {

    private val binder = CBTBinder()

    inner class CBTBinder : Binder() {
        fun getService(): CBTService = this@CBTService
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.d("onBind", "app active")
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("onUnbind", "app in background")
        return super.onUnbind(intent)
    }
}