package com.takaapoo.adab_parsi.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast



class NetworkReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        StringBuilder().apply {
            append("Action: ${intent.action}\n")
            append("URI: ${intent.toUri(Intent.URI_INTENT_SCHEME)}\n")
            toString().also { log ->
                Toast.makeText(context, log, Toast.LENGTH_LONG).show()
            }
        }
    }
}