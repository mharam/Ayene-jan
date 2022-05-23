package com.takaapoo.adab_parsi.util

import android.app.ActivityManager
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.getSystemService
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule


@GlideModule
class CustomGlideModuleV4 : AppGlideModule() {

    override fun applyOptions(context: Context, builder: GlideBuilder) {

//        val memInfo = ActivityManager.MemoryInfo()
//        (context.getSystemService(AppCompatActivity.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memInfo)
//        val totalMemory = memInfo.totalMem.toDouble()/(1024*1024)
//
//        Timber.i("total memory $totalMemory ")
//
//        builder.setDefaultRequestOptions(RequestOptions()
//            .format(if (totalMemory < 1700) DecodeFormat.PREFER_RGB_565 else DecodeFormat.PREFER_ARGB_8888))

//        builder.setDefaultRequestOptions(
//            RequestOptions().set(
//                Downsampler.ALLOW_HARDWARE_CONFIG,
//                true
//            )
//        )
    }

}