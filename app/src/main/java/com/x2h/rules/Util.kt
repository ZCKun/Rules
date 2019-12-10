package com.x2h.rules

import android.content.Context
import android.net.ConnectivityManager

/**
 * Created by 0x2h in 2019-12-10
 * E-mail: zckuna@163.com
 */
class Util constructor(ctx: Context) {
    val ctx = ctx

    fun isNetworkConnected(): Boolean {
        val connectivityManager =
            ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }


}