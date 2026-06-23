package com.vica.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class — entry point for Hilt dependency injection.
 * Referenced in AndroidManifest.xml via android:name=".ViCaApplication"
 */
@HiltAndroidApp
class ViCaApplication : Application()
