package com.personalwallet.app.core.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

object NotificationPermissionHelper {
    /**
     * Memeriksa apakah sakelar Akses Notifikasi untuk aplikasi ini sudah diaktifkan pengguna di Android Settings.
     */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return !flat.isNullOrEmpty() && flat.contains(packageName)
    }

    /**
     * Membuka halaman Pengaturan Sistem Android untuk mengaktifkan Akses Notifikasi.
     */
    fun openNotificationListenerSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
