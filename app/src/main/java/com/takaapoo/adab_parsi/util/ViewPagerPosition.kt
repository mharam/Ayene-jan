package com.takaapoo.adab_parsi.util

import androidx.viewpager2.widget.ViewPager2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ViewPagerPosition(
    pageScope: CoroutineScope,
    var viewPager: ViewPager2?,
    initialPage: Int
) {

    val pagePositionFlow =
        callbackFlow {
            val callback = object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    launch {
                        send(position)
                    }
                }
            }
            viewPager?.registerOnPageChangeCallback(callback)
            awaitClose {
                viewPager?.unregisterOnPageChangeCallback(callback)
                viewPager = null
            }
        }.stateIn(
            scope = pageScope,
            initialValue = initialPage,
            started = SharingStarted.WhileSubscribed(5000)
        )

}