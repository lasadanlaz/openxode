package com.uwu.animex.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class AnimeXFcmService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "ignored data type=${message.data[KEY_TYPE]}")
    }

    companion object {
        private const val TAG = "AnimeXFcm"
        private const val KEY_TYPE = "type"
    }
}
