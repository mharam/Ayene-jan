package com.takaapoo.adab_parsi.util.fastScroll

import android.graphics.Canvas
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import me.zhanghai.android.fastscroll.FastScroller
import me.zhanghai.android.fastscroll.PopupTextProvider
import me.zhanghai.android.fastscroll.Predicate

class ResultFastScrollViewHelper (view: RecyclerView, popupTextProvider: PopupTextProvider?
                                  , val heightCalculate: () -> Int)
    : FastScroller.ViewHelper {


    private val mView: RecyclerView = view
    private var mPopupTextProvider: PopupTextProvider? = popupTextProvider
    private val mTempRect: Rect = Rect()


    var initialOffset = 0
    var scroll = 0

    fun modifyScroll(){
        scroll = (scrollRange-mView.height).coerceAtLeast(0)
        initialOffset = scroll
    }


    override fun addOnPreDrawListener(onPreDraw: Runnable) {
        mView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                onPreDraw.run()
            }
        })
    }

    override fun addOnScrollChangedListener(onScrollChanged: Runnable) {
        mView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                onScrollChanged.run()
                scroll += dy
            }
        })
    }

    override fun addOnTouchEventListener(onTouchEvent: Predicate<MotionEvent?>) {
        mView.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(recyclerView: RecyclerView, event: MotionEvent)
                    : Boolean {
                return onTouchEvent.test(event)
            }

            override fun onTouchEvent(recyclerView: RecyclerView, event: MotionEvent) {
                onTouchEvent.test(event)
            }
        })
    }

    override fun getScrollRange(): Int {
        val itemCount = getItemCount()
        if (itemCount == 0)
            return 0

        return (heightCalculate())
    }

    override fun getScrollOffset(): Int {
        initialOffset = scroll

        return scroll
    }

    override fun scrollTo(offset: Int) {
        // Stop any scroll in progress for RecyclerView.
        mView.stopScroll()
        val myOffset = offset - initialOffset
//        initialOffset = offset
        mView.scrollBy(0, myOffset)
    }





    override fun getPopupText(): String? {
        var popupTextProvider = mPopupTextProvider
        if (popupTextProvider == null) {
            val adapter = mView.adapter
            if (adapter is PopupTextProvider) {
                popupTextProvider = adapter
            }
        }
        if (popupTextProvider == null) {
            return null
        }
        val position = getFirstItemAdapterPosition()
        return if (position == RecyclerView.NO_POSITION) null
        else popupTextProvider.getPopupText(position)
    }

    private fun getItemCount(): Int {
        val linearLayoutManager = getVerticalLinearLayoutManager() ?: return 0
        var itemCount = linearLayoutManager.itemCount
        if (itemCount == 0) {
            return 0
        }
        if (linearLayoutManager is GridLayoutManager) {
            itemCount = (itemCount - 1) / linearLayoutManager.spanCount + 1
        }
        return itemCount
    }



    private fun getFirstItemAdapterPosition(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView: View = mView.getChildAt(0)
        val linearLayoutManager =
            getVerticalLinearLayoutManager() ?: return RecyclerView.NO_POSITION
        return linearLayoutManager.getPosition(itemView)
    }

    private fun getFirstItemOffset(): Int {
        if (mView.childCount == 0) {
            return RecyclerView.NO_POSITION
        }
        val itemView: View = mView.getChildAt(0)
        mView.getDecoratedBoundsWithMargins(itemView, mTempRect)
        return mTempRect.top
    }

    private fun getVerticalLinearLayoutManager(): LinearLayoutManager? {
        val layoutManager = mView.layoutManager as? LinearLayoutManager ?: return null
        val linearLayoutManager = layoutManager
        return if (linearLayoutManager.orientation != RecyclerView.VERTICAL) {
            null
        } else linearLayoutManager
    }


}