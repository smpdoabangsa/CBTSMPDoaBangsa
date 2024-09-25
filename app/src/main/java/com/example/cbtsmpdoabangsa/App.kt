package com.example.cbtsmpdoabangsa

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@HiltAndroidApp
class App: Application() {
    val applicationScope = CoroutineScope(context = SupervisorJob() + Dispatchers.IO)
}