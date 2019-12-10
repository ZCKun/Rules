package com.x2h.rules

import android.content.Context
import android.net.ConnectivityManager


class Util constructor(ctx: Context) {
    val ctx = ctx

    fun isNetworkConnected(): Boolean {
        val connectivityManager =
            ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


}