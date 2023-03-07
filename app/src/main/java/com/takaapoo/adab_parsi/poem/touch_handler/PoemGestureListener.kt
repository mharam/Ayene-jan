package com.takaapoo.adab_parsi.poem.touch_handler

import android.view.GestureDetector
import android.view.MotionEvent
import com.takaapoo.adab_parsi.poem.PoemEvent
import com.takaapoo.adab_parsi.poem.PoemViewModel


class PoemGestureListener(private val poemViewModel: PoemViewModel) : GestureDetector.SimpleOnGestureListener() {

    override fun onDown(event: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        poemViewModel.reportEvent(PoemEvent.OnDoubleTap)
        return true
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        poemViewModel.reportEvent(PoemEvent.OnSingleTap)
        return true
    }

}