package com.seriee.app

import android.app.Application
import android.content.Intent
import android.os.Looper
import android.os.Process
import com.seriee.app.data.SerieeRepository
import com.seriee.app.debug.AppLogger
import com.seriee.app.ui.NeoBrutalistColors
import com.seriee.app.ui.NeoTone
import kotlin.system.exitProcess

class SerieeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppLogger.init(this)
        // Widgets resolve palettes without a ViewModel, so load the Neo tone here.
        runCatching { NeoBrutalistColors.setTone(NeoTone.fromName(SerieeRepository(this).loadNeoTone())) }
        installUncaughtExceptionHandler()
        AppLogger.i("App", "SerieeApplication initialized (v${BuildConfig.VERSION_NAME})")
    }

    private fun installUncaughtExceptionHandler() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val isMainThread = thread === Looper.getMainLooper().thread
            val report = runCatching {
                AppLogger.recordCrash(
                    threadName = thread.name,
                    throwable = throwable,
                    commitSync = isMainThread,
                )
            }.getOrNull()

            if (isMainThread) {
                // A second crash in a row means the user's custom appearance is a
                // likely culprit: restore the shipped defaults before relaunching.
                if ((report?.consecutiveCount ?: 1) >= SAFE_MODE_CRASH_STREAK) {
                    runCatching { SerieeRepository(this).resetAppearanceToDefaults() }
                }

                // A deterministic startup crash would relaunch forever because the
                // in-app error dialog can never compose. After a few attempts in a
                // short window, hand the crash back to the platform.
                if (!AppLogger.allowCrashRelaunch()) {
                    previousHandler?.uncaughtException(thread, throwable)
                        ?: terminate()
                    return@setDefaultUncaughtExceptionHandler
                }

                // On main UI thread exceptions the Looper can no longer pump frames in the broken task,
                // so relaunch MainActivity in a clean task to immediately present the user-friendly error dialog
                // instead of an abrupt OS force-close popup.
                val launched = runCatching {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                        )
                        putExtra(EXTRA_SHOW_CRASH_DIALOG, true)
                    }
                    startActivity(intent)
                }.isSuccess

                if (launched) {
                    terminate()
                } else {
                    previousHandler?.uncaughtException(thread, throwable)
                }
            } else if (throwable is VirtualMachineError || throwable is LinkageError) {
                // The JVM state is no longer trustworthy; let the platform handle it.
                previousHandler?.uncaughtException(thread, throwable)
            }
            // Recoverable background exceptions are captured in AppLogger.pendingCrash
            // so the active UI displays the user-friendly CrashErrorDialog without force closing.
        }
    }

    private fun terminate(): Nothing {
        Process.killProcess(Process.myPid())
        exitProcess(1)
    }

    companion object {
        const val EXTRA_SHOW_CRASH_DIALOG = "com.seriee.app.EXTRA_SHOW_CRASH_DIALOG"
        private const val SAFE_MODE_CRASH_STREAK = 2
    }
}
